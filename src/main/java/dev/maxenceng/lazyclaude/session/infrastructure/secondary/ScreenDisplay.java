package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import com.jediterm.terminal.CursorShape;
import com.jediterm.terminal.TerminalDisplay;
import com.jediterm.terminal.emulator.mouse.MouseFormat;
import com.jediterm.terminal.emulator.mouse.MouseMode;
import com.jediterm.terminal.model.TerminalSelection;

/**
 * What the child has switched on in its terminal, which decides how input is encoded for it: whether it takes bracketed paste,
 * which mouse events it wants and in which format, and whether its cursor shows. The emulator draws into its own buffer, so
 * everything else it announces is ignored.
 */
final class ScreenDisplay implements TerminalDisplay {

    private volatile boolean cursorVisible = true;
    private volatile boolean bracketedPaste;
    private volatile MouseMode mouseMode = MouseMode.MOUSE_REPORTING_NONE;
    private volatile MouseFormat mouseFormat = MouseFormat.MOUSE_FORMAT_XTERM;
    private volatile String title = "";

    boolean cursorVisible() {
        return cursorVisible;
    }

    boolean bracketedPaste() {
        return bracketedPaste;
    }

    /** The mouse mode the child asked for, but only when it asked for SGR coordinates, the one format lazyclaude encodes. */
    MouseMode mouseMode() {
        return mouseFormat == MouseFormat.MOUSE_FORMAT_SGR ? mouseMode : MouseMode.MOUSE_REPORTING_NONE;
    }

    @Override
    public void setCursor(int x, int y) {}

    @Override
    public void setCursorShape(CursorShape cursorShape) {}

    @Override
    public void beep() {}

    @Override
    public void scrollArea(int scrollRegionTop, int scrollRegionSize, int dy) {}

    @Override
    public void setCursorVisible(boolean isCursorVisible) {
        this.cursorVisible = isCursorVisible;
    }

    @Override
    public void useAlternateScreenBuffer(boolean useAlternateScreenBuffer) {}

    @Override
    public String getWindowTitle() {
        return title;
    }

    @Override
    public void setWindowTitle(String windowTitle) {
        this.title = windowTitle;
    }

    @Override
    public TerminalSelection getSelection() {
        return null;
    }

    @Override
    public void terminalMouseModeSet(MouseMode mode) {
        this.mouseMode = mode;
    }

    @Override
    public void setMouseFormat(MouseFormat format) {
        this.mouseFormat = format;
    }

    @Override
    public boolean ambiguousCharsAreDoubleWidth() {
        return false;
    }

    @Override
    public void setBracketedPasteMode(boolean bracketedPasteModeEnabled) {
        this.bracketedPaste = bracketedPasteModeEnabled;
    }
}
