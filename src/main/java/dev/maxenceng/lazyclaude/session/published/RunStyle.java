package dev.maxenceng.lazyclaude.session.published;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** How a run of text looks: its colours and attributes. */
public record RunStyle(CellColour foreground, CellColour background, List<TextAttribute> attributes) {

    public RunStyle {
        Assert.notNull("foreground", foreground);
        Assert.notNull("background", background);
        Assert.notNull("attributes", attributes);
    }

    public static RunStyleBuilder builder() {
        return new RunStyleBuilder();
    }

    public static final class RunStyleBuilder {
        private CellColour foreground = new DefaultColour();
        private CellColour background = new DefaultColour();
        private List<TextAttribute> attributes = List.of();

        private RunStyleBuilder() {}

        public RunStyleBuilder foreground(CellColour foreground) {
            this.foreground = foreground;
            return this;
        }

        public RunStyleBuilder background(CellColour background) {
            this.background = background;
            return this;
        }

        public RunStyleBuilder attributes(List<TextAttribute> attributes) {
            this.attributes = attributes;
            return this;
        }

        public RunStyle build() {
            return new RunStyle(foreground, background, attributes);
        }
    }
}
