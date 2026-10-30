package dev.maxenceng.lazyclaude.board.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the board of the project a working directory belongs to. It owns the board's rules: one group per status in lifecycle
 * order, tickets in id order, and the issues the backlog refused listed beside them, in id order.
 */
public final class BoardManager {

    private static final String UNREADABLE_WARNING = "board: %s; showing the board as last read";

    private final ProjectLocatorPort locator;
    private final BacklogPort backlog;
    private final BranchChangesPort changes;
    private final BoardWatchPort watch;
    private final WarningPort warnings;
    private final WarnOnce unreadable = new WarnOnce();

    public BoardManager(
            ProjectLocatorPort locator,
            BacklogPort backlog,
            BranchChangesPort changes,
            BoardWatchPort watch,
            WarningPort warnings) {
        this.locator = locator;
        this.backlog = backlog;
        this.changes = changes;
        this.watch = watch;
        this.warnings = warnings;
    }

    /** Runs {@code changed} on the watcher's thread whenever the project's issues changed, or the check could not tell. */
    public void watch(WorkingDirectory from, Runnable changed) {
        watch.watch(locator.locate(from), changed);
    }

    /**
     * A re-read of the board failed and the one on screen stays. Saying why is raised once while it keeps failing, and withdrawn by the
     * next good read; a failure after that is named again.
     */
    public void boardUnreadable(Reason cause) {
        unreadable.raise(warnings, new Warning(UNREADABLE_WARNING.formatted(cause.value())));
    }

    public Board boardFor(WorkingDirectory from) {
        Project project = locator.locate(from);
        Board read = board(project, backlog.entries(project));
        unreadable.withdraw(warnings);
        return read;
    }

    /** The project without tickets, saying why there are none, for a project whose issues cannot be read. */
    public UnavailableBoard unavailableBoard(WorkingDirectory from, Reason why) {
        return UnavailableBoard.builder().project(locator.locate(from)).reason(why).build();
    }

    private Board board(Project project, List<BacklogEntry> entries) {
        return Board.builder()
                .project(project)
                .groups(groups(entries.stream()
                        .flatMap(entry -> entry instanceof Ticket read ? Stream.of(withChanges(project, read)) : Stream.empty())
                        .toList()))
                .unreadable(entries.stream()
                        .flatMap(entry -> entry instanceof UnreadableTicket refused ? Stream.of(refused) : Stream.empty())
                        .sorted(Comparator.comparing(UnreadableTicket::id))
                        .toList())
                .build();
    }

    /** The branch is read only for a ticket whose changes decide what it is offered. */
    private Ticket withChanges(Project project, Ticket ticket) {
        return ticket.mayOfferDesign() ? ticket.withChanges(changes.changesOf(project, ticket.branch())) : ticket;
    }

    private static List<StatusGroup> groups(List<Ticket> tickets) {
        return Arrays.stream(Status.values())
                .map(status -> StatusGroup.builder()
                        .status(status)
                        .tickets(tickets.stream()
                                .filter(ticket -> ticket.status() == status)
                                .sorted(Comparator.comparing(Ticket::id))
                                .toList())
                        .build())
                .toList();
    }
}
