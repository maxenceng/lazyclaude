package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.frontendChanges;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicket;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.board.published.LabelNameFixture.labelNames;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class BoardQueryTest {

    private static final BoardPayload FROM_THE_PROJECT = new BoardPayload(workingDirectory().path());

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BranchChangesPort changes;

    @Mock
    private BoardWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private Runnable changed;

    @Test
    void shouldViewEveryStatusGroupInLifecycleOrder() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups())
                .extracting(StatusGroupView::status)
                .containsExactly(Status.values());
    }

    @Test
    void shouldViewTheRootOfTheProjectTheBoardWasReadFrom() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).root())
                .as("the shell hands it on to the contexts that work in the same project")
                .isEqualTo(BoardViewFixture.projectRoot());
    }

    @Test
    void shouldViewTheVerbsAnInProgressTicketIsAllowedWithDesignWhenItsBranchTouchesTheFrontend() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketBuilder().status(Status.IN_PROGRESS).build()));
        when(changes.changesOf(project(), ticketBranch())).thenReturn(frontendChanges());

        assertThat(boardQuery(locator, backlog, changes)
                        .boardFor(FROM_THE_PROJECT)
                        .groups()
                        .get(Status.IN_PROGRESS.ordinal())
                        .tickets())
                .extracting(TicketView::verbs)
                .containsExactly(List.of(Verb.REVIEW, Verb.DESIGN));
    }

    @Test
    void shouldViewATicketsFields() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder().status(Status.TODO).verbs(List.of(Verb.START)).build());
    }

    @Test
    void shouldViewTheOtherLabelsOfATicket() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketBuilder().labels(labelNames()).build()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder().status(Status.TODO).labels(labelNames()).verbs(List.of(Verb.START)).build());
    }

    @Test
    void shouldViewATicketWithNoBodyAsHavingNoBody() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketBuilder().body(new NoBody()).build()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder().status(Status.TODO).body(new NoBody()).verbs(List.of(Verb.START)).build());
    }

    @Test
    void shouldViewWhyThereAreNoTicketsWhenTheProjectHasNoGitHubRemote() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).unavailable())
                .contains(new Reason("this project has no GitHub remote, so it has no backlog"));
    }

    @Test
    void shouldViewNoReasonWhenTheIssuesAreRead() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).unavailable()).isEmpty();
    }

    @Test
    void shouldViewAnUnreadableIssueWithItsReason() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(unreadableTicket()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).unreadable())
                .containsExactly(unreadableTicketView());
    }

    @Test
    void shouldViewTheBoardReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        assertThat(boardQuery(locator, backlog).boardReadAgainFor(FROM_THE_PROJECT))
                .get()
                .extracting(board -> board.groups().get(Status.TODO.ordinal()).tickets())
                .isEqualTo(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets());
    }

    @Test
    void shouldViewNothingWhenTheBoardCannotBeReadAgain() {
        when(locator.locate(workingDirectory())).thenThrow(new ProjectNotFoundException("not here"));

        assertThat(boardQuery(locator, backlog).boardReadAgainFor(FROM_THE_PROJECT))
                .as("a board already on screen stays, rather than being replaced by a failure")
                .isEmpty();
    }

    @Test
    void shouldRunTheListenerWhenTheWatchReportsAChange() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        boardQuery(locator, backlog, changes, watch, warnings)
                .subscribe(new BoardWatchPayload(workingDirectory().path()), changed);
        ArgumentCaptor<Runnable> reported = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), reported.capture());

        reported.getValue().run();

        verify(changed).run();
    }
}
