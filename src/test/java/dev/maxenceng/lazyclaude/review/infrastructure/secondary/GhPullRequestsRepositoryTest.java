package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.argumentsRecordedIn;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.checkRunJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.closingIssueJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.closingIssuesJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.commitsOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.countJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.directoryRecordedIn;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.forkPullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghRecordingItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghRecordingItsDirectory;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.listOfPullRequests;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.outcomeOfTheOnlyCheck;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingReviewsJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonClosing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithComments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithCommits;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithPendingReviewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithReviews;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithTime;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJsonWithout;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readOnePrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readPrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.repositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.rollupOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.statusContextJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.thisRepositoryName;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadsJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.maxenceng.lazyclaude.review.domain.ClosedIssueNumber;
import dev.maxenceng.lazyclaude.review.domain.ClosesIssue;
import dev.maxenceng.lazyclaude.review.domain.ClosesNoIssue;
import dev.maxenceng.lazyclaude.review.domain.CommentCount;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;

class GhPullRequestsRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldReadANumberATitleAHeadAndWhetherItIsDraft() throws IOException {
        PullRequest read = readPrinted(root, listOfPullRequests(pullRequestJson(23, "PR and CI", "PROJ-1-pr", true)))
                .getFirst();

