package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

public final class WarningViewFixture {

    private WarningViewFixture() {}

    public static WarningView warningView() {
        return WarningView.builder()
                .text("docs: cannot watch docs/ (inotify limit); checking every 5 s")
                .count(1)
                .build();
    }

    public static WarningView twoWarningsView() {
        return WarningView.builder()
                .text("sessions: cannot watch the registry; checking every 2 s")
                .count(2)
                .build();
    }

    public static WarningView warningViewOf(String text, int count) {
        return WarningView.builder().text(text).count(count).build();
    }
}
