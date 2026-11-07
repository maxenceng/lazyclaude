package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardAfterCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardAfterChangePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardLookupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.CapturedOnBoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.CapturedTicketView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoBoardAfterCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoProjectView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.RefusedTicketCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCapturePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureView;

import tools.jackson.databind.JsonNode;

/** The {@code board/} methods: one per operation of {@link BoardQuery} and {@link TicketCaptureCommand}, named after it (ADR 0050). */
final class BoardMethods {

    private static final ServedMethod<BoardQuery, BoardPayload, BoardLookupView> BOARD_FOR =
            ServedMethod.of("board/boardFor", BoardPayload.class, BoardLookupView.class, BoardQuery::boardFor);
    private static final ProtocolMethod<BoardPayload, BoardLookupView> WATCH =
            new ProtocolMethod<>("board/watch", BoardPayload.class, BoardLookupView.class);
    private static final ProtocolMethod<TicketCapturePayload, BoardCaptureView> CAPTURE =
            new ProtocolMethod<>("board/capture", TicketCapturePayload.class, BoardCaptureView.class);
    private static final ProtocolNotification<BoardReadingView> CHANGED =
            new ProtocolNotification<>("board/changed", BoardReadingView.class);

    private final BoardQuery board;
    private final TicketCaptureCommand capturing;
    private final SidecarOutput output;
    private final Map<Path, BoardReadingView> shown = new ConcurrentHashMap<>();

    BoardMethods(BoardQuery board, TicketCaptureCommand capturing, SidecarOutput output) {
        this.board = board;
        this.capturing = capturing;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(BOARD_FOR.method(), WATCH, CAPTURE), List.of(), List.of(CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.ofEntries(BOARD_FOR.boundTo(board), Map.entry(WATCH.name(), this::watch), Map.entry(CAPTURE.name(), this::capture));
    }

    /**
     * A captured ticket is answered with the board read once GitHub took it, so the frontend selects it on that board: a watch read that
     * began before the create is sent before this answer, under the same lock, and can no longer be taken for the capture's board.
     */
    private SidecarAnswer capture(JsonNode params) {
        return SidecarAnswer.of(SidecarJson.tree(CAPTURE.answer(params, this::captureOnBoard)));
    }

    private BoardCaptureView captureOnBoard(TicketCapturePayload payload) {
        TicketCaptureView capture = captureOrRefuse(payload);
        return switch (capture) {
            case CapturedTicketView captured -> CapturedOnBoardView.builder()
                    .id(captured.id())
                    .board(boardAfterCapture(payload.root()))
                    .build();
            case RefusedTicketCaptureView refused -> refused;
        };
    }

    private TicketCaptureView captureOrRefuse(TicketCapturePayload payload) {
        try {
            return capturing.capture(payload);
        } catch (RuntimeException failed) {
            return TicketCaptureCommand.refusedBecause(failed);
        }
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

    /** Synchronized with the read after a capture, so each board kept in {@code shown} is the one sent last. */
    private synchronized void pushBoardOf(Path root) {
        boardRead(root).ifPresent(view -> {
            shown.put(root, view);
            output.send(CHANGED.message(view));
        });
    }

    /** A project nobody watches keeps no board, so a read there is not kept as the board last sent; the watch is the frontend's to ask for. */
    private synchronized BoardAfterCaptureView boardAfterCapture(Path root) {
        Optional<BoardReadingView> read = boardRead(root);
        read.ifPresent(view -> shown.computeIfPresent(root, (_, _) -> view));
        return read.<BoardAfterCaptureView>map(view -> view).orElseGet(NoBoardAfterCaptureView::new);
    }

    private Optional<BoardReadingView> boardRead(Path root) {
        return board.boardAfterChange(new BoardAfterChangePayload(root, shown.get(root) instanceof BoardView));
    }
}
