package dev.maxenceng.lazyclaude.sidecar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;

public final class SidecarFixture {

    private static final String NEWLINE = "\n";
    private static final Duration PATIENCE = Duration.ofSeconds(10);

    private SidecarFixture() {}

    public static String ready() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"ready\",\"params\":{\"protocol\":1}}";
    }

    public static List<String> linesWritten(BoardQuery board, String... received) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new Sidecar(board).run(stdin(received), out);
        return out.toString(StandardCharsets.UTF_8).lines().toList();
    }

    public static int exitCodeOf(BoardQuery board, String... received) {
        return new Sidecar(board).run(stdin(received), new ByteArrayOutputStream());
    }

    public static Path projectPath() {
        return Path.of("/work/app");
    }

    public static Path otherProjectPath() {
        return Path.of("/work/other");
    }

    public static BoardPayload payload() {
        return new BoardPayload(projectPath());
    }

    public static BoardPayload otherPayload() {
        return new BoardPayload(otherProjectPath());
    }

    public static String request(int id, String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}".formatted(id, method, params);
    }

    public static String boardForProject(int id) {
        return request(id, "board/boardFor", "{\"from\":\"/work/app\"}");
    }

    public static String boardForOtherProject(int id) {
        return request(id, "board/boardFor", "{\"from\":\"/work/other\"}");
    }

    public static String watchProject(int id) {
        return request(id, "board/watch", "{\"from\":\"/work/app\"}");
    }

    public static String notificationWithoutId() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"no/such\"}";
    }

    public static String unparseableLine() {
        return "{\"jsonrpc\":";
    }

    public static String notACall() {
        return "[1,2]";
    }

    public static String errorResponse(String id, int code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"error\":{\"code\":%d,\"message\":\"%s\"}}".formatted(id, code, message);
    }

    public static String faultWithCode(String id, int code) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%s,\"error\":{\"code\":%d,".formatted(id, code);
    }

    public static String ticketlessBoard() {
        String groups = Arrays.stream(new String[] {"DRAFT", "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
                .map(status -> "{\"status\":\"%s\",\"tickets\":[]}".formatted(status))
                .collect(Collectors.joining(","));
        return "{\"kind\":\"board\",\"groups\":[%s],\"unreadable\":[],\"root\":\"/work/app\"}".formatted(groups);
    }

    public static String ticketBoard() {
        return ticketlessBoard()
                .replace(
                        "{\"status\":\"DRAFT\",\"tickets\":[]}",
                        "{\"status\":\"DRAFT\",\"tickets\":[{\"id\":\"1\",\"branch\":\"PROJ-1\",\"title\":\"A board from the backlog\",\"status\":\"DRAFT\","
                                + "\"context\":{\"kind\":\"contextName\",\"value\":\"board\"},\"type\":{\"kind\":\"typeName\",\"value\":\"feature\"},"
                                + "\"otherLabels\":[],\"body\":{\"kind\":\"body\",\"value\":\"\\n# A board from the backlog\\n\\nBody.\"},\"verbs\":[\"REFINE\"]}]}");
    }

    public static String unavailableBoard() {
        return "{\"kind\":\"unavailableBoard\",\"root\":\"/work/app\",\"reason\":\"gh is not logged in; run gh auth login\"}";
    }

    public static String resultOf(int id, String result) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%d,\"result\":%s}".formatted(id, result);
    }

    public static String changed(String board) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"board/changed\",\"params\":%s}".formatted(board);
    }

    private static ByteArrayInputStream stdin(String... received) {
        String text = Arrays.stream(received).collect(Collectors.joining(NEWLINE, "", NEWLINE));
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    public static Answer<Object> signalAtOnce() {
        return call -> {
            call.<Runnable>getArgument(1).run();
            return null;
        };
    }

    public static Answer<BoardReadingView> waitingFor(CountDownLatch released, BoardReadingView answer) {
        return _ -> {
            if (!released.await(PATIENCE.toSeconds(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("the other request was never answered");
            }
            return answer;
        };
    }

    public static Answer<BoardReadingView> releasing(CountDownLatch released, BoardReadingView answer) {
        return _ -> {
            released.countDown();
            return answer;
        };
    }
}
