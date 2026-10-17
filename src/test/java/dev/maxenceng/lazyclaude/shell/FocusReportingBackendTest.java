package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shell.ScriptedBackendFixture.backendTyping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.EventParser;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

class FocusReportingBackendTest {

    private static final int TIMEOUT_MS = 100;

    @Test
    void shouldLetALoneEscapeThroughAsTheEscapeKey() throws IOException {
        Event event = EventParser.readEvent(
                new FocusReportingBackend(backendTyping("\033"), new TerminalFocus(mock(Runnable.class))), TIMEOUT_MS);

        assertThat(event).isInstanceOfSatisfying(KeyEvent.class, key -> assertThat(key.isKey(KeyCode.ESCAPE)).isTrue());
    }

    @Test
    void shouldLetOtherEscapeSequencesThroughUnchanged() throws IOException {
        Event event = EventParser.readEvent(
                new FocusReportingBackend(backendTyping("\033[A"), new TerminalFocus(mock(Runnable.class))), TIMEOUT_MS);

        assertThat(event).isInstanceOfSatisfying(KeyEvent.class, key -> assertThat(key.isKey(KeyCode.UP)).isTrue());
    }

    @Test
    void shouldLetAnAltChordThrough() throws IOException {
        Event event = EventParser.readEvent(
                new FocusReportingBackend(backendTyping("\033j"), new TerminalFocus(mock(Runnable.class))), TIMEOUT_MS);

        assertThat(event).isInstanceOfSatisfying(KeyEvent.class, key -> {
            assertThat(key.hasAlt()).isTrue();
            assertThat(key.isChar('j')).isTrue();
        });
    }

    @Test
    void shouldLetASequenceWithParametersThrough() throws IOException {
        Event event = EventParser.readEvent(
                new FocusReportingBackend(backendTyping("\033[1;5A"), new TerminalFocus(mock(Runnable.class))), TIMEOUT_MS);

        assertThat(event).isInstanceOfSatisfying(KeyEvent.class, key -> {
            assertThat(key.isKey(KeyCode.UP)).isTrue();
            assertThat(key.hasCtrl()).isTrue();
        });
    }

    @Test
    void shouldStripAFocusInAndNotEatTheKeyThatFollowsIt() throws IOException {
        TerminalFocus focus = new TerminalFocus(mock(Runnable.class));
        focus.lost();
        FocusReportingBackend backend = new FocusReportingBackend(backendTyping("\033[Ia"), focus);

        Event event = EventParser.readEvent(backend, TIMEOUT_MS);

        assertThat(event).isInstanceOfSatisfying(KeyEvent.class, key -> assertThat(key.isChar('a')).isTrue());
        assertThat(focus.focused()).isTrue();
    }

    @Test
    void shouldStripAFocusOutAndReportIt() throws IOException {
        TerminalFocus focus = new TerminalFocus(mock(Runnable.class));
        FocusReportingBackend backend = new FocusReportingBackend(backendTyping("\033[O"), focus);

        Event event = EventParser.readEvent(backend, TIMEOUT_MS);

        assertThat(event).as("a focus report is no key").isNull();
        assertThat(focus.focused()).isFalse();
    }

    @Test
    void shouldReportEveryFocusChangeInOrderBetweenKeys() throws IOException {
        TerminalFocus focus = new TerminalFocus(mock(Runnable.class));
        FocusReportingBackend backend = new FocusReportingBackend(backendTyping("a\033[Ob\033[Ic"), focus);

        EventParser.readEvent(backend, TIMEOUT_MS);
        EventParser.readEvent(backend, TIMEOUT_MS);
        assertThat(focus.focused()).isFalse();
        EventParser.readEvent(backend, TIMEOUT_MS);

        assertThat(focus.focused()).isTrue();
    }

    @Test
    void shouldAskForFocusReportsOnEnteringTheAlternateScreenAndStopOnLeaving() throws IOException {
        Backend delegate = mock(Backend.class);
        FocusReportingBackend backend = new FocusReportingBackend(delegate, new TerminalFocus(mock(Runnable.class)));

        backend.enterAlternateScreen();
        backend.leaveAlternateScreen();

        InOrder order = inOrder(delegate);
        order.verify(delegate).enterAlternateScreen();
        order.verify(delegate).writeRaw("\033[?1004h");
        order.verify(delegate).writeRaw("\033[?1004l");
        order.verify(delegate).leaveAlternateScreen();
    }

    @Test
    void shouldForwardWhatItDoesNotTouch() throws IOException {
        Backend delegate = mock(Backend.class);

        new FocusReportingBackend(delegate, new TerminalFocus(mock(Runnable.class))).enableMouseCapture(true);

        verify(delegate).enableMouseCapture(true);
    }
}
