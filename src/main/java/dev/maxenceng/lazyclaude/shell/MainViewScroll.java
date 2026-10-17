package dev.maxenceng.lazyclaude.shell;

/**
 * Where the main view is scrolled to. The offset is the first visible row of the whole detail; the
 * heights come from the last render, so it can never rest beyond the last row that fits. A view that tails
 * (a transcript) also follows: while it rests on the last row, content added below carries it down, and scrolling
 * up lets go until the bottom is reached again.
 */
final class MainViewScroll {

    private static final int ONE_ROW = 1;
    private static final int HALF = 2;

    private int offset;
    private boolean following = true;
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

    /** Takes the heights the last render saw like {@link #observe}, and keeps the last row in view while the view follows. */
    void follow(int contentHeight, int viewHeight) {
        this.contentHeight = contentHeight;
        this.viewHeight = viewHeight;
        offset = following ? maxOffset() : Math.min(offset, maxOffset());
    }

    void rowDown() {
        move(ONE_ROW);
    }

    void rowUp() {
        move(-ONE_ROW);
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
        following = true;
    }

    /** Back to the top, and ready to follow: the next thing shown that tails starts at its latest row. */
    void reset() {
        top();
        following = true;
    }

    private void move(int rows) {
        offset = Math.clamp((long) offset + rows, 0, maxOffset());
        following = offset >= maxOffset();
    }

    private int halfPage() {
        return Math.max(ONE_ROW, viewHeight / HALF);
    }

    private int maxOffset() {
        return Math.max(0, contentHeight - viewHeight);
    }
}
