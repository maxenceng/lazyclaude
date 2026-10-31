package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.ContextsFixture.sidecarContexts;
import static org.mockito.Mockito.mock;

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

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardAfterChangePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.NoProjectView;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;

public final class SidecarFixture {

    private static final String NEWLINE = "\n";
    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration OVERLAP = Duration.ofMillis(200);

    private SidecarFixture() {}

    public static String ready() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"ready\",\"params\":{\"protocol\":1}}";
    }

    public static List<String> linesWritten(BoardQuery board, String... received) {
        return linesWritten(sidecarOver(board), received);
    }

    public static List<String> linesWritten(SessionQuery sessions, HostedSessionCommand hosting, String... received) {
        return linesWritten(sidecarOver(sessions, hosting), received);
    }

    public static List<String> linesWritten(WarningQuery warnings, String... received) {
        return linesWritten(sidecarOver(warnings), received);
    }

    public static List<String> linesWritten(DocsQuery docs, String... received) {
        return linesWritten(sidecarOver(docs), received);
    }

    public static List<String> linesWritten(ReviewQuery review, String... received) {
        return linesWritten(sidecarOver(review), received);
    }

    public static List<String> linesWritten(PendingReviewCommand pendingReview, String... received) {
        return linesWritten(sidecarOver(pendingReview), received);
    }

    public static int exitCodeOf(BoardQuery board, String... received) {
        return sidecarOver(board).run(stdin(received), new ByteArrayOutputStream());
    }

    private static List<String> linesWritten(Sidecar sidecar, String... received) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sidecar.run(stdin(received), out);
        return out.toString(StandardCharsets.UTF_8).lines().toList();
    }

    private static Sidecar sidecarOver(BoardQuery board) {
        return new Sidecar(sidecarContexts(
                board,
                mock(SessionQuery.class),
                mock(HostedSessionCommand.class),
                mock(WarningQuery.class),
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class)));
    }

    private static Sidecar sidecarOver(SessionQuery sessions, HostedSessionCommand hosting) {
        return new Sidecar(sidecarContexts(
                mock(BoardQuery.class),
                sessions,
                hosting,
                mock(WarningQuery.class),
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class)));
    }

    private static Sidecar sidecarOver(WarningQuery warnings) {
        return new Sidecar(sidecarContexts(
                mock(BoardQuery.class),
                mock(SessionQuery.class),
                mock(HostedSessionCommand.class),
                warnings,
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class)));
    }

    private static Sidecar sidecarOver(DocsQuery docs) {
        return new Sidecar(sidecarContexts(
                mock(BoardQuery.class),
                mock(SessionQuery.class),
                mock(HostedSessionCommand.class),
                mock(WarningQuery.class),
                docs,
                mock(ReviewQuery.class),
                mock(PendingReviewCommand.class)));
    }

    private static Sidecar sidecarOver(ReviewQuery review) {
        return new Sidecar(sidecarContexts(
                mock(BoardQuery.class),
                mock(SessionQuery.class),
                mock(HostedSessionCommand.class),
                mock(WarningQuery.class),
                mock(DocsQuery.class),
                review,
                mock(PendingReviewCommand.class)));
    }

    private static Sidecar sidecarOver(PendingReviewCommand pendingReview) {
        return new Sidecar(sidecarContexts(
                mock(BoardQuery.class),
                mock(SessionQuery.class),
                mock(HostedSessionCommand.class),
                mock(WarningQuery.class),
                mock(DocsQuery.class),
                mock(ReviewQuery.class),
                pendingReview));
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

    public static BoardAfterChangePayload afterChangeOverABoard() {
        return new BoardAfterChangePayload(projectPath(), true);
    }

    public static BoardAfterChangePayload afterChangeOverAReason() {
        return new BoardAfterChangePayload(projectPath(), false);
    }

    public static BoardPayload otherPayload() {
        return new BoardPayload(otherProjectPath());
    }

    public static String request(int id, String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}".formatted(id, method, params);
    }

    public static Duration observedRepaintGap() {
        return Duration.ofMillis(30);
    }

    public static String notification(String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":%s}".formatted(method, params);
    }

    public static String requestWithoutParams(int id, String method) {
        return "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\"}".formatted(id, method);
    }

    public static String callOfAnotherVersion() {
        return "{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"board/boardFor\",\"params\":{\"from\":\"/work/app\"}}";
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

    public static String invalidParamsCode() {
        return "\"code\":-32602";
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

    public static String unreachableBoard() {
        return "{\"kind\":\"unavailableBoard\",\"root\":\"/work/app\","
                + "\"reason\":\"cannot read the issues from GitHub: error connecting to api.github.com\"}";
    }

    public static NoProjectView noProjectView() {
        return new NoProjectView(new Reason("/work/app is not inside a git repository"));
    }

    public static String noProject() {
        return noProject("/work/app is not inside a git repository");
    }

    public static String noProject(String reason) {
        return "{\"kind\":\"noProject\",\"reason\":\"%s\"}".formatted(reason);
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

    public static Answer<Object> signalTwiceAtOnce() {
        return call -> {
            call.<Runnable>getArgument(1).run();
            call.<Runnable>getArgument(1).run();
            return null;
        };
    }

    public static Answer<Object> failingAfterReleasing(CountDownLatch released) {
        return _ -> {
            released.countDown();
            Thread.sleep(OVERLAP);
            throw new IllegalStateException("the watch could not start");
        };
    }

    public static <V> Answer<V> waitingFor(CountDownLatch released, V answer) {
        return _ -> {
            if (!released.await(PATIENCE.toSeconds(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("the other request was never answered");
            }
            Thread.sleep(OVERLAP);
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
