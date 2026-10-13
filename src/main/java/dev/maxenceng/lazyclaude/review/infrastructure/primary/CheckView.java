package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.Check;
import dev.maxenceng.lazyclaude.review.published.CheckName;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

/** One check of a pull request and where it stands. */
public record CheckView(CheckName name, CheckOutcome outcome) {

    static CheckView from(Check check) {
        return builder().name(check.name()).outcome(check.outcome()).build();
    }

    public static CheckViewBuilder builder() {
        return new CheckViewBuilder();
    }

    public static final class CheckViewBuilder {
        private CheckName name;
        private CheckOutcome outcome;

        private CheckViewBuilder() {}

        public CheckViewBuilder name(CheckName name) {
            this.name = name;
            return this;
        }

        public CheckViewBuilder outcome(CheckOutcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public CheckView build() {
            return new CheckView(name, outcome);
        }
    }
}
