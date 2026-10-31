package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardLookupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
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
    private final Set<Path> watched = ConcurrentHashMap.newKeySet();

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
            case BoardReadingView read -> new SidecarAnswer(result, () -> watchOnce(read.root().value()));
            case NoProjectView _ -> SidecarAnswer.of(result);
        };
    }

    /** A watch that failed to open is forgotten, so the next {@code board/watch} of the project tries again. */
    private void watchOnce(Path root) {
        if (watched.add(root)) {
            try {
                board.subscribe(new BoardWatchPayload(root), () -> pushBoardOf(root));
            } catch (RuntimeException failure) {
                watched.remove(root);
                throw failure;
            }
        }
    }

    // deferred: an unavailable board is never pushed again, so its first reason stays — close it when the shell's read-after-a-change rule moves into board.
    private void pushBoardOf(Path root) {
        board.boardReadAgainFor(new BoardPayload(root))
                .ifPresent(view -> output.send(JsonRpcMessages.notification(CHANGED, SidecarJson.tree(view))));
    }
}
