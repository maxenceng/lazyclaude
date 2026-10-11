package dev.maxenceng.lazyclaude.session.published;

public final class PaneSizeFixture {

    private PaneSizeFixture() {}

    public static PaneSize paneSize() {
        return paneSizeOf(100, 30);
    }

    public static PaneSize resizedPaneSize() {
        return paneSizeOf(120, 40);
    }

    public static PaneSize paneSizeOf(int columns, int rows) {
        return PaneSize.builder()
                .width(new PaneWidth(columns))
                .height(new PaneHeight(rows))
                .build();
    }
}
