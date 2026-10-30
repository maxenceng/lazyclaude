package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.UnavailableBoard;
import dev.maxenceng.lazyclaude.board.published.ProjectRoot;
import dev.maxenceng.lazyclaude.board.published.Reason;

/** A board whose issues could not be read: no tickets, and why. */
public record UnavailableBoardView(ProjectRoot root, Reason reason) implements BoardReadingView {

    static UnavailableBoardView from(UnavailableBoard board) {
        return builder().root(new ProjectRoot(board.project().root())).reason(board.reason()).build();
    }

    public static UnavailableBoardViewBuilder builder() {
        return new UnavailableBoardViewBuilder();
    }

    public static final class UnavailableBoardViewBuilder {
        private ProjectRoot root;
        private Reason reason;

        private UnavailableBoardViewBuilder() {}

        public UnavailableBoardViewBuilder root(ProjectRoot root) {
            this.root = root;
            return this;
        }

        public UnavailableBoardViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableBoardView build() {
            return new UnavailableBoardView(root, reason);
        }
    }
}
