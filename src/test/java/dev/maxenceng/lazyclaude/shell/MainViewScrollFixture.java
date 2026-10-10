package dev.maxenceng.lazyclaude.shell;

final class MainViewScrollFixture {

    private MainViewScrollFixture() {}

    static MainViewScroll scrollOver(int contentHeight, int viewHeight) {
        MainViewScroll scroll = new MainViewScroll();
        scroll.observe(contentHeight, viewHeight);
        return scroll;
    }

    static MainViewScroll scrollOverTwentyRowsInTen() {
        return scrollOver(20, 10);
    }

    static void scrolledDown(MainViewScroll scroll, int rows) {
        for (int pressed = 0; pressed < rows; pressed++) {
            scroll.rowDown();
        }
    }
}