        assertThat(read.number().value()).isEqualTo(23);
        assertThat(read.title().value()).isEqualTo("PR and CI");
        assertThat(read.head().value()).isEqualTo("PROJ-1-pr");
        assertThat(read.draft()).isTrue();
    }

    @Test
    void shouldReadWhenItWasOpenedAsAnInstant() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithTime("2026-11-13T10:21:46Z")).openedAt().at())
                .isEqualTo(Instant.parse("2026-11-13T10:21:46Z"));
    }

    @Test
    void shouldBeUnavailableWhenItsCreatedAtIsNotATime() {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonWithTime("last Tuesday")))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("last Tuesday");
    }

    @Test
    void shouldBeUnavailableWhenAPullRequestLacksWhenItWasOpened() {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonWithout("createdAt")))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("created at");
    }

    @ParameterizedTest
    @CsvSource({
        "comments,comment count",
        "reviewThreads,review threads",
        "reviews,pending reviews",
        "closingIssuesReferences,closing issues",
        "baseRepository,base repository"
    })
    void shouldBeUnavailableWhenAPullRequestLacksWhatItsFactsAreReadFrom(String field, String named) {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonWithout(field)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(named);
    }

    @Test
    void shouldCountItsConversationComments() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithComments(countJson(2), threadsJson())).comments())
                .isEqualTo(new CommentCount(2));
    }

    @Test
    void shouldCountNoCommentsForAPullRequestNobodyCommentedOn() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithComments(countJson(0), threadsJson())).comments())
                .isEqualTo(new CommentCount(0));
    }

    @Test
    void shouldAddEveryCommentOfEveryReviewThreadToTheConversationOnes() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithComments(countJson(2), threadsJson(3, 1, 4))).comments())
                .as("a thread with several comments counts each, not once")
                .isEqualTo(new CommentCount(10));
    }

    @Test
    void shouldLeaveOutTheCommentsOfYourPendingReviewWhichNobodyElseCanSee() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithPendingReviewOf(2, 5)).comments())
                .isEqualTo(new CommentCount(3));
    }

    @Test
    void shouldHaveAPendingReviewWhenGitHubShowsYouOne() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithReviews(pendingReviewsJson(0))).pendingReview()).isTrue();
    }

    @Test
    void shouldHaveNoPendingReviewWhenGitHubShowsYouNone() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithReviews(threadsJson())).pendingReview()).isFalse();
    }

    @Test
    void shouldCloseTheIssueItNames() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonClosing(closingIssuesJson(closingIssueJson(215, thisRepositoryName())))).closing())
                .isEqualTo(new ClosesIssue(new ClosedIssueNumber(215)));
    }

    @Test
    void shouldCloseTheFirstIssueWhenItNamesSeveral() throws IOException {
        assertThat(readOnePrinted(
                                root,
                                pullRequestJsonClosing(closingIssuesJson(
                                        closingIssueJson(7, thisRepositoryName()), closingIssueJson(9, thisRepositoryName()))))
                        .closing())
                .isEqualTo(new ClosesIssue(new ClosedIssueNumber(7)));
    }

    @Test
    void shouldCloseTheFirstIssueOfThisRepositoryWhenAnotherRepositoryComesFirst() throws IOException {
        assertThat(readOnePrinted(
                                root,
                                pullRequestJsonClosing(closingIssuesJson(
                                        closingIssueJson(3, "acme/elsewhere"), closingIssueJson(9, thisRepositoryName()))))
                        .closing())
                .as("an issue number of another repository is not a ticket of this one")
                .isEqualTo(new ClosesIssue(new ClosedIssueNumber(9)));
    }

    @Test
    void shouldCloseNoIssueForNoneOrForAnotherRepositoryOnly() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonClosing(closingIssuesJson())).closing()).isEqualTo(new ClosesNoIssue());
        assertThat(readOnePrinted(root, pullRequestJsonClosing(closingIssuesJson(closingIssueJson(3, "acme/elsewhere")))).closing())
                .isEqualTo(new ClosesNoIssue());
    }

    @Test
    void shouldBeUnavailableWhenAClosedIssueLacksItsNumber() {
        assertThatThrownBy(() -> readOnePrinted(
                        root, pullRequestJsonClosing(closingIssuesJson("{\"repository\":{\"nameWithOwner\":\"acme/app\"}}"))))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("closed issue number");
    }

    @Test
    void shouldBeUnavailableWhenAClosedIssueLacksItsRepository() {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonClosing(closingIssuesJson("{\"number\":4}"))))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("closed issue repository");
    }

    @Test
    void shouldBeUnavailableWhenAThreadLacksItsCommentCount() {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonWithComments(countJson(0), "{\"nodes\":[{}]}")))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("review thread comment count");
    }

    @Test
    void shouldLeaveOutAPullRequestFromAForkEvenOnTheSameHeadBranch() throws IOException {
        List<PullRequest> read = readPrinted(
                root,
                listOfPullRequests(
                        forkPullRequestJson(31, "from a fork", "PROJ-1-pr"), pullRequestJson(23, "PR and CI", "PROJ-1-pr", false)));

        assertThat(read)
                .as("a fork's head carries no owner, so it would shadow this repository's own pull request")
                .extracting(pullRequest -> pullRequest.number().value())
                .containsExactly(23);
    }

    @Test
    void shouldListNothingWhenNoPullRequestIsOpen() throws IOException {
        assertThat(readPrinted(root, "[]")).isEmpty();
    }

    @Test
    void shouldHaveNoChecksForAnEmptyRollup() throws IOException {
        assertThat(readPrinted(root, listOfPullRequests(pullRequestJson(1, "t", "b", false))).getFirst().checks().listed())
                .isEmpty();
    }

    @Test
    void shouldHaveNoChecksForACommitWithNoRollup() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithCommits(commitsOf("null"))).checks().listed()).isEmpty();
    }

    @Test
    void shouldHaveNoChecksForAPullRequestWithNoCommit() throws IOException {
        assertThat(readOnePrinted(root, pullRequestJsonWithCommits("{\"nodes\":[]}")).checks().listed()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILURE", "TIMED_OUT", "CANCELLED", "ACTION_REQUIRED", "STARTUP_FAILURE", "ERROR"})
    void shouldTakeACompletedRunWithThisConclusionAsFailed(String conclusion) throws IOException {
        assertThat(outcomeOfTheOnlyCheck(root, checkRunJson("build", "COMPLETED", conclusion)))
                .isEqualTo(CheckOutcome.FAILED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUEUED", "IN_PROGRESS", "PENDING", "WAITING", "REQUESTED"})
    void shouldTakeARunThatIsNotCompleteAsPendingWhateverItsConclusionSays(String status) throws IOException {
        assertThat(outcomeOfTheOnlyCheck(root, checkRunJson("build", status, ""))).isEqualTo(CheckOutcome.PENDING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUCCESS", "NEUTRAL", "SKIPPED"})
    void shouldTakeACompletedRunWithThisConclusionAsPassed(String conclusion) throws IOException {
        assertThat(outcomeOfTheOnlyCheck(root, checkRunJson("build", "COMPLETED", conclusion)))
                .isEqualTo(CheckOutcome.PASSED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILURE", "ERROR"})
    void shouldTakeACommitStatusInThisStateAsFailed(String state) throws IOException {
        assertThat(outcomeOfTheOnlyCheck(root, statusContextJson("ci/build", state))).isEqualTo(CheckOutcome.FAILED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "EXPECTED"})
    void shouldTakeACommitStatusInThisStateAsPending(String state) throws IOException {
        assertThat(outcomeOfTheOnlyCheck(root, statusContextJson("ci/build", state))).isEqualTo(CheckOutcome.PENDING);
    }

    @Test
    void shouldTakeASuccessfulCommitStatusAsPassedUnderItsContextName() throws IOException {
        PullRequest read = readPrinted(
                        root,
                        listOfPullRequests(pullRequestJson(1, "t", "b", false, statusContextJson("ci/build", "SUCCESS"))))
                .getFirst();

        assertThat(read.checks().checks().getFirst().name().value()).isEqualTo("ci/build");
        assertThat(read.checks().checks().getFirst().outcome()).isEqualTo(CheckOutcome.PASSED);
    }

    @Test
    void shouldListMixedChecksFailedOverPendingOverPassed() throws IOException {
        List<PullRequest> read = readPrinted(
                root,
                listOfPullRequests(
                        pullRequestJson(
                                1, "t", "b", false, checkRunJson("a", "COMPLETED", "SUCCESS"), checkRunJson("b", "QUEUED", "")),
                        pullRequestJson(
                                2, "t", "c", false, checkRunJson("a", "IN_PROGRESS", ""), statusContextJson("ci", "ERROR"))));

        assertThat(read.get(0).checks().listed().getFirst().outcome()).isEqualTo(CheckOutcome.PENDING);
        assertThat(read.get(1).checks().listed().getFirst().outcome()).isEqualTo(CheckOutcome.FAILED);
    }

    @Test
    void shouldAskOneGraphqlQueryForTheOpenPullRequestsNewestFirstInThisRepository() throws IOException {
        repositoryOver(ghRecordingItsArguments(root, "[]")).openPullRequests(projectAt(root));

        assertThat(argumentsRecordedIn(root))
                .containsSequence("api", "graphql")
                .contains("owner={owner}", "repo={repo}", ".data.repository.pullRequests.nodes")
                .anySatisfy(argument -> assertThat(argument)
                        .startsWith("query=")
                        .contains("pullRequests(states: OPEN, first: 100, orderBy: {field: CREATED_AT, direction: DESC})")
                        .contains("reviews(states: PENDING, first: 1)")
                        .contains("reviewThreads(first: 100)")
                        .contains("closingIssuesReferences(first: 10)"));
    }

    @Test
    void shouldAskInTheProjectsDirectoryWhereverLazyclaudeWasStarted() throws IOException {
        repositoryOver(ghRecordingItsDirectory(root, "[]")).openPullRequests(projectAt(root));

        assertThat(directoryRecordedIn(root)).isEqualTo(root.toRealPath());
    }

    @Test
    void shouldBeUnavailableSayingWhatGhSaidWhenItExitsNonZero() throws IOException {
        GhPullRequestsRepository repository = repositoryOver(ghFailing(root, 4, "gh: not logged in"));

        assertThatThrownBy(() -> repository.openPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("exited with 4")
                .hasMessageContaining("gh: not logged in");
    }

    @Test
    void shouldBeUnavailableWhenGhIsNotOnThePath() {
        assertThatThrownBy(() -> repositoryOverMissingGh().openPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenTheRepositoryAnswersNoListOfPullRequests() {
        assertThatThrownBy(() -> readPrinted(root, "null"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("no list of pull requests");
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsSomethingThatIsNotJson() {
        assertThatThrownBy(() -> readPrinted(root, "Welcome to gh"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql");
    }

    @Test
    void shouldBeUnavailableWhenAPullRequestLacksItsHeadBranch() {
        assertThatThrownBy(() -> readPrinted(root, listOfPullRequests(pullRequestJsonWithout("headRefName"))))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("head branch");
    }

    @Test
    void shouldBeUnavailableWhenACheckLacksItsName() {
        assertThatThrownBy(() -> readOnePrinted(root, pullRequestJsonWithCommits(commitsOf(rollupOf("{\"status\":\"COMPLETED\"}")))))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("wrote a pull request without check name");
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhPullRequestsRepository repository = impatientRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.openPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}
