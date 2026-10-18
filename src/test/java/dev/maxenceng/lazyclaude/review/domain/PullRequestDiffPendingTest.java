package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.addedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.fileWithOldNumbersFarAboveNewOnes;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.pullRequestDiffOf;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOn;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onNoDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyCommentOn;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PullRequestDiffPendingTest {

    @Test
    void shouldPutAPendingCommentOnTheLineOfItsNumberOnItsSide() {
        PendingComment onTheNewSide = commentOn(diffPath(), rightOn(3));
        PendingComment onTheOldSide = commentOn(diffPath(), leftOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(List.of(onTheNewSide, onTheOldSide)).comments())
                .as("new line 3 is the added line, old line 3 the removed one above it")
                .containsExactly(onDiffLine(onTheOldSide, 1), onDiffLine(onTheNewSide, 2));
    }

    @Test
    void shouldPutAReplyOnTheLineOfItsThread() {
        PendingComment reply = replyCommentOn(diffPath(), rightOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(List.of(reply)).comments()).containsExactly(onDiffLine(reply, 2));
    }

    @Test
    void shouldPutOnNoLineAndLastAReplyToAnOutdatedThreadOrAnythingOnALineTheDiffDoesNotHold() {
        PendingComment outdatedReply = replyCommentOn(diffPath(), outdated());
        PendingComment offTheDiff = commentOn(diffPath(), rightOn(99));
        PendingComment placed = commentOn(diffPath(), rightOn(3));

        assertThat(pullRequestDiffOf(modifiedFile()).place(List.of(outdatedReply, offTheDiff, placed)).comments())
                .containsExactly(onDiffLine(placed, 2), onNoDiffLine(outdatedReply), onNoDiffLine(offTheDiff));
    }

    @Test
    void shouldListPendingCommentsInDiffOrderEvenWhereTheirLineNumbersSayOtherwise() {
        PendingComment onNewLineFifty = commentOn(diffPath(), rightOn(50));
        PendingComment onOldLineHundred = commentOn(diffPath(), leftOn(100));

        assertThat(pullRequestDiffOf(fileWithOldNumbersFarAboveNewOnes())
                        .place(List.of(onNewLineFifty, onOldLineHundred))
                        .comments())
                .containsExactly(onDiffLine(onOldLineHundred, 0), onDiffLine(onNewLineFifty, 1));
    }

    @Test
    void shouldNotPutAPendingCommentOnTheLineOfAnotherFileWithTheSameNumber() {
        PendingComment inSecond = commentOn(secondDiffPath(), rightOn(3));

        assertThat(pullRequestDiffOf(modifiedFile(), addedFile()).place(List.of(inSecond)).comments())
                .containsExactly(onNoDiffLine(inSecond));
    }

    @Test
    void shouldHoldNoCommentForAReviewWithNone() {
        assertThat(pullRequestDiffOf(modifiedFile()).place(List.of()).comments()).isEmpty();
    }
}
