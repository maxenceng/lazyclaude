package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import dev.maxenceng.lazyclaude.shared.domain.Warnings;

/** The warnings as the status line shows them: the newest one's text, and how many have been raised. */
public record WarningView(String text, int count) {

    static WarningView from(Warnings warnings) {
        return builder().text(warnings.newest().text()).count(warnings.count()).build();
    }

    public static WarningViewBuilder builder() {
        return new WarningViewBuilder();
    }

    public static final class WarningViewBuilder {
        private String text;
        private int count;

        private WarningViewBuilder() {}

        public WarningViewBuilder text(String text) {
            this.text = text;
            return this;
        }

        public WarningViewBuilder count(int count) {
            this.count = count;
            return this;
        }

        public WarningView build() {
            return new WarningView(text, count);
        }
    }
}
