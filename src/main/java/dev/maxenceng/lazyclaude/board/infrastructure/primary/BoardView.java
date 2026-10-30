package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardReading;
import dev.maxenceng.lazyclaude.board.domain.UnavailableBoard;
import dev.maxenceng.lazyclaude.board.published.ProjectRoot;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;

/**
 * A board as the shell reads it: one group per status in lifecycle order, then the unreadable issues, and the project root it was read
 * from. When the issues could not be read there are no tickets, and {@code unavailable} says why.
 */
public record BoardView(
        List<StatusGroupView> groups, List<UnreadableTicketView> unreadable, ProjectRoot root, Optional<Reason> unavailable) {

    static BoardView from(BoardReading reading) {
        return switch (reading) {
            case Board board -> ofBoard(board);
            case UnavailableBoard unavailable -> unavailable(unavailable);
        };
    }

    private static BoardView unavailable(UnavailableBoard board) {
        return builder()
                .groups(Arrays.stream(Status.values())
                        .map(status -> StatusGroupView.builder().status(status).tickets(List.of()).build())
                        .toList())
                .unreadable(List.of())
                .root(new ProjectRoot(board.project().root()))
                .unavailable(Optional.of(board.reason()))
                .build();
    }

    private static BoardView ofBoard(Board board) {
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
        private Optional<Reason> unavailable = Optional.empty();

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

        public BoardViewBuilder unavailable(Optional<Reason> unavailable) {
            this.unavailable = unavailable;
            return this;
        }

        public BoardView build() {
            return new BoardView(groups, unreadable, root, unavailable);
        }
    }
}
