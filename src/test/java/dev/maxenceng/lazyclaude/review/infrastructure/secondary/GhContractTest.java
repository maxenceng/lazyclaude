package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.aMergedPullRequestWithAThread;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.discard;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.pendingReviews;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.realDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.realPullRequest;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.realThreads;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhContractFixture.thisRepository;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.review.domain.DiffFile;
import dev.maxenceng.lazyclaude.review.domain.NoPendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingReply;
import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewId;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.domain.ReviewedPullRequest;
import dev.maxenceng.lazyclaude.review.published.AddedFile;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

@Tag("contract")
class GhContractTest {

    @Test
    void shouldParseTheDiffGhPrDiffPrintsForARealPullRequestOfThisRepository() {
        assertThat(realDiff().files())
                .as("gh pr diff --color never must still print a unified diff the parser reads")
                .isNotEmpty()
                .allSatisfy(file -> assertThat(file.path().value()).isNotBlank())
                .anySatisfy(file -> assertThat(file.status()).isInstanceOf(AddedFile.class))
                .flatExtracting(DiffFile::hunks)
                .isNotEmpty();
    }

    @Test
    void shouldParseTheThreadsTheGraphqlQueryAnswersForARealPullRequestOfThisRepository() {
        assertThat(realThreads())
                .as("the reviewThreads query must still answer the fields the parser reads")
                .isNotEmpty()
                .allSatisfy(thread -> assertThat(thread.comments()).isNotEmpty());
    }

    @Test
    void shouldWriteALineCommentAndAReplyToAPendingReviewReadThemBackAndKeepThemOutOfTheThreads() {
        Project project = thisRepository();
        PullRequestNumber number = aMergedPullRequestWithAThread();
        ReviewedPullRequest before = realPullRequest();
        PendingReviewId review = switch (before.review()) {
            case PendingReview _ -> existing.id();
            case NoPendingReview _ -> pendingReviews().create(project, before.id());
        };
        try {
            PendingOnDiff firstLine = realDiff().commentOn(new DiffLineIndex(0), new DiffLineIndex(0));
            pendingReviews().comment(project, review, firstLine, new CommentBody("contract probe: a line comment"));
            pendingReviews().reply(project, review, realThreads().getFirst().id(), new CommentBody("contract probe: a reply"));

            PendingReview read = (PendingReview) realPullRequest().review();

            assertThat(read.comments())
                    .extracting(comment -> comment.body().value())
                    .contains("contract probe: a line comment", "contract probe: a reply");
            assertThat(read.comments()).extracting(PendingComment::anchor).anySatisfy(anchor -> assertThat(anchor).isInstanceOf(PendingReply.class));
            assertThat(realThreads())
                    .as("a pending comment is shown as one, never also as a comment of a thread")
                    .flatExtracting(ReviewThread::comments)
                    .extracting(comment -> comment.body().value())
                    .doesNotContain("contract probe: a line comment", "contract probe: a reply");
        } finally {
            discard(review);
        }
    }
}
