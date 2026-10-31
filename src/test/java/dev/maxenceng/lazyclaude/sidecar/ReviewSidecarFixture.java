package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.otherProjectPath;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.projectPath;

import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;

public final class ReviewSidecarFixture {

    private ReviewSidecarFixture() {}

    public static String reviewParams() {
        return "{\"root\":\"%s\"}".formatted(projectPath());
    }

    public static String otherReviewParams() {
        return "{\"root\":\"%s\"}".formatted(otherProjectPath());
    }

    public static String pullRequestParams() {
        return "{\"root\":\"%s\",\"number\":23}".formatted(projectPath());
    }

    public static String loadingDiffAndThreadsParams() {
        return "{\"diff\":{\"kind\":\"loadingDiff\"},\"threads\":{\"kind\":\"loadingThreads\"}}";
    }

    public static String loadingDiffAndPendingParams() {
        return "{\"diff\":{\"kind\":\"loadingDiff\"},\"pending\":{\"kind\":\"loadingPendingReview\"}}";
    }

    public static String anchorParams() {
        return "{\"diff\":{\"kind\":\"loadingDiff\"},\"first\":2,\"last\":3}";
    }

    public static String loadingDiff() {
        return "{\"kind\":\"loadingDiff\"}";
    }

    public static String loadingThreads() {
        return "{\"kind\":\"loadingThreads\"}";
    }

    public static String loadingPending() {
        return "{\"kind\":\"loadingPendingReview\"}";
    }

    public static String unavailableDiff() {
        return "{\"kind\":\"unavailableDiff\",\"reason\":\"gh: not logged in\",\"tooLarge\":false}";
    }

    public static String unavailableThreads() {
        return "{\"kind\":\"unavailableThreads\",\"reason\":\"gh: not logged in\"}";
    }

    public static String unavailablePending() {
        return "{\"kind\":\"unavailablePendingReview\",\"reason\":\"gh: not logged in\"}";
    }

    public static String refusedAnchor() {
        return "{\"kind\":\"refusedAnchor\",\"reason\":\"gh: not logged in\"}";
    }

    public static String noneInDiff() {
        return "[]";
    }

    public static String checking() {
        return "{\"kind\":\"checkingPullRequests\"}";
    }

    public static String unavailablePullRequests() {
        return "{\"kind\":\"unavailablePullRequests\",\"reason\":\"gh: not logged in\"}";
    }

    public static String reviewChanged(String pullRequests) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"review/changed\",\"params\":%s}".formatted(pullRequests);
    }

    public static String mergedChanged(String merged) {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"review/mergedChanged\",\"params\":%s}".formatted(merged);
    }

    public static Answer<Object> pullRequestsAtOnce(PullRequestsView view) {
        return call -> {
            call.<Consumer<PullRequestsView>>getArgument(1).accept(view);
            return null;
        };
    }

    public static Answer<Object> mergedAtOnce(MergedPullRequestsView view) {
        return call -> {
            call.<Consumer<MergedPullRequestsView>>getArgument(2).accept(view);
            return null;
        };
    }

    public static String commentParams() {
        return "{\"root\":\"%s\",\"number\":23,\"path\":\"src/Board.java\",".formatted(projectPath())
                + "\"position\":{\"kind\":\"placedThread\",\"side\":\"RIGHT\",\"first\":3,\"last\":3},"
                + "\"body\":\"Does this need its own Element?\"}";
    }

    public static String replyParams() {
        return "{\"root\":\"%s\",\"number\":23,\"thread\":\"PRRT_1\",\"body\":\"Does this need its own Element?\"}"
                .formatted(projectPath());
    }

    public static String editParams() {
        return "{\"root\":\"%s\",\"number\":23,\"comment\":\"PRRC_1\",\"body\":\"It does, for the cursor.\"}"
                .formatted(projectPath());
    }

    public static String deleteParams() {
        return "{\"root\":\"%s\",\"number\":23,\"comment\":\"PRRC_1\"}".formatted(projectPath());
    }

    public static String submitParams() {
        return "{\"root\":\"%s\",\"number\":23,\"summary\":\"Looks close. Two nits.\\nThe second is about naming.\"}"
                .formatted(projectPath());
    }

    public static String writtenWithNoPending() {
        return "{\"kind\":\"writtenReview\",\"pending\":{\"kind\":\"noPendingReview\"}}";
    }

    public static String refusedWrite() {
        return "{\"kind\":\"refusedWrite\",\"reason\":\"gh: not logged in\"}";
    }
}
