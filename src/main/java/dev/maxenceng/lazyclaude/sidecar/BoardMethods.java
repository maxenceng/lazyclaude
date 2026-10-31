package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
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

    private static final String BOARD_FOR = "board/boardFor";
    private static final String WATCH = "board/watch";
    private static final String CHANGED = "board/changed";

    private final BoardQuery board;
    private final SidecarOutput output;
    private final Map<Path, BoardReadingView> shown = new ConcurrentHashMap<>();

    BoardMethods(BoardQuery board, SidecarOutput output) {
        this.board = board;
        this.output = output;
    }

    Map<String, SidecarMethod> methods() {
        return Map.of(BOARD_FOR, this::boardFor, WATCH, this::watch);
    }

    private SidecarAnswer boardFor(JsonNode params) {
        return SidecarAnswer.of(SidecarJson.tree(board.boardFor(SidecarJson.params(params, BoardPayload.class))));
    }

    /** The watch opens once the current board is queued, so no {@code board/changed} reaches the frontend before the board it changes. */
    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a ticket edited as the frontend connects is seen stale.
    private SidecarAnswer watch(JsonNode params) {
        BoardLookupView current = board.boardFor(SidecarJson.params(params, BoardPayload.class));
        JsonNode result = SidecarJson.tree(current);
        return switch (current) {
            case BoardReadingView read -> new SidecarAnswer(result, () -> watchOnce(read));
            case NoProjectView _ -> SidecarAnswer.of(result);
        };
    }

    /**
     * The board the frontend was last sent is what the next change is read against. A watch that failed to open is forgotten, so the
     * next {@code board/watch} of the project tries again.
     */
    private void watchOnce(BoardReadingView current) {
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
            output.send(JsonRpcMessages.notification(CHANGED, SidecarJson.tree(view)));
        });
    }
}
