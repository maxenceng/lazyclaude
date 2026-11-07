package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardAfterChangePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardLookupView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardWatchPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.CapturedTicketView;
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
    private static final ProtocolMethod<TicketCapturePayload, TicketCaptureView> CAPTURE =
            new ProtocolMethod<>("board/capture", TicketCapturePayload.class, TicketCaptureView.class);
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
     * A captured ticket is followed, once its answer is queued, by the board read at once: the frontend selects the ticket on the first
     * board it receives after the answer, so one must follow it even when the read fails, and a board before it would be taken for it.
     */
    private SidecarAnswer capture(JsonNode params) {
        TicketCaptureView answer = CAPTURE.answer(params, this::captureOrRefuse);
        JsonNode result = SidecarJson.tree(answer);
        return switch (answer) {
            case CapturedTicketView _ -> new SidecarAnswer(result, () -> pushBoardAfterCapture(SidecarJson.params(params, CAPTURE.params()).root()));
            case RefusedTicketCaptureView _ -> SidecarAnswer.of(result);
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

    /** Synchronized with the push after a capture, so each board kept in {@code shown} is the one sent last. */
    private synchronized void pushBoardOf(Path root) {
        boardRead(root).ifPresent(view -> {
            shown.put(root, view);
            output.send(CHANGED.message(view));
        });
    }

    /**
     * The board is read again, and the board last sent is pushed again when that read fails. A project nobody watches keeps no board,
     * so a failed read there has nothing to push; the watch it would open is the frontend's to ask for.
     */
    // deferred: a capture on a project with no watch and a failing read pushes nothing — close it when a frontend captures before it watches.
    private synchronized void pushBoardAfterCapture(Path root) {
        boardRead(root).or(() -> Optional.ofNullable(shown.get(root))).ifPresent(view -> {
            shown.computeIfPresent(root, (_, _) -> view);
            output.send(CHANGED.message(view));
        });
    }

    private Optional<BoardReadingView> boardRead(Path root) {
        return board.boardAfterChange(new BoardAfterChangePayload(root, shown.get(root) instanceof BoardView));
    }
}
