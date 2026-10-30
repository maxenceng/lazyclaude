package dev.maxenceng.lazyclaude.board.application;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.UnavailableBoard;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceTest {

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

    @InjectMocks
    private BoardApplicationService service;

    @Test
    void shouldReadTheBoardOfTheProjectTheWorkingDirectoryBelongsTo() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        assertThat(service.boardFor(workingDirectory()))
                .asInstanceOf(InstanceOfAssertFactories.type(Board.class))
                .extracting(board -> board.groups().get(Status.TODO.ordinal()).tickets())
                .isEqualTo(List.of(ticket()));
    }

    @Test
    void shouldSayWhyThereIsNoBoardWhenTheProjectHasNoGitHubRemote() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException());

        assertThat(service.boardFor(workingDirectory()))
                .isEqualTo(UnavailableBoard.builder()
                        .project(project())
                        .reason(new Reason("this project has no GitHub remote, so it has no backlog"))
                        .build());
    }

    @Test
    void shouldSayWhyThereIsNoBoardWhenGitHubCannotBeReached() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException("gh is not logged in; run gh auth login"));

        assertThat(service.boardFor(workingDirectory()))
                .isEqualTo(UnavailableBoard.builder()
                        .project(project())
                        .reason(new Reason("gh is not logged in; run gh auth login"))
                        .build());
    }

    @Test
    void shouldLetTheLocatorsFailureThroughWhenThereIsNoProject() {
        when(locator.locate(workingDirectory())).thenThrow(new ProjectNotFoundException("not here"));

        assertThatThrownBy(() -> service.boardFor(workingDirectory())).isExactlyInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void shouldReadTheBoardAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        assertThat(service.boardReadAgainFor(workingDirectory()))
                .map(board -> board.groups().get(Status.TODO.ordinal()).tickets())
                .contains(List.of(ticket()));
    }

    @Test
    void shouldReadNothingAndRaiseAWarningWhenTheBoardCannotBeReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException());

        assertThat(service.boardReadAgainFor(workingDirectory())).isEmpty();

        assertThat(raisedTexts(warnings))
                .containsExactly("board: this project has no GitHub remote, so it has no backlog; showing the board as last read");
    }

    @Test
    void shouldRaiseNothingWhenTheBoardIsReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        service.boardReadAgainFor(workingDirectory());

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldNotRaiseTheWarningAgainWhileTheBoardKeepsFailing() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException());

        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());

        assertThat(raisedTexts(warnings))
                .as("naming the mode again would only pile up copies of it")
                .hasSize(1);
    }

    @Test
    void shouldRunTheListenerWhenTheWatchReportsAChange() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        service.subscribe(workingDirectory(), changed);
        ArgumentCaptor<Runnable> reported = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), reported.capture());

        reported.getValue().run();

        verify(changed).run();
    }

    @Test
    void shouldWithdrawTheWarningWhenTheBoardIsReadAgainAfterAFailedRead() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException()).thenReturn(List.of());
        service.boardReadAgainFor(workingDirectory());

        service.boardReadAgainFor(workingDirectory());

        verify(warnings)
                .withdraw(new Warning("board: this project has no GitHub remote, so it has no backlog; showing the board as last read"));
    }

    @Test
    void shouldWithdrawNothingWhenTheBoardIsReadAgainWithNoEarlierFailure() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldWithdrawTheWarningOnlyOnceHoweverManyGoodReadsFollow() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException()).thenReturn(List.of());
        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());

        service.boardReadAgainFor(workingDirectory());

        verify(warnings, times(1)).withdraw(any());
    }

    @Test
    void shouldRaiseAgainWhenTheBoardFailsAfterTheWarningWasWithdrawn() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project()))
                .thenThrow(new BacklogNotFoundException())
                .thenReturn(List.of())
                .thenThrow(new BacklogNotFoundException());
        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());

        service.boardReadAgainFor(workingDirectory());

        assertThat(raisedTexts(warnings)).as("a new failure is a new warning once the old one is gone").hasSize(2);
    }

    @Test
    void shouldKeepTheWarningWhenTheBoardIsStillUnreadable() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException());

        service.boardReadAgainFor(workingDirectory());
        service.boardReadAgainFor(workingDirectory());

        verify(warnings, never()).withdraw(any());
    }
}
