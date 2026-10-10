package dev.maxenceng.lazyclaude.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.shared.domain.Project;

class BoardManagerTest {

    private static final Project PROJECT = new Project(Path.of("/work/app"));
    private static final WorkingDirectory FROM = new WorkingDirectory(Path.of("/work/app/src"));

    private static TicketFile ticket(String id, Status status) {
        return new TicketFile(
                id + ".md", Ticket.builder().id(new TicketId(id)).status(status).build());
    }

    private static Board board(BacklogEntry... entries) {
        return new BoardManager(from -> PROJECT, project -> List.of(entries)).boardFor(FROM);
    }

    private static List<String> ids(Board board, Status status) {
        return board.groups().get(status.ordinal()).tickets().stream()
                .map(ticket -> ticket.id().value())
                .toList();
    }

    @Test
    void shouldReadTheBacklogOfTheProjectTheWorkingDirectoryBelongsTo() {
        List<Project> asked = new ArrayList<>();
        BoardManager manager = new BoardManager(
                from -> from.equals(FROM) ? PROJECT : null,
                project -> {
                    asked.add(project);
                    return List.of();
                });

        manager.boardFor(FROM);

        assertThat(asked).containsExactly(PROJECT);
    }

    @Test
    void shouldHaveOneGroupPerStatusInLifecycleOrder() {
        assertThat(board().groups()).extracting(StatusGroup::status).containsExactly(Status.values());
    }

    @Test
    void shouldGroupTicketsByStatus() {
        Board board = board(ticket("A-1", Status.TODO), ticket("A-2", Status.DONE), ticket("A-3", Status.TODO));

        assertThat(ids(board, Status.TODO)).containsExactly("A-1", "A-3");
        assertThat(ids(board, Status.DONE)).containsExactly("A-2");
    }

    @Test
    void shouldOrderTicketsWithinAGroupNumerically() {
        Board board = board(ticket("PROJ-10", Status.DRAFT), ticket("PROJ-2", Status.DRAFT), ticket("PROJ-1", Status.DRAFT));

        assertThat(ids(board, Status.DRAFT)).containsExactly("PROJ-1", "PROJ-2", "PROJ-10");
    }

    @Test
    void shouldKeepAnEmptyGroupForAStatusWithNoTickets() {
        assertThat(ids(board(ticket("A-1", Status.DONE)), Status.TODO)).isEmpty();
    }

    @Test
    void shouldOrderUnreadableTicketsByFile() {
        Board board = board(new UnreadableTicket("b.md", "broken"), new UnreadableTicket("a.md", "broken too"));

        assertThat(board.unreadable()).extracting(UnreadableTicket::file).containsExactly("a.md", "b.md");
    }

    @Test
    void shouldListALaterFileClaimingAnIdAsUnreadable() {
        TicketFile first = new TicketFile("PROJ-1-a.md", Ticket.builder().id(new TicketId("PROJ-1")).status(Status.TODO).build());
        TicketFile second = new TicketFile("PROJ-1-b.md", Ticket.builder().id(new TicketId("PROJ-1")).status(Status.DONE).build());

        Board board = board(first, second);

        assertThat(ids(board, Status.TODO)).containsExactly("PROJ-1");
        assertThat(ids(board, Status.DONE)).as("one id is one ticket, so selection by id stays unambiguous").isEmpty();
        assertThat(board.unreadable()).containsExactly(new UnreadableTicket("PROJ-1-b.md", "id PROJ-1 is already used"));
    }

    @Test
    void shouldNotBeAbleToChangeTheTicketsOfAGroup() {
        Board board = board(ticket("A-1", Status.TODO));

        assertThatThrownBy(() -> board.groups().get(Status.TODO.ordinal()).tickets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldLetTheLocatorsFailureThrough() {
        BoardManager manager = new BoardManager(
                from -> {
                    throw new ProjectNotFoundException("not here");
                },
                project -> List.of());

        assertThatThrownBy(() -> manager.boardFor(FROM)).isInstanceOf(ProjectNotFoundException.class);
    }
}
