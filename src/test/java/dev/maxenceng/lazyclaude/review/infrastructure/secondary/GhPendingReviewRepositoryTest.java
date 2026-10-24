package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOn;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.commentOnDiffLine;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.onDiff;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewBuilder;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pendingReviewId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.pullRequestNodeId;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyCommentOn;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.replyToThread;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.reviewedPullRequestWith;
import static dev.maxenceng.lazyclaude.review.domain.PendingReviewFixture.unreviewedPullRequest;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.argumentsRecordedIn;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghAnsweringPendingWithItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghRecordingItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientPendingRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingPageJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingRepositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingReviewJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingThreadJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readPendingCommentsPrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readPendingPrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadCommentJson;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.commentBody;
import static dev.maxenceng.lazyclaude.review.published.CommentBodyFixture.editedCommentBody;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.pendingCommentId;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftRange;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewedPullRequest;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.Summary;

class GhPendingReviewRepositoryTest {

    private static final String COMMENT = commentBody().value();
    private static final String REPLY = editedCommentBody().value();
    private static final String REVIEW = pendingReviewJson("PRR_1", "Looks close.", "9f3c2e1a0b8d7c6e5f4a3b2c1d0e9f8a7b6c5d4e");

    @TempDir
    Path root;

    @Test
    void shouldReadThePullRequestsIdAndHeadAndNoReviewWhenGitHubListsNoPendingOne() throws IOException {
        String output = pendingPageJson(
                "", pendingThreadJson("PRRT_9", "src/Board.java", "3", "3", "null", "RIGHT", false, threadCommentJson("PRRC_9", "SUBMITTED", "hi")));

        assertThat(readPendingPrinted(root, output)).isEqualTo(unreviewedPullRequest());
    }

    @Test
    void shouldReadThePendingReviewWithItsSummaryItsCommitAndTheCommentsThatStartThreads() throws IOException {
        String output = pendingPageJson(
                pendingReviewJson("PRR_1", "Looks close.", "9f3c2e1a0b8d7c6e5f4a3b2c1d0e9f8a7b6c5d4e"),
                pendingThreadJson("PRRT_1", "src/Board.java", "3", "3", "null", "RIGHT", false, threadCommentJson("PRRC_1", "PENDING", COMMENT)));

        assertThat(readPendingPrinted(root, output))
                .isEqualTo(reviewedPullRequestWith(pendingReviewBuilder()
                        .summary(new Summary("Looks close."))
                        .comments(List.of(commentOnDiffLine()))
                        .build()));
    }

    @Test
    void shouldReadARangeFromItsStartLineToItsLineOnTheOldSideForALeftThread() throws IOException {
        String output = pendingPageJson(
                REVIEW,
                pendingThreadJson("PRRT_1", "src/Board.java", "18", "18", "12", "LEFT", false, threadCommentJson("PRRC_1", "PENDING", COMMENT)));

        assertThat(readPendingCommentsPrinted(root, output)).containsExactly(commentOn(diffPath(), leftRange(12, 18)));
    }

    @Test
    void shouldPlaceACommentOnALineThePullRequestNoLongerHasWhereItWasWritten() throws IOException {
        String output = pendingPageJson(
                REVIEW,
                pendingThreadJson("PRRT_1", "src/Board.java", "null", "7", "null", "RIGHT", true, threadCommentJson("PRRC_1", "PENDING", COMMENT)));

        assertThat(readPendingCommentsPrinted(root, output)).containsExactly(commentOn(diffPath(), rightOn(7)));
    }

    @Test
    void shouldReadAPendingCommentAfterTheFirstOfAThreadAsAReplyToIt() throws IOException {
        String output = pendingPageJson(
                REVIEW,
                pendingThreadJson(
                        "PRRT_1", "src/Board.java", "3", "3", "null", "RIGHT", false,
                        threadCommentJson("PRRC_0", "SUBMITTED", "Why?"), threadCommentJson("PRRC_2", "PENDING", REPLY)));

        assertThat(readPendingCommentsPrinted(root, output)).containsExactly(replyToThread());
    }

