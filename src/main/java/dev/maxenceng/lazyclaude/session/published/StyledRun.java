package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Consecutive cells of a row that share one style. */
public record StyledRun(RunText text, RunStyle style) {

    public StyledRun {
        Assert.notNull("text", text);
        Assert.notNull("style", style);
    }

    public static StyledRunBuilder builder() {
        return new StyledRunBuilder();
    }

    public static final class StyledRunBuilder {
        private RunText text;
        private RunStyle style = RunStyle.builder().build();

        private StyledRunBuilder() {}

        public StyledRunBuilder text(RunText text) {
            this.text = text;
            return this;
        }

        public StyledRunBuilder style(RunStyle style) {
            this.style = style;
            return this;
        }

        public StyledRun build() {
            return new StyledRun(text, style);
        }
    }
}
