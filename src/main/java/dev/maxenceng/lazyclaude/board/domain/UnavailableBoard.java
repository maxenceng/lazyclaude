package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** A project whose issues could not be read: it has a root, but no tickets, and the reason why. */
public record UnavailableBoard(Project project, Reason reason) implements BoardReading {

    public UnavailableBoard {
        Assert.notNull("project", project);
        Assert.notNull("reason", reason);
    }

    public static UnavailableBoardBuilder builder() {
        return new UnavailableBoardBuilder();
    }

    public static final class UnavailableBoardBuilder {
        private Project project;
        private Reason reason;

        private UnavailableBoardBuilder() {}

        public UnavailableBoardBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public UnavailableBoardBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableBoard build() {
            return new UnavailableBoard(project, reason);
        }
    }
}
