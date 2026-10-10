package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Board;

/** A board as the shell reads it: one group per status in lifecycle order, then the unreadable files. */
public record BoardView(List<StatusGroupView> groups, List<UnreadableTicketView> unreadable) {

    static BoardView from(Board board) {
        return builder()
                .groups(board.groups().stream().map(StatusGroupView::from).toList())
                .unreadable(board.unreadable().stream().map(UnreadableTicketView::from).toList())
                .build();
    }

    public static BoardViewBuilder builder() {
        return new BoardViewBuilder();
    }

    public static final class BoardViewBuilder {
        private List<StatusGroupView> groups;
        private List<UnreadableTicketView> unreadable;

        private BoardViewBuilder() {}

        public BoardViewBuilder groups(List<StatusGroupView> groups) {
            this.groups = groups;
            return this;
        }

        public BoardViewBuilder unreadable(List<UnreadableTicketView> unreadable) {
            this.unreadable = unreadable;
            return this;
        }

        public BoardView build() {
            return new BoardView(groups, unreadable);
        }
    }
}
