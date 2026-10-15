package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.forkMergedPullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghMergedWithItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghMergedWithItsDirectory;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientMergedRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.listOfPullRequests;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.mergedPullRequestJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.mergedRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.mergedRepositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readMergedPrinted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

class GhMergedPullRequestsRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldReadANumberAHeadBranchAndTheCommitTheHeadWasAt() throws IOException {
        MergedPullRequest read =
                readMergedPrinted(root, listOfPullRequests(mergedPullRequestJson(23, "PROJ-1-pr", "0a1b2c"))).getFirst();

        assertThat(read.number().value()).isEqualTo(23);
        assertThat(read.head().value()).isEqualTo("PROJ-1-pr");
        assertThat(read.headCommit().value()).isEqualTo("0a1b2c");
    }

    @Test
    void shouldLeaveOutAPullRequestFromAForkEvenOnTheSameHeadBranch() throws IOException {
        List<MergedPullRequest> read = readMergedPrinted(
                root,
                listOfPullRequests(
                        forkMergedPullRequestJson(31, "PROJ-1-pr", "ffff"), mergedPullRequestJson(23, "PROJ-1-pr", "0a1b2c")));

        assertThat(read)
                .as("a fork's head carries no owner, so it would hide this repository's own branch behind a merge that is not its")
                .extracting(pullRequest -> pullRequest.number().value())
                .containsExactly(23);
    }

    @Test
    void shouldListNothingWhenNoPullRequestIsMerged() throws IOException {
        assertThat(readMergedPrinted(root, "[]")).isEmpty();
    }

    @Test
    void shouldAskForTheHundredMostRecentMergedPullRequestsWithTheFieldsTheBoardNeeds() throws IOException {
        List<MergedPullRequest> read = mergedRepositoryOver(ghMergedWithItsArguments(root)).mergedPullRequests(projectAt(root));

        assertThat(read.getFirst().head().value())
                .isEqualTo("pr list --state merged --limit 100 --json headRefName,headRefOid,number,isCrossRepository");
    }

    @Test
    void shouldAskInTheProjectsDirectoryWhereverLazyclaudeWasStarted() throws IOException {
        List<MergedPullRequest> read = mergedRepositoryOver(ghMergedWithItsDirectory(root)).mergedPullRequests(projectAt(root));

        assertThat(read.getFirst().head().value()).isEqualTo(root.toRealPath().toString());
    }

    @Test
    void shouldBeUnavailableSayingWhatGhSaidWhenItExitsNonZero() throws IOException {
        GhMergedPullRequestsRepository repository = mergedRepositoryOver(ghFailing(root, 4, "gh: not logged in"));

        assertThatThrownBy(() -> repository.mergedPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("exited with 4")
                .hasMessageContaining("gh: not logged in");
    }

    @Test
    void shouldBeUnavailableWhenGhIsNotOnThePath() {
        assertThatThrownBy(() -> mergedRepositoryOverMissingGh().mergedPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsSomethingThatIsNotJson() {
        assertThatThrownBy(() -> readMergedPrinted(root, "Welcome to gh"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr list");
    }

    @Test
    void shouldBeUnavailableWhenAPullRequestLacksItsHeadCommit() {
        String output = "[{\"number\":1,\"headRefName\":\"b\"}]";

        assertThatThrownBy(() -> readMergedPrinted(root, output))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("head commit");
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhMergedPullRequestsRepository repository = impatientMergedRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.mergedPullRequests(projectAt(root)))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}
