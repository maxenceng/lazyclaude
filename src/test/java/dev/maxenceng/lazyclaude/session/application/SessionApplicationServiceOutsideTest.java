package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.anOutsideSessionInKitty;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.outsideSessionApplicationService;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.transcriptReadingService;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.adding;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.addingBoth;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.bothLines;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigPort;
import dev.maxenceng.lazyclaude.session.domain.KittySettings;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.NoTranscript;
import dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentPort;
import dev.maxenceng.lazyclaude.session.domain.RemoteControlOffException;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptReadPort;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionApplicationServiceOutsideTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private WarningPort warnings;

    @Mock
    private HostPort hosts;

    @Mock
    private ProcessEnvironmentPort environments;

    @Mock
    private TerminalJumpPort jumps;

    @Mock
    private KittyConfigPort kittyConfig;

    private SessionApplicationService service;

    @BeforeEach
    void setUp() {
        service = outsideSessionApplicationService(sessions, locator, warnings, hosts, environments, jumps, kittyConfig);
    }

    @Test
    void shouldJumpToAnOutsideSessionOfTheLastPush() {
        anOutsideSessionInKitty(sessions, locator, environments);
        service.subscribe(project(), noTickets(), read -> {});

        service.jump(project(), noTickets(), sessionId());

        verify(jumps).jump(inKitty());
    }

    @Test
    void shouldReadTheSessionsWhenNothingHasBeenPushedYet() {
        anOutsideSessionInKitty(sessions, locator, environments);

        service.jump(project(), noTickets(), sessionId());

        verify(jumps).jump(inKitty());
    }

    @Test
    void shouldDoNothingForASessionLazyclaudeHosts() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(environments.of(processId())).thenReturn(kittyEnvironment());
        when(hosts.spawn(any(), any())).thenReturn(processId());
        service.startUnlinked(project(), paneSize());

        assertThat(service.jump(project(), noTickets(), sessionId())).isEmpty();

        verifyNoInteractions(jumps);
    }

    @Test
    void shouldDoNothingForASessionThatIsNoLongerLive() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(service.jump(project(), noTickets(), sessionId())).isEmpty();

        verifyNoInteractions(jumps);
    }

    @Test
    void shouldHandBackWhatKittyConfigNeedsWhenKittyRefusesRemoteControl() {
        anOutsideSessionInKitty(sessions, locator, environments);
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(kittyConfig.locate()).thenReturn(kittyConfigFile());
        when(kittyConfig.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));

        assertThat(service.jump(project(), noTickets(), sessionId()))
                .contains(addingBoth());
    }

    @Test
    void shouldAddTheRemoteControlLinesToKittyConf() {
        when(kittyConfig.locate()).thenReturn(kittyConfigFile());
        when(kittyConfig.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));

        service.addKittyRemoteControl();

        verify(kittyConfig).append(adding(bothLines()));
    }

    @Test
    void shouldReadTheTranscriptOfASessionFromTheOffset() {
        TranscriptReadPort transcripts = mock(TranscriptReadPort.class);
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start())).thenReturn(new NoTranscript());

        assertThat(transcriptReadingService(transcripts).transcriptOf(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .isEqualTo(new NoTranscript());
    }
}
