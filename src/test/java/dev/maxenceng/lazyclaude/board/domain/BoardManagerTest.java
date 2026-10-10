package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFileOf;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicket;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.WorkingDirectoryFixture.workingDirectory;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.tenthTicketId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;

@ExtendWith(MockitoExtension.class)
class BoardManagerTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @InjectMocks
    private BoardManager manager;

    @Test
    void shouldHaveOneGroupPerStatusInLifecycleOrder() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(manager.boardFor(workingDirectory()).groups())
                .extracting(StatusGroup::status)
                .containsExactly(Status.values());
    }

    @Test
    void shouldCarryTheProjectItWasReadFrom() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(manager.boardFor(workingDirectory()).project()).isEqualTo(project());
    }

    @Test
    void shouldGroupTicketsByStatus() {
        Ticket done = ticketBuilder().id(secondTicketId()).status(Status.DONE).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticket()), ticketFileOf(done)));

        Board board = manager.boardFor(workingDirectory());

        assertThat(board.groups().get(Status.TODO.ordinal()).tickets()).containsExactly(ticket());
        assertThat(board.groups().get(Status.DONE.ordinal()).tickets()).containsExactly(done);
    }

    @Test
    void shouldOrderTicketsWithinAGroupNumerically() {
        Ticket tenth = ticketBuilder().id(tenthTicketId()).build();
        Ticket second = ticketBuilder().id(secondTicketId()).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(tenth), ticketFileOf(second), ticketFileOf(ticket())));

        assertThat(manager.boardFor(workingDirectory()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticket(), second, tenth);
    }

    @Test
    void shouldKeepAnEmptyGroupForAStatusWithNoTickets() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticket())));

        assertThat(manager.boardFor(workingDirectory()).groups().get(Status.DRAFT.ordinal()).tickets()).isEmpty();
    }

    @Test
    void shouldOrderUnreadableTicketsByFile() {
        UnreadableTicket first = unreadableTicketBuilder().file(fileName()).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(unreadableTicket(), first));

        assertThat(manager.boardFor(workingDirectory()).unreadable()).containsExactly(first, unreadableTicket());
    }

    @Test
    void shouldListALaterFileClaimingAnIdAsUnreadable() {
        TicketFile second = TicketFile.builder().file(new FileName("PROJ-1-b.md")).ticket(ticket()).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticket()), second));

        Board board = manager.boardFor(workingDirectory());

        assertThat(board.groups().get(Status.TODO.ordinal()).tickets())
                .as("one id is one ticket, so selection by id stays unambiguous")
                .containsExactly(ticket());
        assertThat(board.unreadable())
                .containsExactly(UnreadableTicket.builder()
                        .file(second.file())
                        .reason(new Reason("id PROJ-1 is already used"))
                        .build());
    }

    @Test
    void shouldNotBeAbleToChangeTheTicketsOfAGroup() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticket())));

        List<Ticket> todo = manager.boardFor(workingDirectory()).groups().get(Status.TODO.ordinal()).tickets();

        assertThatThrownBy(todo::clear).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldLetTheLocatorsFailureThrough() {
        when(locator.locate(workingDirectory())).thenThrow(new ProjectNotFoundException("not here"));

        assertThatThrownBy(() -> manager.boardFor(workingDirectory())).isExactlyInstanceOf(ProjectNotFoundException.class);
    }
}
