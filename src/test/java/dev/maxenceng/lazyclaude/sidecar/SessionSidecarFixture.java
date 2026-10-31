package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyInputPayload;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyOutputView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionExitView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;


public final class SessionSidecarFixture {

    private static final Duration OVERTAKING = Duration.ofMillis(200);
    private static final String SIZE = "{\"width\":100,\"height\":30}";

    private SessionSidecarFixture() {}

    public static String newHostedParams() {
        return "{\"root\":\"%s\",\"size\":%s}".formatted(project().root(), SIZE);
    }

    public static String ticketHostedParams() {
        return "{\"root\":\"%s\",\"ticket\":\"1\",\"branch\":\"%s\",\"verb\":\"REVIEW\",\"size\":%s}"
                .formatted(project().root(), branchName().value(), SIZE);
    }

    public static String resumeHostedParams() {
        return "{\"root\":\"%s\",\"session\":\"%s\",\"recordedDirectory\":\"%s\",\"size\":%s}"
                .formatted(project().root(), sessionId().value(), worktreeDirectory().path(), SIZE);
    }

    public static String resumeTicketHostedParams() {
        return "{\"session\":\"%s\",\"recordedDirectory\":\"%s\",\"root\":\"%s\",\"branch\":\"%s\",\"size\":%s}"
                .formatted(sessionId().value(), worktreeDirectory().path(), project().root(), branchName().value(), SIZE);
    }

    public static String sessionsParams() {
        return "{\"root\":\"%s\",\"ticketIds\":[\"1\"]}".formatted(project().root());
    }

    public static String startedHost() {
        return "{\"kind\":\"startedHost\",\"id\":\"%s\"}".formatted(sessionId().value());
    }

    public static String notStartedHost() {
        return "{\"kind\":\"notStartedHost\"}";
    }

    public static String noSessions() {
        return "{\"live\":[],\"past\":[],\"hosted\":[]}";
    }

    public static String sessionsChanged(String sessions) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"session/changed\",\"params\":%s}".formatted(sessions);
    }

    public static String ptyInputParams() {
        return "{\"session\":\"%s\",\"data\":\"G1tB\"}".formatted(sessionId().value());
    }

    public static String typedPtyInputParams() {
        return "{\"session\":\"%s\",\"data\":\"YQ==\"}".formatted(sessionId().value());
    }

    public static Answer<Object> appliedOnReturn(List<PtyInputPayload> applied) {
        CountDownLatch overtaken = new CountDownLatch(1);
        return call -> {
            PtyInputPayload input = call.getArgument(0);
            if (input.equals(ptyInputPayload(sessionId()))) {
                overtaken.await(OVERTAKING.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                overtaken.countDown();
            }
            applied.add(input);
            return null;
        };
    }

    public static String ptyResizeParams() {
        return "{\"session\":\"%s\",\"cols\":120,\"rows\":40}".formatted(sessionId().value());
    }

    public static String repaintParams() {
        return "{\"session\":\"%s\"}".formatted(sessionId().value());
    }

    public static String ptyOutput() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"pty/output\",\"params\":{\"session\":\"%s\",\"data\":\"G1tB\"}}"
                .formatted(sessionId().value());
    }

    public static String sessionExited(int code) {
        return sessionExited(sessionId().value(), code);
    }

    public static String sessionExited(String session, int code) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"session/exited\",\"params\":{\"session\":\"%s\",\"code\":%d}}"
                .formatted(session, code);
    }

    public static Answer<Object> outputtingAtOnce(PtyOutputView view) {
        return call -> {
            call.<Consumer<PtyOutputView>>getArgument(0).accept(view);
            return null;
        };
    }

    public static Answer<Object> exitingAtOnce(SessionExitView view) {
        return call -> {
            call.<Consumer<SessionExitView>>getArgument(0).accept(view);
            return null;
        };
    }

    public static Answer<Object> changingAtOnce(SessionsView view) {
        return call -> {
            call.<Consumer<SessionsView>>getArgument(1).accept(view);
            return null;
        };
    }
}
