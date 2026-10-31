package dev.maxenceng.lazyclaude.sidecar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;

/**
 * The backend a desktop frontend drives: JSON-RPC 2.0, one message per line, requests answered on the sidecar's own executor in any
 * order, matched by {@code id}. It ends when stdin does (ADR 0050).
 */
public final class Sidecar {

    private static final String REQUEST_NAME = "sidecar-request-";
    private static final int ENDED = 0;
    private static final int STDIN_FAILED = 1;
    private static final String UNKNOWN_METHOD = "unknown method: %s";
    private static final String FAILED = "%s failed";
    private static final String UNANSWERED = "notification %s not handled: %s";
    private static final String CANNOT_READ_STDIN = "cannot read stdin";
    private static final String INTERNAL_MESSAGE = "internal error";
    private static final System.Logger LOG = System.getLogger(Sidecar.class.getName());

    private final BoardQuery board;

    public Sidecar(BoardQuery board) {
        this.board = board;
    }

    /** The JSON Schema of every method and notification, as committed under {@code docs/protocol/}; the build fails when it is stale (ADR 0073). */
    public static String protocolSchema() {
        return ProtocolSchema.text(protocolMethods(), protocolNotifications());
    }

    /** Every method the sidecar declares: what the schema states, what the contract test counts and what {@link #methods} serves. */
    static List<ProtocolMethod<?, ?>> protocolMethods() {
        return BoardMethods.protocolMethods();
    }

    static List<ProtocolNotification<?>> protocolNotifications() {
        return BoardMethods.protocolNotifications();
    }

    Map<String, SidecarMethod> methods(SidecarOutput output) {
        return new BoardMethods(board, output).methods();
    }

    /** Writes {@code ready}, then answers every line of {@code in} until it closes; returns the exit code. */
    public int run(InputStream in, OutputStream out) {
        try (SidecarOutput output = new SidecarOutput(out);
                ExecutorService requests = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(REQUEST_NAME, 0).factory())) {
            Map<String, SidecarMethod> methods = methods(output);
            output.send(JsonRpcMessages.ready());
            return read(in, line -> requests.execute(() -> answer(line, methods, output)));
        }
    }

    private static int read(InputStream in, Consumer<String> received) {
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            lines.lines().filter(line -> !line.isBlank()).forEach(received);
            return ENDED;
        } catch (UncheckedIOException | IOException e) {
            LOG.log(System.Logger.Level.ERROR, CANNOT_READ_STDIN, e);
            return STDIN_FAILED;
        }
    }

    private static void answer(String line, Map<String, SidecarMethod> methods, SidecarOutput output) {
        try {
            switch (JsonRpcReader.read(line)) {
                case JsonRpcRequest request -> respondTo(request, methods, output);
                case JsonRpcNotification notification -> handle(notification, methods);
            }
        } catch (JsonRpcFaultException unreadable) {
            output.send(JsonRpcMessages.fault(JsonRpcMessages.unidentified(), unreadable.code(), unreadable.getMessage()));
        }
    }

    /** The result is queued before what the method does afterwards, which fails only into the log: the request has its answer. */
    private static void respondTo(JsonRpcRequest request, Map<String, SidecarMethod> methods, SidecarOutput output) {
        SidecarAnswer answer;
        try {
            answer = invoke(request, methods);
        } catch (JsonRpcFaultException fault) {
            output.send(JsonRpcMessages.fault(request.id(), fault.code(), fault.getMessage()));
            return;
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, FAILED.formatted(request.method()), failure);
            output.send(JsonRpcMessages.fault(request.id(), ErrorCode.INTERNAL_ERROR, shortMessage(failure)));
            return;
        }
        output.send(JsonRpcMessages.result(request.id(), answer.result()));
        runAfterwards(request, answer);
    }

    private static void handle(JsonRpcNotification notification, Map<String, SidecarMethod> methods) {
        try {
            runAfterwards(notification, invoke(notification, methods));
        } catch (JsonRpcFaultException fault) {
            LOG.log(System.Logger.Level.DEBUG, UNANSWERED.formatted(notification.method(), fault.getMessage()));
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, FAILED.formatted(notification.method()), failure);
        }
    }

    private static void runAfterwards(JsonRpcCall call, SidecarAnswer answer) {
        try {
            answer.afterwards().run();
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, FAILED.formatted(call.method()), failure);
        }
    }

    private static SidecarAnswer invoke(JsonRpcCall call, Map<String, SidecarMethod> methods) {
        return Optional.ofNullable(methods.get(call.method()))
                .orElseThrow(() -> new JsonRpcFaultException(ErrorCode.METHOD_NOT_FOUND, UNKNOWN_METHOD.formatted(call.method())))
                .call(call.params());
    }

    private static String shortMessage(RuntimeException failure) {
        return Optional.ofNullable(failure.getMessage()).filter(message -> !message.isBlank()).orElse(INTERNAL_MESSAGE);
    }
}
