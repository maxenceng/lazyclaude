package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.checkRunJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.forkPullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghTitlingWithItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghTitlingWithItsDirectory;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.listOfPullRequests;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.outcomeOfTheOnlyCheck;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readPrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.repositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.statusContextJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    void shouldHaveNoChecksForARollupThatIsNull() throws IOException {
        String output = "[{\"number\":1,\"title\":\"t\",\"headRefName\":\"b\",\"isDraft\":false,\"statusCheckRollup\":null}]";

        assertThat(readPrinted(root, output).getFirst().checks().listed()).isEmpty();
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
    void shouldAskForTheOpenPullRequestsWithTheFieldsTheBoardNeeds() throws IOException {
        List<PullRequest> read = repositoryOver(ghTitlingWithItsArguments(root)).openPullRequests(projectAt(root));

        assertThat(read.getFirst().title().value())
                .isEqualTo("pr list --state open --limit 100 --json number,title,headRefName,isDraft,isCrossRepository,statusCheckRollup");
    }

    @Test
    void shouldAskInTheProjectsDirectoryWhereverLazyclaudeWasStarted() throws IOException {
        List<PullRequest> read = repositoryOver(ghTitlingWithItsDirectory(root)).openPullRequests(projectAt(root));

        assertThat(read.getFirst().title().value()).isEqualTo(root.toRealPath().toString());
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
    void shouldBeUnavailableWhenGhPrintsSomethingThatIsNotJson() {
        assertThatThrownBy(() -> readPrinted(root, "Welcome to gh"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr list");
    }

    @Test
    void shouldBeUnavailableWhenAPullRequestLacksItsHeadBranch() {
        String output = "[{\"number\":1,\"title\":\"t\",\"isDraft\":false,\"statusCheckRollup\":[]}]";

        assertThatThrownBy(() -> readPrinted(root, output))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("head branch");
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhPullRequestsRepository repository = impatientRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.openPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}
