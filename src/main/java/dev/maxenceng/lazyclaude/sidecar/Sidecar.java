package dev.maxenceng.lazyclaude.sidecar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;

import tools.jackson.databind.JsonNode;

/**
 * The backend a desktop frontend drives: JSON-RPC 2.0, one message per line, requests answered on the sidecar's own executor in any
 * order, matched by {@code id}. It ends when stdin does (ADR 0050).
 */
public final class Sidecar {

    private static final String REQUEST_NAME = "sidecar-request-";
    private static final int ENDED = 0;
    private static final int STDIN_FAILED = 1;
    private static final String INTERNAL_MESSAGE = "internal error";
    private static final System.Logger LOG = System.getLogger(Sidecar.class.getName());

    private final BoardQuery board;

    public Sidecar(BoardQuery board) {
        this.board = board;
    }

    /** Writes {@code ready}, then answers every line of {@code in} until it closes; returns the exit code. */
    public int run(InputStream in, OutputStream out) {
        try (SidecarOutput output = new SidecarOutput(out);
                ExecutorService requests = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(REQUEST_NAME, 0).factory())) {
            Map<String, SidecarMethod> methods = new BoardMethods(board, output).methods();
            output.send(JsonRpcMessages.ready());
            return read(in, line -> requests.execute(() -> answer(line, methods, output)));
        }
    }

    private static int read(InputStream in, Consumer<String> received) {
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            lines.lines().filter(line -> !line.isBlank()).forEach(received);
            return ENDED;
        } catch (UncheckedIOException | IOException e) {
            LOG.log(System.Logger.Level.ERROR, "cannot read stdin", e);
            return STDIN_FAILED;
        }
    }

    private static void answer(String line, Map<String, SidecarMethod> methods, SidecarOutput output) {
        try {
            switch (JsonRpcReader.read(line)) {
                case JsonRpcRequest request -> output.send(respondTo(request, methods));
                case JsonRpcNotification notification -> handle(notification, methods);
            }
        } catch (JsonRpcFaultException unreadable) {
            output.send(JsonRpcMessages.fault(JsonRpcMessages.unidentified(), unreadable.code(), unreadable.getMessage()));
        }
    }

    private static JsonNode respondTo(JsonRpcRequest request, Map<String, SidecarMethod> methods) {
        try {
            return JsonRpcMessages.result(request.id(), invoke(request, methods));
        } catch (JsonRpcFaultException fault) {
            return JsonRpcMessages.fault(request.id(), fault.code(), fault.getMessage());
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, "%s failed".formatted(request.method()), failure);
            return JsonRpcMessages.fault(request.id(), ErrorCode.INTERNAL_ERROR, shortMessage(failure));
        }
    }

    private static void handle(JsonRpcNotification notification, Map<String, SidecarMethod> methods) {
        try {
            invoke(notification, methods);
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, "%s failed".formatted(notification.method()), failure);
        }
    }

    private static JsonNode invoke(JsonRpcCall call, Map<String, SidecarMethod> methods) {
        return Optional.ofNullable(methods.get(call.method()))
                .orElseThrow(() -> new JsonRpcFaultException(ErrorCode.METHOD_NOT_FOUND, "unknown method: " + call.method()))
                .call(call.params());
    }

    private static String shortMessage(RuntimeException failure) {
        return Optional.ofNullable(failure.getMessage()).filter(message -> !message.isBlank()).orElse(INTERNAL_MESSAGE);
    }
}
