package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The size of the pane a hosted session is drawn in, which is the size of its pty. */
public record PaneSize(PaneWidth width, PaneHeight height) {

    public PaneSize {
        Assert.notNull("pane width", width);
        Assert.notNull("pane height", height);
    }

    public static PaneSizeBuilder builder() {
        return new PaneSizeBuilder();
    }

    public static final class PaneSizeBuilder {
        private PaneWidth width;
        private PaneHeight height;

        private PaneSizeBuilder() {}

        public PaneSizeBuilder width(PaneWidth width) {
            this.width = width;
            return this;
        }

        public PaneSizeBuilder height(PaneHeight height) {
            this.height = height;
            return this;
        }

        public PaneSize build() {
            return new PaneSize(width, height);
        }
    }
}
