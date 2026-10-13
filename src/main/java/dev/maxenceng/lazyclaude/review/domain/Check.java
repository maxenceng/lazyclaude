package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

/** One check run, or commit status, on a pull request's head, and where it stands. */
public record Check(CheckName name, CheckOutcome outcome) {

    public Check {
        Assert.notNull("check name", name);
        Assert.notNull("check outcome", outcome);
    }

    public static CheckBuilder builder() {
        return new CheckBuilder();
    }

    public static final class CheckBuilder {
        private CheckName name;
        private CheckOutcome outcome;

        private CheckBuilder() {}

        public CheckBuilder name(CheckName name) {
            this.name = name;
            return this;
        }

        public CheckBuilder outcome(CheckOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Check build() {
            return new Check(name, outcome);
        }
    }
}
