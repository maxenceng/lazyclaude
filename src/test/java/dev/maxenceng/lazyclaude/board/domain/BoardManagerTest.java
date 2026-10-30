package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.frontendChanges;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicket;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicketBuilder;
import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.tenthTicketId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class BoardManagerTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BoardWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private BranchChangesPort changes;

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
        when(backlog.entries(project())).thenReturn(List.of(ticket(), done));

        Board board = manager.boardFor(workingDirectory());

        assertThat(board.groups().get(Status.TODO.ordinal()).tickets()).containsExactly(ticket());
        assertThat(board.groups().get(Status.DONE.ordinal()).tickets()).containsExactly(done);
    }

    @Test
    void shouldOrderTicketsWithinAGroupNumerically() {
        Ticket tenth = ticketBuilder().id(tenthTicketId()).build();
        Ticket second = ticketBuilder().id(secondTicketId()).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(tenth, second, ticket()));

        assertThat(manager.boardFor(workingDirectory()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticket(), second, tenth);
    }

    @Test
    void shouldKeepAnEmptyGroupForAStatusWithNoTickets() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        assertThat(manager.boardFor(workingDirectory()).groups().get(Status.DRAFT.ordinal()).tickets()).isEmpty();
    }

    @Test
    void shouldOrderUnreadableTicketsByNumber() {
        UnreadableTicket first = unreadableTicketBuilder().id(tenthTicketId()).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(unreadableTicket(), first));

        assertThat(manager.boardFor(workingDirectory()).unreadable()).containsExactly(unreadableTicket(), first);
    }

    @Test
    void shouldNotBeAbleToChangeTheTicketsOfAGroup() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        List<Ticket> todo = manager.boardFor(workingDirectory()).groups().get(Status.TODO.ordinal()).tickets();

        assertThatThrownBy(todo::clear).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldLetTheLocatorsFailureThrough() {
        when(locator.locate(workingDirectory())).thenThrow(new ProjectNotFoundException("not here"));

        assertThatThrownBy(() -> manager.boardFor(workingDirectory())).isExactlyInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void shouldReadWhatAnInProgressTicketsBranchChangesAndKeepItOnTheTicket() {
        Ticket inProgress = ticketBuilder().status(Status.IN_PROGRESS).build();
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(inProgress));
        when(changes.changesOf(project(), ticketBranch())).thenReturn(frontendChanges());

        assertThat(manager.boardFor(workingDirectory()).groups().get(Status.IN_PROGRESS.ordinal()).tickets())
                .containsExactly(inProgress.withChanges(frontendChanges()));
    }

    @Test
    void shouldReadNoBranchChangesForTicketsNotInProgress() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        manager.boardFor(workingDirectory());

        verifyNoInteractions(changes);
    }

    @Test
    void shouldSayWhyAProjectHasNoTicketsWhenItsIssuesCannotBeRead() {
        when(locator.locate(workingDirectory())).thenReturn(project());

        assertThat(manager.unavailableBoard(workingDirectory(), reason()))
                .isEqualTo(UnavailableBoard.builder().project(project()).reason(reason()).build());
    }

    @Test
    void shouldTellTheWatchWhenTheBoardCannotBeRead() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException(reason().value()));

        assertThatThrownBy(() -> manager.boardFor(workingDirectory())).isExactlyInstanceOf(BacklogUnavailableException.class);
        verify(watch).forget();
    }

    @Test
    void shouldTellTheWatchNothingWhenTheBoardIsRead() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        manager.boardFor(workingDirectory());

        verifyNoInteractions(watch);
    }

    @Test
    void shouldWatchTheProjectTheWorkingDirectoryBelongsTo() {
        Runnable changed = () -> {};
        when(locator.locate(workingDirectory())).thenReturn(project());

        manager.watch(workingDirectory(), changed);

        verify(watch).watch(project(), changed);
    }
}
