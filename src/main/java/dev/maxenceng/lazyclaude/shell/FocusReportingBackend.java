package dev.maxenceng.lazyclaude.shell;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;

/**
 * Asks the terminal to report focus ({@code ?1004}) and strips the reports from the input before TamboUI's parser sees them.
 * That parser reads {@code ESC [ I} and {@code ESC [ O} as an unknown sequence and takes the next byte as its terminator, so a
 * focus change would swallow the next keystroke and could not be told in from out. Every other byte, a lone {@code ESC}
 * and the other {@code ESC [} sequences included, reaches it unchanged.
 */
// deferred: sits below TamboUI's parser because 0.5.0 cannot decode focus events — remove it once TamboUI reports them itself.
// deferred: an ESC pressed right before a focus report leaves the report's "[I" to be typed — close it if that shows up in use.
final class FocusReportingBackend implements Backend {

    private static final int ESC = 27;
    private static final int CSI = '[';
    private static final int FOCUS_IN = 'I';
    private static final int FOCUS_OUT = 'O';
    private static final int LOOKAHEAD_MS = 50;
    private static final String REPORT_FOCUS = "\033[?1004h";
    private static final String STOP_REPORTING_FOCUS = "\033[?1004l";

    private final Backend delegate;
    private final TerminalFocus focus;
    private final Deque<Integer> pushedBack = new ArrayDeque<>();

    FocusReportingBackend(Backend delegate, TerminalFocus focus) {
        this.delegate = delegate;
        this.focus = focus;
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        int first = next(timeoutMs);
        return first == ESC ? afterEscape(timeoutMs) : first;
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        return pushedBack.isEmpty() ? delegate.peek(timeoutMs) : pushedBack.peekFirst();
    }

    /** The ESC just read is either a focus report, which vanishes, or whatever else it began, which goes on as it was. */
    private int afterEscape(int timeoutMs) throws IOException {
        if (peek(LOOKAHEAD_MS) != CSI) {
            return ESC;
        }
        next(LOOKAHEAD_MS);
        int third = peek(LOOKAHEAD_MS);
        if (third != FOCUS_IN && third != FOCUS_OUT) {
            pushedBack.addFirst(CSI);
            return ESC;
        }
        next(LOOKAHEAD_MS);
        if (third == FOCUS_IN) {
            focus.gained();
        } else {
            focus.lost();
        }
        return read(timeoutMs);
    }

    private int next(int timeoutMs) throws IOException {
        return pushedBack.isEmpty() ? delegate.read(timeoutMs) : pushedBack.pollFirst();
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        delegate.enterAlternateScreen();
        delegate.writeRaw(REPORT_FOCUS);
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        delegate.writeRaw(STOP_REPORTING_FOCUS);
        delegate.leaveAlternateScreen();
    }

    @Override
    public void draw(DiffResult diff) throws IOException {
        delegate.draw(diff);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void clear() throws IOException {
        delegate.clear();
    }

    @Override
    public Size size() throws IOException {
        return delegate.size();
    }

    @Override
    public void showCursor() throws IOException {
        delegate.showCursor();
    }

    @Override
    public void hideCursor() throws IOException {
        delegate.hideCursor();
    }

    @Override
    public Position getCursorPosition() throws IOException {
        return delegate.getCursorPosition();
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
        delegate.setCursorPosition(position);
    }

    @Override
    public void enableRawMode() throws IOException {
        delegate.enableRawMode();
    }

    @Override
    public void disableRawMode() throws IOException {
        delegate.disableRawMode();
    }

    @Override
    public void enableMouseCapture() throws IOException {
        delegate.enableMouseCapture();
    }

    @Override
    public void enableMouseCapture(boolean motion) throws IOException {
        delegate.enableMouseCapture(motion);
    }

    @Override
    public void disableMouseCapture() throws IOException {
        delegate.disableMouseCapture();
    }

    @Override
    public void beginSynchronizedUpdate() throws IOException {
        delegate.beginSynchronizedUpdate();
    }

    @Override
    public void endSynchronizedUpdate() throws IOException {
        delegate.endSynchronizedUpdate();
    }

    @Override
    public void enableBracketedPaste() throws IOException {
        delegate.enableBracketedPaste();
    }

    @Override
    public void disableBracketedPaste() throws IOException {
        delegate.disableBracketedPaste();
    }

    @Override
    public void scrollUp(int lines) throws IOException {
        delegate.scrollUp(lines);
    }

    @Override
    public void scrollDown(int lines) throws IOException {
        delegate.scrollDown(lines);
    }

    @Override
    public void writeRaw(byte[] data) throws IOException {
        delegate.writeRaw(data);
    }

    @Override
    public void writeRaw(String data) throws IOException {
        delegate.writeRaw(data);
    }

    @Override
    public void writeRaw(CharSequence data) throws IOException {
        delegate.writeRaw(data);
    }

    @Override
    public void onResize(Runnable handler) {
        delegate.onResize(handler);
    }

    @Override
    public void insertLines(int n) throws IOException {
        delegate.insertLines(n);
    }

    @Override
    public void deleteLines(int n) throws IOException {
        delegate.deleteLines(n);
    }

    @Override
    public void moveCursorUp(int n) throws IOException {
        delegate.moveCursorUp(n);
    }

    @Override
    public void moveCursorDown(int n) throws IOException {
        delegate.moveCursorDown(n);
    }

    @Override
    public void moveCursorRight(int n) throws IOException {
        delegate.moveCursorRight(n);
    }

    @Override
    public void moveCursorLeft(int n) throws IOException {
        delegate.moveCursorLeft(n);
    }

    @Override
    public void eraseToEndOfLine() throws IOException {
        delegate.eraseToEndOfLine();
    }

    @Override
    public void setWindowTitle(String title) throws IOException {
        delegate.setWindowTitle(title);
    }

    @Override
    public void saveWindowTitle() throws IOException {
        delegate.saveWindowTitle();
    }

    @Override
    public void restoreWindowTitle() throws IOException {
        delegate.restoreWindowTitle();
    }

    @Override
    public void carriageReturn() throws IOException {
        delegate.carriageReturn();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
