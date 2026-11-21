package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.ContextsFixture.sidecarContexts;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.mergedAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.ReviewSidecarFixture.reviewParams;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture;

public final class WorktreeSidecarFixture {

    private WorktreeSidecarFixture() {}

    static Map<String, SidecarMethod> served(SessionQuery sessions, TicketWorktreeCommand removing, ReviewQuery review) {
        return servedTo(sessions, removing, review, new SidecarOutput(new ByteArrayOutputStream()));
    }

    static Map<String, SidecarMethod> servedTo(
            SessionQuery sessions, TicketWorktreeCommand removing, ReviewQuery review, SidecarOutput output) {
        return new Sidecar(sidecarContexts(sessions, removing, review)).methods(output);
    }

    static void watchingTheProject(Map<String, SidecarMethod> methods, SessionQuery sessions) {
        when(sessions.sessionsFor(any())).thenReturn(emptySessionsView());
        methods.get("session/watch").call(SidecarJson.parse(sessionsWatched()));
    }

    static void havingReadMerged(Map<String, SidecarMethod> methods, ReviewQuery review, String... heads) {
        when(review.checking()).thenReturn(checkingPullRequestsView());
        doAnswer(mergedAtOnce(mergedPullRequestsViewOn(heads))).when(review).subscribe(any(), any(), any());
        methods.get("review/watch").call(SidecarJson.parse(reviewParams())).afterwards().run();
    }

    static Answer<Object> endingTheDoneSessionOf(LinkedTicketId ticket) {
        return call -> {
            call.<Consumer<LinkedTicketId>>getArgument(0).accept(ticket);
            return null;
        };
    }

    static String answerTo(Map<String, SidecarMethod> methods, String method) {
        return SidecarJson.line(methods.get(method).call(SidecarJson.parse(branchParams())).result());
    }

    public static String sessionsWatched() {
        return "{\"root\":\"/work/app\",\"ticketIds\":[\"%s\"]}".formatted(LinkedTicketIdFixture.linkedTicketId().value());
    }

    public static String branchParams() {
        return "{\"branch\":\"PROJ-1-ticket\"}";
    }

    public static String clearWorktree() {
        return "{\"kind\":\"clearWorktree\",\"shown\":\"../app.worktrees/PROJ-1-ticket\"}";
    }

    public static String ticketDoneEnded() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"session/ticketDoneEnded\",\"params\":\"1\"}";
    }
}
