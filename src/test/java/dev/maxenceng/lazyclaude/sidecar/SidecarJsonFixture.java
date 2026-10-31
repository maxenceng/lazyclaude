package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.pendingCommentPayload;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.loadingThreadsView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffPayload;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class SidecarJsonFixture {

    private static final String POSITION = "position";

    private SidecarJsonFixture() {}

    public static JsonNode pendingCommentWithPositionOfKind(String kind) {
        ObjectNode params = pendingCommentParams();
        ((ObjectNode) params.get(POSITION)).put(ViewShape.KIND, kind);
        return params;
    }

    public static JsonNode pendingCommentWithPositionWithoutKind() {
        ObjectNode params = pendingCommentParams();
        ((ObjectNode) params.get(POSITION)).remove(ViewShape.KIND);
        return params;
    }

    public static JsonNode pendingCommentWithAParamNotStated() {
        ObjectNode params = pendingCommentParams();
        params.put("bodie", "misspelled");
        return params;
    }

    public static JsonNode threadsInDiffWithANullFile() {
        ObjectNode params = (ObjectNode) SidecarJson.tree(new ThreadsInDiffPayload(twoFileDiffView(), loadingThreadsView()));
        ((ArrayNode) params.get("diff").get("files")).addNull();
        return params;
    }

    private static ObjectNode pendingCommentParams() {
        return (ObjectNode) SidecarJson.tree(pendingCommentPayload());
    }
}
