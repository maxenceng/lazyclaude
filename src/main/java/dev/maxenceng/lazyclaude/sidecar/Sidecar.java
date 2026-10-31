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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.Contexts;


/**
 * The backend a desktop frontend drives: JSON-RPC 2.0, one message per line, requests answered on the sidecar's own executor in any
 * order, matched by {@code id}, and inputs taken in the order they arrive. It ends when stdin does (ADR 0050).
 */
public final class Sidecar {

    private static final String REQUEST_NAME = "sidecar-request-";
    private static final String INPUT_NAME = "sidecar-input";
    private static final Set<String> INPUT_NAMES = protocol().inputNames();
    private static final int ENDED = 0;
    private static final int STDIN_FAILED = 1;
    private static final String UNKNOWN_METHOD = "unknown method: %s";
    private static final String FAILED = "%s failed";
    private static final String UNANSWERED = "notification %s not handled: %s";
    private static final String CANNOT_READ_STDIN = "cannot read stdin";
    private static final String INTERNAL_MESSAGE = "internal error";
    private static final System.Logger LOG = System.getLogger(Sidecar.class.getName());

    private final Contexts contexts;

    public Sidecar(Contexts contexts) {
        this.contexts = contexts;
    }

    /** The JSON Schema of every method and notification, as committed under {@code docs/protocol/}; the build fails when it is stale (ADR 0073). */
    public static String protocolSchema() {
        return ProtocolSchema.text(protocol());
    }

    /** Everything the sidecar declares: what the schema states, what the contract test counts and what {@link #methods} serves. */
    static Protocol protocol() {
        return Protocol.of(BoardMethods.protocol(), SessionMethods.protocol(), WarningMethods.protocol(), DocsMethods.protocol(), ReviewMethods.protocol());
    }

    /** What is served on {@code output}, with the pushes of what a hosted {@code claude} writes and how it exits wired to it. */
    Map<String, SidecarMethod> methods(SidecarOutput output) {
        return Stream.of(
                        new BoardMethods(contexts.board(), output).methods(),
                        new SessionMethods(contexts.sessions(), contexts.hosting(), output).methods(),
                        new WarningMethods(contexts.warnings(), output).methods(),
                        new DocsMethods(contexts.docs(), output).methods(),
                        new ReviewMethods(contexts.review(), contexts.pendingReview(), output).methods())
                .flatMap(served -> served.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Writes {@code ready}, then answers every line of {@code in} until it closes, ends every hosted {@code claude} once the requests
     * still running are answered, and returns the exit code (ADR 0048).
     */
    public int run(InputStream in, OutputStream out) {
        try (SidecarOutput output = new SidecarOutput(out)) {
            Map<String, SidecarMethod> methods = methods(output);
            output.send(JsonRpcMessages.ready());
            int ended = answering(in, methods, output);
            contexts.hosting().endAll();
            return ended;
        }
    }

    /** Inputs are taken one at a time in the order they arrive, as keystrokes and resizes must be; requests run concurrently. */
    private static int answering(InputStream in, Map<String, SidecarMethod> methods, SidecarOutput output) {
        try (ExecutorService requests = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(REQUEST_NAME, 0).factory());
                ExecutorService inputs = Executors.newSingleThreadExecutor(Thread.ofVirtual().name(INPUT_NAME).factory())) {
            return read(in, line -> received(line, output, call -> (INPUT_NAMES.contains(call.method()) ? inputs : requests)
                    .execute(() -> answer(call, methods, output))));
        }
    }

    private static void received(String line, SidecarOutput output, Consumer<JsonRpcCall> dispatched) {
        try {
            dispatched.accept(JsonRpcReader.read(line));
        } catch (JsonRpcFaultException unreadable) {
            output.send(JsonRpcMessages.fault(JsonRpcMessages.unidentified(), unreadable.code(), unreadable.getMessage()));
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

    private static void answer(JsonRpcCall call, Map<String, SidecarMethod> methods, SidecarOutput output) {
        switch (call) {
            case JsonRpcRequest request -> respondTo(request, methods, output);
            case JsonRpcNotification notification -> handle(notification, methods);
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
