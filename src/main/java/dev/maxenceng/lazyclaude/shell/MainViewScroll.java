package dev.maxenceng.lazyclaude.shell;

/**
 * Where the main view is scrolled to. The offset is the first visible row of the whole detail; the
 * heights come from the last render, so it can never rest beyond the last row that fits.
 */
final class MainViewScroll {

    private static final int ONE_ROW = 1;
    private static final int HALF = 2;
    private static final int WHEEL_ROWS = 3;

    private int offset;
    private int contentHeight;
    private int viewHeight;

    int offset() {
        return offset;
    }

    /** Takes the heights the last render saw, pulling the offset back if the content no longer reaches it. */
    void observe(int contentHeight, int viewHeight) {
        this.contentHeight = contentHeight;
        this.viewHeight = viewHeight;
        offset = Math.min(offset, maxOffset());
    }

    void rowDown() {
        move(ONE_ROW);
    }

    void rowUp() {
        move(-ONE_ROW);
    }

    /** One notch of the wheel, down or up. */
    void wheel(boolean down) {
        move(down ? WHEEL_ROWS : -WHEEL_ROWS);
    }

    void halfPageDown() {
        move(halfPage());
    }

    void halfPageUp() {
        move(-halfPage());
    }

    void top() {
        move(-offset);
    }

    void bottom() {
        offset = maxOffset();
    }

    void reset() {
        top();
    }

    private void move(int rows) {
        offset = Math.clamp((long) offset + rows, 0, maxOffset());
    }

    private int halfPage() {
        return Math.max(ONE_ROW, viewHeight / HALF);
    }

    private int maxOffset() {
        return Math.max(0, contentHeight - viewHeight);
    }
}
