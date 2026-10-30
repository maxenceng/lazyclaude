package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.published.ProjectRoot;

/** A board read: one group per status in lifecycle order, then the unreadable tickets, and the project root it was read from. */
public record BoardView(List<StatusGroupView> groups, List<UnreadableTicketView> unreadable, ProjectRoot root) implements BoardReadingView {

    static BoardView from(Board board) {
        return builder()
                .groups(board.groups().stream().map(StatusGroupView::from).toList())
                .unreadable(board.unreadable().stream().map(UnreadableTicketView::from).toList())
                .root(new ProjectRoot(board.project().root()))
                .build();
    }

    public static BoardViewBuilder builder() {
        return new BoardViewBuilder();
    }

    public static final class BoardViewBuilder {
        private List<StatusGroupView> groups;
        private List<UnreadableTicketView> unreadable;
        private ProjectRoot root;

        private BoardViewBuilder() {}

        public BoardViewBuilder groups(List<StatusGroupView> groups) {
            this.groups = groups;
            return this;
        }

        public BoardViewBuilder unreadable(List<UnreadableTicketView> unreadable) {
            this.unreadable = unreadable;
            return this;
        }

        public BoardViewBuilder root(ProjectRoot root) {
            this.root = root;
            return this;
        }

        public BoardView build() {
            return new BoardView(groups, unreadable, root);
        }
    }
}
