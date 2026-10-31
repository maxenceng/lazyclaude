package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;

import tools.jackson.databind.JsonNode;

/** The {@code board/} methods: one per operation of {@link BoardQuery}, named after it (ADR 0050). */
final class BoardMethods {

    private static final String BOARD_FOR = "board/boardFor";
    private static final String WATCH = "board/watch";
    private static final String CHANGED = "board/changed";

    private final BoardQuery board;
    private final SidecarOutput output;
    private final Set<Path> watched = ConcurrentHashMap.newKeySet();

    BoardMethods(BoardQuery board, SidecarOutput output) {
        this.board = board;
        this.output = output;
    }

    Map<String, SidecarMethod> methods() {
        return Map.of(BOARD_FOR, this::boardFor, WATCH, this::watch);
    }

    private JsonNode boardFor(JsonNode params) {
        return SidecarJson.tree(board.boardFor(SidecarJson.params(params, BoardPayload.class)));
    }

    // deferred: a change between this read and the watch starting is pushed only at the next change — close it when a ticket edited as the frontend connects is seen stale.
    private JsonNode watch(JsonNode params) {
        BoardReadingView current = board.boardFor(SidecarJson.params(params, BoardPayload.class));
        Path root = current.root().value();
        if (watched.add(root)) {
            board.subscribe(new BoardWatchPayload(root), () -> pushBoardOf(root));
        }
        return SidecarJson.tree(current);
    }

    private void pushBoardOf(Path root) {
        board.boardReadAgainFor(new BoardPayload(root))
                .ifPresent(view -> output.send(JsonRpcMessages.notification(CHANGED, SidecarJson.tree(view))));
    }
}
