package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final List<ServedMethod<ReviewQuery, ?, ?>> READS = List.of(
            ServedMethod.of("review/loadingDiff", NoParams.class, PullRequestDiffView.class, (review, _) -> review.loadingDiff()),
            ServedMethod.of("review/loadingThreads", NoParams.class, ThreadsView.class, (review, _) -> review.loadingThreads()),
            ServedMethod.of("review/diffOf", PullRequestPayload.class, PullRequestDiffView.class, ReviewQuery::diffOf),
            ServedMethod.of("review/threadsOf", PullRequestPayload.class, ThreadsView.class, ReviewQuery::threadsOf),
            ServedMethod.of("review/threadsInDiff", ThreadsInDiffPayload.class, ThreadsInDiffView.class, ReviewQuery::threadsInDiff),
            ServedMethod.of("review/loadingPending", NoParams.class, PendingReviewView.class, (review, _) -> review.loadingPending()),
            ServedMethod.of("review/pendingOf", PullRequestPayload.class, PendingReviewView.class, ReviewQuery::pendingOf),
            ServedMethod.of("review/anchorOf", CommentAnchorPayload.class, CommentAnchorView.class, ReviewQuery::anchorOf),
            ServedMethod.of(
                    "review/pendingInDiff", PendingCommentsInDiffPayload.class, PendingCommentsInDiffView.class, ReviewQuery::pendingInDiff),
            ServedMethod.of("review/checking", NoParams.class, PullRequestsView.class, (review, _) -> review.checking()));
    // deferred: review/comment does not check its anchor against the diff, so a cross-hunk range reaches the user as GitHub's refusal — close it when a frontend comments without asking review/anchorOf first.
    private static final List<ServedMethod<PendingReviewCommand, ?, ?>> WRITES = List.of(
            ServedMethod.of("review/comment", PendingCommentPayload.class, PendingReviewWriteView.class, PendingReviewCommand::comment),
            ServedMethod.of("review/reply", PendingReplyPayload.class, PendingReviewWriteView.class, PendingReviewCommand::reply),
            ServedMethod.of("review/edit", PendingEditPayload.class, PendingReviewWriteView.class, PendingReviewCommand::edit),
            ServedMethod.of("review/delete", PendingDeletePayload.class, PendingReviewWriteView.class, PendingReviewCommand::delete),
            ServedMethod.of("review/submit", PendingSubmitPayload.class, PendingReviewWriteView.class, PendingReviewCommand::submit));
    private static final ProtocolMethod<NoParams, Void> REFRESH = new ProtocolMethod<>("review/refresh", NoParams.class, Void.class);
    private static final ProtocolMethod<ReviewPayload, PullRequestsView> WATCH =
            new ProtocolMethod<>("review/watch", ReviewPayload.class, PullRequestsView.class);
    private static final ProtocolNotification<PullRequestsView> CHANGED =
            new ProtocolNotification<>("review/changed", PullRequestsView.class);
    private static final ProtocolNotification<MergedPullRequestsView> MERGED_CHANGED =
            new ProtocolNotification<>("review/mergedChanged", MergedPullRequestsView.class);

    private final ReviewQuery review;
    private final PendingReviewCommand writing;
    private final SidecarOutput output;
    private final WatchedRoot watched = new WatchedRoot(WATCH.name());

    ReviewMethods(ReviewQuery review, PendingReviewCommand writing, SidecarOutput output) {
        this.review = review;
        this.writing = writing;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(
                Stream.of(
                                READS.stream().map(ServedMethod::method),
                                Stream.of(REFRESH, WATCH),
                                WRITES.stream().map(ServedMethod::method))
                        .<ProtocolMethod<?, ?>>flatMap(declared -> declared)
                        .toList(),
                List.of(),
                List.of(CHANGED, MERGED_CHANGED));
    }

    Map<String, SidecarMethod> methods() {
        return Stream.of(
                        READS.stream().map(read -> read.boundTo(review)),
                        WRITES.stream().map(write -> write.boundTo(writing)),
                        Stream.of(Map.<String, SidecarMethod>entry(REFRESH.name(), this::refresh), Map.<String, SidecarMethod>entry(WATCH.name(), this::watch)))
                .flatMap(served -> served)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** A request rather than an input: the frontend is told the fetch was asked for, with a null result, and the pull requests arrive as {@code review/changed}. */
    private SidecarAnswer refresh(JsonNode params) {
        SidecarJson.params(params, REFRESH.params());
        review.refresh();
        return SidecarAnswer.of(NullNode.getInstance());
    }

    /**
     * The watch opens once the answer, the pull requests not checked yet, is queued, so no {@code review/changed} reaches the frontend
     * before it. A repeat watch, a reloaded page, asks for a fetch, since the pull requests last pushed are not kept to answer it with.
     */
    private SidecarAnswer watch(JsonNode params) {
        ReviewPayload payload = SidecarJson.params(params, WATCH.params());
        watched.claim(payload.root());
        PullRequestsView current = WATCH.answer(params, _ -> review.checking());
        return new SidecarAnswer(SidecarJson.tree(current), () -> watched.open(() -> subscribe(payload), review::refresh));
    }

    private void subscribe(ReviewPayload payload) {
        review.subscribe(
                payload,
                view -> output.send(CHANGED.message(view)),
                merged -> output.send(MERGED_CHANGED.message(merged)));
    }
}
