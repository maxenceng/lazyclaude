package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.utf8Of;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenViewFixture.blankScreen;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.emulatorsLeft;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.startedOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.textOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.typingPayload;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;

@ExtendWith(MockitoExtension.class)
class HostedEmulatorQueryTest {

    private static final int PATIENCE_MILLIS = 10_000;

    @Mock
    private HostPort hosts;

    @Mock
    private Runnable drew;

    private HostedEmulatorCommand emulating;
    private HostedEmulatorQuery emulators;
    private SessionApplicationService service;

    @BeforeEach
    void setUp() {
        service = serviceOver(hosts);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        emulating = new HostedEmulatorCommand(service, running);
        emulators = new HostedEmulatorQuery(running);
    }

    @Test
    void shouldShowWhatTheSessionWrote() throws Exception {
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("hello pane"));

        assertThat(textOf(screenContaining(emulators, session.id(), "hello pane"))).startsWith("hello pane");
    }

    @Test
    void shouldKeepEachSessionsScreenToItself() throws Exception {
        RunningSession first = startedOn(hosts, service);
        RunningSession second = startedOn(hosts, service);

        first.output().accept(ptyBytesOf("only first"));
        second.output().accept(ptyBytesOf("only second"));

        assertThat(textOf(screenContaining(emulators, first.id(), "only first"))).doesNotContain("second");
        assertThat(textOf(screenContaining(emulators, second.id(), "only second"))).doesNotContain("first");
    }

    @Test
    void shouldReadAnEscapeSequenceSplitBetweenTwoReadsWhole() throws Exception {
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("a\u001b[3"));
        session.output().accept(ptyBytesOf("1mred"));

        assertThat(textOf(screenContaining(emulators, session.id(), "red")))
                .as("the emulator waits for the rest of a sequence, so none of it is drawn as text")
                .isEqualTo("ared");
    }

    @Test
    void shouldReadACharacterSplitBetweenTwoReadsWhole() throws Exception {
        RunningSession session = startedOn(hosts, service);
        byte[] accented = "é".getBytes(StandardCharsets.UTF_8);

        session.output().accept(new PtyBytes(new byte[] {accented[0]}));
        session.output().accept(new PtyBytes(new byte[] {accented[1]}));

        assertThat(textOf(screenContaining(emulators, session.id(), "é"))).isEqualTo("é");
    }

    @Test
    void shouldTellAListenerWhenASessionDrew() throws Exception {
        emulators.onScreenChange(drew);
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("drawn"));

        verify(drew, timeout(PATIENCE_MILLIS).atLeastOnce()).run();
    }

    @Test
    void shouldAnswerATerminalQueryOnThePtyOfTheSessionThatAskedIt() {
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("\u001b[c"));

        ArgumentCaptor<PtyBytes> reply = ArgumentCaptor.captor();
        verify(hosts, timeout(PATIENCE_MILLIS)).write(eq(session.id()), reply.capture());
        assertThat(utf8Of(reply.getValue())).as("the primary device attributes reply").startsWith("\u001b[?");
    }

    @Test
    void shouldShowABlankScreenForASessionItNeverStarted() {
        assertThat(emulators.screenOf(sessionId())).isEqualTo(blankScreen());
    }

    @Test
    void shouldForgetASessionOnceItsClaudeExited() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("bye"));
        screenContaining(emulators, session.id(), "bye");

        session.exit().accept(new ExitCode(0));

        assertThat(emulators.screenOf(session.id())).isEqualTo(blankScreen());
        emulating.send(typingPayload(session.id()));
        verify(hosts, never()).write(any(), any());
    }

    @Test
    void shouldStopTheEmulatorOnceItsClaudeExited() throws Exception {
        RunningSession session = startedOn(hosts, service);
        emulatorsLeft(session.id(), 1);

        session.exit().accept(new ExitCode(0));

        emulatorsLeft(session.id(), 0);
    }
}