    @Test
    void shouldReadAReplyToAnOutdatedThreadAsSittingOnNoLine() throws IOException {
        String output = pendingPageJson(
                REVIEW,
                pendingThreadJson(
                        "PRRT_1", "src/Board.java", "null", "3", "null", "RIGHT", true,
                        threadCommentJson("PRRC_0", "SUBMITTED", "Why?"), threadCommentJson("PRRC_2", "PENDING", REPLY)));

        assertThat(readPendingCommentsPrinted(root, output)).containsExactly(replyCommentOn(diffPath(), outdated()));
    }

    @Test
    void shouldLeaveOutAPendingCommentOnAWholeFileAndTheCommentsOfOthers() throws IOException {
        String output = pendingPageJson(
                REVIEW,
                pendingThreadJson("PRRT_1", "src/Board.java", "null", "null", "null", "RIGHT", false, threadCommentJson("PRRC_1", "PENDING", COMMENT)),
                pendingThreadJson("PRRT_3", "src/Docs.java", "4", "4", "null", "RIGHT", false, threadCommentJson("PRRC_3", "SUBMITTED", "Why?")));

        assertThat(readPendingCommentsPrinted(root, output)).isEmpty();
    }

    @Test
    void shouldReadThePendingCommentsOfEveryPageOfThreads() throws IOException {
        String first = pendingPageJson(
                REVIEW, pendingThreadJson("PRRT_1", "src/Board.java", "3", "3", "null", "RIGHT", false, threadCommentJson("PRRC_1", "PENDING", COMMENT)));
        String second = pendingPageJson(
                REVIEW, pendingThreadJson("PRRT_4", "src/Docs.java", "4", "4", "null", "RIGHT", false, threadCommentJson("PRRC_1", "PENDING", COMMENT)));

        assertThat(readPendingCommentsPrinted(root, first + second))
                .containsExactly(commentOn(diffPath(), rightOn(3)), commentOn(secondDiffPath(), rightOn(4)));
    }

    @Test
    void shouldAskGraphqlForThePullRequestsHeadAndPendingReviewAndThreadsOfTheRepositoryTheDirectoryBelongsTo() throws IOException {
        ReviewedPullRequest read = pendingRepositoryOver(ghAnsweringPendingWithItsArguments(root))
                .pullRequest(projectAt(root), pullRequestNumber());

        assertThat(((PendingReview) read.review()).summary().value())
                .startsWith("api graphql --paginate -F owner={owner} -F repo={repo} -F number=23 -f query=query(")
                .contains("reviews(states: PENDING, first: 1)")
                .contains("reviewThreads(first: 100, after: $endCursor)");
    }

