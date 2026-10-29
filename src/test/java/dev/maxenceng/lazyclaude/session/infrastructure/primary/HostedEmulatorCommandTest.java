package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.startedOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resizePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.typingPayload;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;

@ExtendWith(MockitoExtension.class)
class HostedEmulatorCommandTest {

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
    void shouldWriteAKeyAsTheBytesATerminalSends() {
        RunningSession session = startedOn(hosts, service);

        emulating.send(typingPayload(session.id()));

        verify(hosts).write(session.id(), ptyBytesOf("a"));
    }

    @Test
    void shouldSendArrowsAsCursorSequencesUntilTheChildAsksForApplicationKeys() {
        RunningSession session = startedOn(hosts, service);

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.pressed(NamedKey.UP)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[A"));
    }

    @Test
    void shouldSendArrowsAsApplicationKeysOnceTheChildAskedForThem() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?1hready"));
        screenContaining(emulators, session.id(), "ready");

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.pressed(NamedKey.UP)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001bOA"));
    }

    @Test
    void shouldSendAModifiedKeyAsItsControlByte() {
        RunningSession session = startedOn(hosts, service);

        emulating.send(new HostedInputPayload(
                session.id(), EmulatorInputPayloadFixture.typed('c', Modifiers.of(Modifier.CTRL))));

        verify(hosts).write(session.id(), new PtyBytes(new byte[] {3}));
    }

    @Test
    void shouldPasteBareUntilTheChildAsksForBrackets() {
        RunningSession session = startedOn(hosts, service);

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.pasted("plain")));

        verify(hosts).write(session.id(), ptyBytesOf("plain"));
    }

    @Test
    void shouldPasteInsideBracketsOnceTheChildAskedForThem() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?2004hready"));
        screenContaining(emulators, session.id(), "ready");

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.pasted("x")));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[200~x\u001b[201~"));
    }

    @Test
    void shouldSendTheMouseOnceTheChildAskedForIt() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("\u001b[?1000h\u001b[?1006hready"));
        screenContaining(emulators, session.id(), "ready");

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.leftPressAt(4, 2)));

        verify(hosts).write(session.id(), ptyBytesOf("\u001b[<0;5;3M"));
    }

    @Test
    void shouldWriteNothingForAMouseTheChildDidNotAskFor() throws Exception {
        RunningSession session = startedOn(hosts, service);
        session.output().accept(ptyBytesOf("ready"));
        screenContaining(emulators, session.id(), "ready");

        emulating.send(new HostedInputPayload(session.id(), EmulatorInputPayloadFixture.leftPressAt(4, 2)));

        verify(hosts, never()).write(any(), any());
    }

    @Test
    void shouldResizeTheEmulatorBeforeThePty() {
        RunningSession session = startedOn(hosts, service);
        List<Integer> rowsWhenThePtyResized = new ArrayList<>();
        doAnswer(invocation -> rowsWhenThePtyResized.add(emulators.screenOf(session.id()).rows().size()))
                .when(hosts)
                .resize(session.id(), resizedPaneSize());

        emulating.resize(resizePayload(session.id()));

        assertThat(rowsWhenThePtyResized)
                .as("what claude redraws on SIGWINCH must land in a screen of the new size")
                .containsExactly(resizedPaneSize().height().cells());
    }

    @Test
    void shouldResizeTheScreenToTheNewPane() {
        RunningSession session = startedOn(hosts, service);

        emulating.resize(resizePayload(session.id()));

        assertThat(emulators.screenOf(session.id()).rows()).hasSize(resizedPaneSize().height().cells());
        verify(hosts).resize(session.id(), resizedPaneSize());
    }

    @Test
    void shouldIgnoreInputAndResizeForASessionItNeverStarted() {
        emulating.send(typingPayload(sessionId()));
        emulating.resize(resizePayload(sessionId()));

        verify(hosts, never()).write(any(), any());
        verify(hosts, never()).resize(any(), any());
    }
}
