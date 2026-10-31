package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.CommentAnchorPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.CommentAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingDeletePayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingEditPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReplyPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingSubmitPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

/**
 * The {@code review/} methods: one per operation of {@link ReviewQuery} and {@link PendingReviewCommand}, named after it, except that
 * {@code subscribe} is {@code review/watch}, which pushes {@code review/changed} and {@code review/mergedChanged} (ADR 0076). A write
 * GitHub refuses is a result, as the shell reads it.
 */
final class ReviewMethods {

    private static final ProtocolMethod<NoParams, PullRequestDiffView> LOADING_DIFF =
            new ProtocolMethod<>("review/loadingDiff", NoParams.class, PullRequestDiffView.class);
    private static final ProtocolMethod<NoParams, ThreadsView> LOADING_THREADS =
            new ProtocolMethod<>("review/loadingThreads", NoParams.class, ThreadsView.class);
    private static final ProtocolMethod<PullRequestPayload, PullRequestDiffView> DIFF_OF =
            new ProtocolMethod<>("review/diffOf", PullRequestPayload.class, PullRequestDiffView.class);
    private static final ProtocolMethod<PullRequestPayload, ThreadsView> THREADS_OF =
            new ProtocolMethod<>("review/threadsOf", PullRequestPayload.class, ThreadsView.class);
    private static final ProtocolMethod<ThreadsInDiffPayload, ThreadsInDiffView> THREADS_IN_DIFF =
            new ProtocolMethod<>("review/threadsInDiff", ThreadsInDiffPayload.class, ThreadsInDiffView.class);
    private static final ProtocolMethod<NoParams, PendingReviewView> LOADING_PENDING =
            new ProtocolMethod<>("review/loadingPending", NoParams.class, PendingReviewView.class);
    private static final ProtocolMethod<PullRequestPayload, PendingReviewView> PENDING_OF =
            new ProtocolMethod<>("review/pendingOf", PullRequestPayload.class, PendingReviewView.class);
    private static final ProtocolMethod<CommentAnchorPayload, CommentAnchorView> ANCHOR_OF =
            new ProtocolMethod<>("review/anchorOf", CommentAnchorPayload.class, CommentAnchorView.class);
    private static final ProtocolMethod<PendingCommentsInDiffPayload, PendingCommentsInDiffView> PENDING_IN_DIFF =
            new ProtocolMethod<>("review/pendingInDiff", PendingCommentsInDiffPayload.class, PendingCommentsInDiffView.class);
    private static final ProtocolMethod<NoParams, PullRequestsView> CHECKING =
            new ProtocolMethod<>("review/checking", NoParams.class, PullRequestsView.class);
    private static final ProtocolMethod<NoParams, Void> REFRESH = new ProtocolMethod<>("review/refresh", NoParams.class, Void.class);
    private static final ProtocolMethod<ReviewPayload, PullRequestsView> WATCH =
            new ProtocolMethod<>("review/watch", ReviewPayload.class, PullRequestsView.class);
    private static final ProtocolMethod<PendingCommentPayload, PendingReviewWriteView> COMMENT =
            new ProtocolMethod<>("review/comment", PendingCommentPayload.class, PendingReviewWriteView.class);
    private static final ProtocolMethod<PendingReplyPayload, PendingReviewWriteView> REPLY =
            new ProtocolMethod<>("review/reply", PendingReplyPayload.class, PendingReviewWriteView.class);
    private static final ProtocolMethod<PendingEditPayload, PendingReviewWriteView> EDIT =
            new ProtocolMethod<>("review/edit", PendingEditPayload.class, PendingReviewWriteView.class);
    private static final ProtocolMethod<PendingDeletePayload, PendingReviewWriteView> DELETE =
            new ProtocolMethod<>("review/delete", PendingDeletePayload.class, PendingReviewWriteView.class);
    private static final ProtocolMethod<PendingSubmitPayload, PendingReviewWriteView> SUBMIT =
            new ProtocolMethod<>("review/submit", PendingSubmitPayload.class, PendingReviewWriteView.class);
    private static final ProtocolNotification<PullRequestsView> CHANGED =
            new ProtocolNotification<>("review/changed", PullRequestsView.class);
    private static final ProtocolNotification<MergedPullRequestsView> MERGED_CHANGED =
            new ProtocolNotification<>("review/mergedChanged", MergedPullRequestsView.class);