    @Test
    void shouldBeUnavailableSayingWhatGhSaidWhenItExitsNonZero() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghFailing(root, 4, "gh: not logged in"));

        assertThatThrownBy(() -> repository.pullRequest(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql exited with 4")
                .hasMessageContaining("gh: not logged in");
    }

    @Test
    void shouldBeUnavailableWhenGhIsNotOnThePath() {
        assertThatThrownBy(() -> pendingRepositoryOverMissingGh().pullRequest(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhPendingReviewRepository repository = impatientPendingRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.pullRequest(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsSomethingThatIsNotJson() {
        assertThatThrownBy(() -> readPendingPrinted(root, "Welcome to gh"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql does not parse");
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsNothing() {
        assertThatThrownBy(() -> readPendingPrinted(root, ""))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("printed no pull request");
    }

    @Test
    void shouldBeUnavailableWhenTheAnswerHoldsNoPullRequest() {
        assertThatThrownBy(() -> readPendingPrinted(root, "{\"data\":{\"repository\":{\"pullRequest\":null}}}"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("holds no pull request");
    }

    @Test
    void shouldBeUnavailableWhenThePullRequestLacksItsHead() {
        String output = pendingPageJson("").replace("\"headRefOid\":\"9f3c2e1a0b8d7c6e5f4a3b2c1d0e9f8a7b6c5d4e\"", "\"headRefOid\":null");

        assertThatThrownBy(() -> readPendingPrinted(root, output))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("is not a pull request");
    }

    @Test
    void shouldStartAPendingReviewWithNoEventAndAnswerItsId() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "PRR_1"));

        assertThat(repository.create(projectAt(root), pullRequestNodeId())).isEqualTo(pendingReviewId());
        assertThat(argumentsRecordedIn(root))
                .contains("pullRequestId=PR_1")
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("addPullRequestReview(input: {pullRequestId: $pullRequestId})").doesNotContain("event"));
    }

    @Test
    void shouldBeUnavailableWhenGhAnswersNoIdForTheReviewItStarted() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, ""));

        assertThatThrownBy(() -> repository.create(projectAt(root), pullRequestNodeId()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("printed no review id");
    }

    @Test
    void shouldAddACommentOnOneLineAsAThreadOfThePendingReviewWithNoStartLine() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.addThread(projectAt(root), pendingReviewId(), onDiff(diffPath(), rightOn(3)), commentBody());

        assertThat(argumentsRecordedIn(root))
                .contains("review=PRR_1", "path=src/Board.java", "body=Does this need its own Element?", "line=3", "side=RIGHT")
                .noneMatch(argument -> argument.startsWith("startLine="))
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("addPullRequestReviewThread("));
    }

    @Test
    void shouldAddARangeWithItsFirstLineAsItsStartOnTheSameSide() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.addThread(projectAt(root), pendingReviewId(), onDiff(diffPath(), leftRange(12, 18)), commentBody());

        assertThat(argumentsRecordedIn(root)).contains("line=18", "side=LEFT", "startLine=12", "startSide=LEFT");
    }

    @Test
    void shouldSendABodyThatLooksLikeAFileOrAFlagAsPlainText() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.addThread(projectAt(root), pendingReviewId(), onDiff(diffPath(), rightOn(3)), new CommentBody("@/etc/passwd"));

        assertThat(argumentsRecordedIn(root)).contains("body=@/etc/passwd");
    }

    @Test
    void shouldAddAReplyToAThreadOfThePendingReview() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.reply(projectAt(root), pendingReviewId(), threadId(), commentBody());

        assertThat(argumentsRecordedIn(root))
                .contains("review=PRR_1", "thread=PRRT_1", "body=Does this need its own Element?")
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("addPullRequestReviewThreadReply("));
    }

    @Test
    void shouldReplaceWhatAPendingCommentSays() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.edit(projectAt(root), pendingCommentId(), editedCommentBody());

        assertThat(argumentsRecordedIn(root))
                .contains("comment=PRRC_1", "body=It does, for the cursor.")
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("updatePullRequestReviewComment("));
    }

    @Test
    void shouldDeleteAPendingComment() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.delete(projectAt(root), pendingCommentId());

        assertThat(argumentsRecordedIn(root))
                .contains("comment=PRRC_1")
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("deletePullRequestReviewComment("));
    }

    @Test
    void shouldSubmitThePendingReviewAsAComment() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghRecordingItsArguments(root, "{}"));

        repository.submit(projectAt(root), pendingReviewId(), new Summary("Looks close."));

        assertThat(argumentsRecordedIn(root))
                .contains("review=PRR_1", "body=Looks close.")
                .noneSatisfy(argument -> assertThat(argument).contains("updatePullRequestReview"))
                .anySatisfy(argument -> assertThat(argument).startsWith("query=mutation").contains("submitPullRequestReview(").contains("event: COMMENT"));
    }

    @Test
    void shouldBeUnavailableSayingWhyGitHubRefusedAWrite() throws IOException {
        GhPendingReviewRepository repository = pendingRepositoryOver(ghFailing(root, 1, "gh: Review can not be submitted"));

        assertThatThrownBy(() -> repository.submit(projectAt(root), pendingReviewId(), new Summary("")))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql exited with 1")
                .hasMessageContaining("Review can not be submitted");
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerAWriteInTime() throws IOException {
        GhPendingReviewRepository repository = impatientPendingRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.delete(projectAt(root), pendingCommentId()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}
