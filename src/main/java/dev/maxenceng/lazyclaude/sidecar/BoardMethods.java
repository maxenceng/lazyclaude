package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardAfterChangePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardLookupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoProjectView;

import tools.jackson.databind.JsonNode;

/** The {@code board/} methods: one per operation of {@link BoardQuery}, named after it (ADR 0050). */
final class BoardMethods {

    private static final ServedMethod<BoardQuery, BoardPayload, BoardLookupView> BOARD_FOR =
            ServedMethod.of("board/boardFor", BoardPayload.class, BoardLookupView.class, BoardQuery::boardFor);
    private static final ProtocolMethod<BoardPayload, BoardLookupView> WATCH =
            new ProtocolMethod<>("board/watch", BoardPayload.class, BoardLookupView.class);
    private static final ProtocolNotification<BoardReadingView> CHANGED =
            new ProtocolNotification<>("board/changed", BoardReadingView.class);

    private final BoardQuery board;
    private final SidecarOutput output;
    private final Map<Path, BoardReadingView> shown = new ConcurrentHashMap<>();

    BoardMethods(BoardQuery board, SidecarOutput output) {
        this.board = board;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(BOARD_FOR.method(), WATCH), List.of(), List.of(CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.ofEntries(BOARD_FOR.boundTo(board), Map.entry(WATCH.name(), this::watch));
    }

    /** The watch opens once the current board is queued, so no {@code board/changed} reaches the frontend before the board it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a ticket edited as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        BoardLookupView current = WATCH.answer(params, board::boardFor);
        JsonNode result = SidecarJson.tree(current);
        return switch (current) {
            case BoardReadingView read -> new SidecarAnswer(result, () -> watchOnce(read));
            case NoProjectView _ -> SidecarAnswer.of(result);
        };
    }

    /**
     * The board the frontend was last sent is what the next change is read against. A watch that failed to open is forgotten before
     * another {@code board/watch} of the project is let in, so that one tries again.
     */
    private synchronized void watchOnce(BoardReadingView current) {
        Path root = current.root().value();
        if (shown.put(root, current) == null) {
            try {
                board.subscribe(new BoardWatchPayload(root), () -> pushBoardOf(root));
            } catch (RuntimeException failure) {
                shown.remove(root);
                throw failure;
            }
        }
    }

    private void pushBoardOf(Path root) {
        board.boardAfterChange(new BoardAfterChangePayload(root, shown.get(root) instanceof BoardView)).ifPresent(view -> {
            shown.put(root, view);
            output.send(CHANGED.message(view));
        });
    }
}