    private final ReviewQuery review;
    private final PendingReviewCommand writing;
    private final SidecarOutput output;
    private final Set<Path> watching = ConcurrentHashMap.newKeySet();

    ReviewMethods(ReviewQuery review, PendingReviewCommand writing, SidecarOutput output) {
        this.review = review;
        this.writing = writing;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(
                List.of(
                        LOADING_DIFF,
                        LOADING_THREADS,
                        DIFF_OF,
                        THREADS_OF,
                        THREADS_IN_DIFF,
                        LOADING_PENDING,
                        PENDING_OF,
                        ANCHOR_OF,
                        PENDING_IN_DIFF,
                        CHECKING,
                        REFRESH,
                        WATCH,
                        COMMENT,
                        REPLY,
                        EDIT,
                        DELETE,
                        SUBMIT),
                List.of(),
                List.of(CHANGED, MERGED_CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Map.ofEntries(
                Map.entry(LOADING_DIFF.name(), params -> answered(LOADING_DIFF.answer(params, _ -> review.loadingDiff()))),
                Map.entry(LOADING_THREADS.name(), params -> answered(LOADING_THREADS.answer(params, _ -> review.loadingThreads()))),
                Map.entry(DIFF_OF.name(), params -> answered(DIFF_OF.answer(params, review::diffOf))),
                Map.entry(THREADS_OF.name(), params -> answered(THREADS_OF.answer(params, review::threadsOf))),
                Map.entry(THREADS_IN_DIFF.name(), params -> answered(THREADS_IN_DIFF.answer(params, review::threadsInDiff))),
                Map.entry(LOADING_PENDING.name(), params -> answered(LOADING_PENDING.answer(params, _ -> review.loadingPending()))),
                Map.entry(PENDING_OF.name(), params -> answered(PENDING_OF.answer(params, review::pendingOf))),
                Map.entry(ANCHOR_OF.name(), params -> answered(ANCHOR_OF.answer(params, review::anchorOf))),
                Map.entry(PENDING_IN_DIFF.name(), params -> answered(PENDING_IN_DIFF.answer(params, review::pendingInDiff))),
                Map.entry(CHECKING.name(), params -> answered(CHECKING.answer(params, _ -> review.checking()))),
                Map.entry(REFRESH.name(), this::refresh),
                Map.entry(WATCH.name(), this::watch),
                Map.entry(COMMENT.name(), params -> answered(COMMENT.answer(params, writing::comment))),
                Map.entry(REPLY.name(), params -> answered(REPLY.answer(params, writing::reply))),
                Map.entry(EDIT.name(), params -> answered(EDIT.answer(params, writing::edit))),
                Map.entry(DELETE.name(), params -> answered(DELETE.answer(params, writing::delete))),
                Map.entry(SUBMIT.name(), params -> answered(SUBMIT.answer(params, writing::submit))));
    }

    private static SidecarAnswer answered(Object view) {
        return SidecarAnswer.of(SidecarJson.tree(view));
    }

    /** A request rather than an input: the frontend is told the fetch was asked for, with a null result, and the pull requests arrive as {@code review/changed}. */
    private SidecarAnswer refresh(JsonNode params) {
        SidecarJson.params(params, REFRESH.params());
        review.refresh();
        return SidecarAnswer.of(NullNode.getInstance());
    }

    /** The watch opens once the answer, the pull requests not checked yet, is queued, so no {@code review/changed} reaches the frontend before it. */
    // deferred: a second review/watch of a root answers checking and pushes nothing until the next fetch — close it when a reloaded desktop page is seen waiting on it.
    private SidecarAnswer watch(JsonNode params) {
        PullRequestsView current = WATCH.answer(params, _ -> review.checking());
        ReviewPayload payload = SidecarJson.params(params, WATCH.params());
        return new SidecarAnswer(SidecarJson.tree(current), () -> watchOnce(payload));
    }

    /** A watch that failed to open is forgotten before another {@code review/watch} of the project is let in, so that one tries again. */
    // deferred: the one PullRequestWatchRepository serves one project, so a second root's watch shares its timer and refresh — close it when a frontend watches two projects.
    private synchronized void watchOnce(ReviewPayload payload) {
        if (watching.add(payload.root())) {
            try {
                review.subscribe(
                        payload,
                        view -> output.send(CHANGED.message(view)),
                        merged -> output.send(MERGED_CHANGED.message(merged)));
            } catch (RuntimeException failure) {
                watching.remove(payload.root());
                throw failure;
            }
        }
    }
}
