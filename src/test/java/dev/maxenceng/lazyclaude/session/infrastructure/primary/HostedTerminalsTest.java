package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.utf8Of;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resizePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.typingPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.emulatorsLeft;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.startedOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.textOf;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;

@ExtendWith(MockitoExtension.class)
class HostedTerminalsTest {

    private static final int PATIENCE_MILLIS = 10_000;

    @Mock
    private HostPort hosts;

    @Mock
    private Runnable drew;

    private HostedTerminals terminals;
    private SessionApplicationService service;

    @BeforeEach
    void setUp() {
        service = serviceOver(hosts);
        terminals = new HostedTerminals(service);
    }

    @Test
    void shouldShowWhatTheSessionWrote() throws Exception {
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("hello pane"));

        assertThat(textOf(screenContaining(terminals, session.id(), "hello pane"))).startsWith("hello pane");
    }

    @Test
    void shouldKeepEachSessionsScreenToItself() throws Exception {
        RunningSession first = startedOn(hosts, service);
        RunningSession second = startedOn(hosts, service);

        first.output().accept(ptyBytesOf("only first"));
        second.output().accept(ptyBytesOf("only second"));

        assertThat(textOf(screenContaining(terminals, first.id(), "only first"))).doesNotContain("second");
        assertThat(textOf(screenContaining(terminals, second.id(), "only second"))).doesNotContain("first");
    }

    @Test
    void shouldReadAnEscapeSequenceSplitBetweenTwoReadsWhole() throws Exception {
        RunningSession session = startedOn(hosts, service);

        session.output().accept(ptyBytesOf("a\u001b[3"));
        session.output().accept(ptyBytesOf("1mred"));

        assertThat(textOf(screenContaining(terminals, session.id(), "red")))
                .as("the emulator waits for the rest of a sequence, so none of it is drawn as text")
                .isEqualTo("ared");
    }

    @Test
    void shouldReadACharacterSplitBetweenTwoReadsWhole() throws Exception {
        RunningSession session = startedOn(hosts, service);
        byte[] accented = "é".getBytes(StandardCharsets.UTF_8);

        session.output().accept(new PtyBytes(new byte[] {accented[0]}));
        session.output().accept(new PtyBytes(new byte[] {accented[1]}));

        assertThat(textOf(screenContaining(terminals, session.id(), "é"))).isEqualTo("é");
    }

    @Test
    void shouldTellAListenerWhenASessionDrew() throws Exception {
        terminals.onScreenChange(drew);
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
    void shouldWriteAKeyAsTheBytesATerminalSends() {
        RunningSession session = startedOn(hosts, service);

        terminals.send(typingPayload(session.id()));

        verify(hosts).write(session.id(), ptyBytesOf("a"));
    }

    @Test
    void shouldSendArrowsAsCursorSequencesUntilTheChildAsksForApplicationKeys() {
        RunningSession session = startedOn(hosts, service);

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.pressed(NamedKey.UP)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[A"));
    }

    @Test
    void shouldSendArrowsAsApplicationKeysOnceTheChildAskedForThem() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?1hready"));
        screenContaining(terminals, session.id(), "ready");

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.pressed(NamedKey.UP)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001bOA"));
    }

    @Test
    void shouldSendAModifiedKeyAsItsControlByte() {
        RunningSession session = startedOn(hosts, service);

        terminals.send(new HostedInputPayload(
                session.id(), TerminalInputFixture.typed('c', Modifiers.of(Modifier.CTRL))));

        verify(hosts).write(session.id(), new PtyBytes(new byte[] {3}));
    }

    @Test
    void shouldPasteBareUntilTheChildAsksForBrackets() {
        RunningSession session = startedOn(hosts, service);

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.pasted("plain")));

        verify(hosts).write(session.id(), ptyBytesOf("plain"));
    }

    @Test
    void shouldPasteInsideBracketsOnceTheChildAskedForThem() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?2004hready"));
        screenContaining(terminals, session.id(), "ready");

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.pasted("x")));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[200~x\u001b[201~"));
    }

    @Test
    void shouldSendTheMouseOnceTheChildAskedForIt() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?1000h\u001b[?1006hready"));
        screenContaining(terminals, session.id(), "ready");

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.leftPressAt(4, 2)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[<0;5;3M"));
    }

    @Test
    void shouldWriteNothingForAMouseTheChildDidNotAskFor() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("ready"));
        screenContaining(terminals, session.id(), "ready");

        terminals.send(new HostedInputPayload(session.id(), TerminalInputFixture.leftPressAt(4, 2)));

        verify(hosts, never()).write(any(), any());
    }

    @Test
    void shouldResizeTheEmulatorBeforeThePty() {
        RunningSession session = startedOn(hosts, service);
        List<Integer> rowsWhenThePtyResized = new ArrayList<>();
        doAnswer(invocation -> rowsWhenThePtyResized.add(terminals.screenOf(session.id()).rows().size()))
                .when(hosts)
                .resize(session.id(), resizedPaneSize());

        terminals.resize(resizePayload(session.id()));

        assertThat(rowsWhenThePtyResized)
                .as("what claude redraws on SIGWINCH must land in a screen of the new size")
                .containsExactly(resizedPaneSize().height().cells());
    }

    @Test
    void shouldResizeTheScreenToTheNewPane() {
        RunningSession session = startedOn(hosts, service);

        terminals.resize(resizePayload(session.id()));

        assertThat(terminals.screenOf(session.id()).rows()).hasSize(resizedPaneSize().height().cells());
        verify(hosts).resize(session.id(), resizedPaneSize());
    }

    @Test
    void shouldShowABlankScreenForASessionItNeverStarted() {
        assertThat(terminals.screenOf(sessionId())).isEqualTo(TerminalScreen.blank());
    }

    @Test
    void shouldIgnoreInputAndResizeForASessionItNeverStarted() {
        terminals.send(typingPayload(sessionId()));
        terminals.resize(resizePayload(sessionId()));

        verify(hosts, never()).write(any(), any());
        verify(hosts, never()).resize(any(), any());
    }

    @Test
    void shouldForgetASessionOnceItsClaudeExited() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("bye"));
        screenContaining(terminals, session.id(), "bye");

        session.exit().accept(new ExitCode(0));

        assertThat(terminals.screenOf(session.id())).isEqualTo(TerminalScreen.blank());
        terminals.send(typingPayload(session.id()));
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
