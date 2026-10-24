package dev.maxenceng.lazyclaude.board.domain;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.shared.domain.BranchName;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarnOnce;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * Builds the board of the project a working directory belongs to. It owns the board's rules:
 * one group per status in lifecycle order, tickets in id order, and one ticket per id — a later
 * file claiming an id already read becomes an unreadable ticket, whoever produced the entries —
 * and which copy of a ticket file is the ticket: a branch copy unless the root copy reads done, a
 * merged branch read only through a worktree that holds it, and a branch copy that cannot be read
 * never replaced by the root copy.
 */
public final class BoardManager {

    private static final String POLLING_WARNING = "board: %s; checking every %d s";
    private static final String UNREADABLE_WARNING = "board: %s; showing the board as last read";

    private final ProjectLocatorPort locator;
    private final BacklogPort backlog;
    private final BranchCopyPort copies;
    private final BranchChangesPort changes;
    private final BoardWatchPort watch;
    private final WarningPort warnings;
    private final WarnOnce unreadable = new WarnOnce();

    public BoardManager(
            ProjectLocatorPort locator,
            BacklogPort backlog,
            BranchCopyPort copies,
            BranchChangesPort changes,
            BoardWatchPort watch,
            WarningPort warnings) {
        this.locator = locator;
        this.backlog = backlog;
        this.copies = copies;
        this.changes = changes;
        this.watch = watch;
        this.warnings = warnings;
    }

    /**
     * Runs {@code changed} after each batch of changes to what the board is read from, on the watcher's thread. A tree that is
     * polled instead of watched is warned of, once for each tree.
     */
    public void watch(WorkingDirectory from, Runnable changed) {
        watch.watch(locator.locate(from), changed, this::warnOfPolling);
    }

    /**
     * A re-read of the board failed and the one on screen stays. Saying why is raised once for the run: a warning is never withdrawn,
     * so a board that keeps failing and recovering would otherwise pile up copies of it.
     */
    public void boardUnreadable(Reason cause) {
        unreadable.raise(warnings, new Warning(UNREADABLE_WARNING.formatted(cause.value())));
    }

    private void warnOfPolling(Reason cause, Duration every) {
        warnings.raise(new Warning(POLLING_WARNING.formatted(cause.value(), every.toSeconds())));
    }

    public Board boardFor(WorkingDirectory from, Set<TicketBranch> merged) {
        Project project = locator.locate(from);
        List<BacklogEntry> entries = backlog.entries(project);
        List<Worktree> worktrees = copies.worktrees(project);
        return board(
                project,
                entries.stream()
                        .map(entry -> standingFor(project, worktrees, merged, entry))
                        .toList());
    }

    /** The copy that stands for a ticket: its root copy once that reads done, else the branch's, if it has one. */
    private BacklogEntry standingFor(
            Project project, List<Worktree> worktrees, Set<TicketBranch> merged, BacklogEntry root) {
        if (!(root instanceof TicketFile rootCopy) || rootCopy.ticket().status().finished()) {
            return root;
        }
        try {
            return branchCopy(project, worktrees, merged, rootCopy).orElse(root);
        } catch (BranchCopyUnavailableException e) {
            return UnreadableTicket.builder()
                    .file(rootCopy.file())
                    .reason(new Reason(e.getMessage()))
                    .build();
        }
    }

    /** A worktree that holds the ticket branch speaks for it, a missing file there included; only without one is an unmerged branch itself read. */
    private Optional<BacklogEntry> branchCopy(
            Project project, List<Worktree> worktrees, Set<TicketBranch> merged, TicketFile root) {
        TicketBranch branch = root.ticket().branch();
        List<Worktree> holding = worktrees.stream()
                .filter(worktree -> holds(project, worktree, branch))
                .toList();
        if (holding.isEmpty()) {
            return merged.contains(branch) ? Optional.empty() : copies.onBranch(project, branch, root.file());
        }
        return holding.stream()
                .map(worktree -> copies.inWorktree(worktree, root.file()))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean holds(Project project, Worktree worktree, TicketBranch branch) {
        boolean onBranch =
                switch (worktree.head()) {
                    case CheckedOutBranch checkedOut -> checkedOut.is(branch);
                    case DetachedHead _ -> false;
                };
        return onBranch
                || project.ticketWorktree(new BranchName(branch.value()))
                        .map(directory -> new WorktreePath(directory.path()))
                        .filter(worktree.path()::equals)
                        .isPresent();
    }

    private Board board(Project project, List<BacklogEntry> entries) {
        Map<TicketId, List<TicketFile>> byId = entries.stream()
                .flatMap(entry -> entry instanceof TicketFile read ? Stream.of(read) : Stream.empty())
                .collect(Collectors.groupingBy(read -> read.ticket().id(), LinkedHashMap::new, Collectors.toList()));
        Stream<UnreadableTicket> refused = entries.stream()
                .flatMap(entry -> entry instanceof UnreadableTicket refusedTicket ? Stream.of(refusedTicket) : Stream.empty());
        Stream<UnreadableTicket> claimedTwice = byId.values().stream()
                .flatMap(files -> files.stream().skip(1))
                .map(BoardManager::alreadyClaimed);
        return Board.builder()
                .project(project)
                .groups(groups(byId.values().stream()
                        .map(files -> withChanges(project, files.getFirst().ticket()))
                        .toList()))
                .unreadable(Stream.concat(refused, claimedTwice)
                        .sorted(Comparator.comparing(UnreadableTicket::file))
                        .toList())
                .build();
    }

    /** The branch is read only for a ticket whose changes decide what it is offered. */
    private Ticket withChanges(Project project, Ticket ticket) {
        return ticket.mayOfferDesign() ? ticket.withChanges(changes.changesOf(project, ticket.branch())) : ticket;
    }

    private static UnreadableTicket alreadyClaimed(TicketFile read) {
        return UnreadableTicket.builder()
                .file(read.file())
                .reason(new Reason("id %s is already used".formatted(read.ticket().id().value())))
                .build();
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
