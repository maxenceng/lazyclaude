package dev.maxenceng.lazyclaude.board.application;

import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BranchCopyPort copies;

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
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(service.boardFor(workingDirectory(), Set.of()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticket());
    }

    @Test
    void shouldReadTheBoardAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(service.boardReadAgainFor(workingDirectory(), Set.of()))
                .map(board -> board.groups().get(Status.TODO.ordinal()).tickets())
                .contains(List.of(ticket()));
    }

    @Test
    void shouldReadNothingAndRaiseAWarningWhenTheBoardCannotBeReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogNotFoundException(Path.of("/x/docs/backlog")));

        assertThat(service.boardReadAgainFor(workingDirectory(), Set.of())).isEmpty();

        assertThat(raisedTexts(warnings))
                .containsExactly("board: the main worktree has no /x/docs/backlog; showing the board as last read");
    }

    @Test
    void shouldRaiseNothingWhenTheBoardIsReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        service.boardReadAgainFor(workingDirectory(), Set.of());

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldNotRaiseTheWarningAgainWhenTheBoardFailsAfterHavingBeenReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project()))
                .thenThrow(new BacklogNotFoundException(Path.of("/x/docs/backlog")))
                .thenReturn(List.of())
                .thenThrow(new BacklogNotFoundException(Path.of("/x/docs/backlog")));

        service.boardReadAgainFor(workingDirectory(), Set.of());
        service.boardReadAgainFor(workingDirectory(), Set.of());
        service.boardReadAgainFor(workingDirectory(), Set.of());

        assertThat(raisedTexts(warnings))
                .as("a warning is never withdrawn, so naming the mode again would only pile up copies of it")
                .hasSize(1);
    }

    @Test
    void shouldRunTheListenerWhenTheWatchReportsAChange() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        service.subscribe(workingDirectory(), changed);
        ArgumentCaptor<Runnable> reported = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), reported.capture(), any());

        reported.getValue().run();

        verify(changed).run();
    }
}
