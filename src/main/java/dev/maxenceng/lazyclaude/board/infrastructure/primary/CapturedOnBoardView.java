package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.published.TicketId;

/** GitHub took the ticket and numbered it, and the board as read once it had. */
public record CapturedOnBoardView(TicketId id, BoardAfterCaptureView board) implements BoardCaptureView {

    public static CapturedOnBoardViewBuilder builder() {
        return new CapturedOnBoardViewBuilder();
    }

    public static final class CapturedOnBoardViewBuilder {
        private TicketId id;
        private BoardAfterCaptureView board;

        private CapturedOnBoardViewBuilder() {}

        public CapturedOnBoardViewBuilder id(TicketId id) {
            this.id = id;
            return this;
        }

        public CapturedOnBoardViewBuilder board(BoardAfterCaptureView board) {
            this.board = board;
            return this;
        }

        public CapturedOnBoardView build() {
            return new CapturedOnBoardView(id, board);
        }
    }
}
