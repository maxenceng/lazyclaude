package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.domain.DiffFileFixture.modifiedFile;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.diffRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.diffRepositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghDiffingWithItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghDiffingWithItsDirectory;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghPrinting;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghPrintingALargeDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghRefusingALargeDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientDiffRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.linesOfALargeDiff;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.whileEveryCommonPoolThreadIsBusy;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.UnifiedDiffFixture.modifiedFileDiff;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.review.domain.PullRequestDiff;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffTooLargeException;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineContent;

class GhPullRequestDiffRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldReadTheUnifiedDiffGhPrints() throws IOException {
        PullRequestDiff read = diffRepositoryOver(ghPrinting(root, modifiedFileDiff())).diff(projectAt(root), pullRequestNumber());

        assertThat(read.files()).isEqualTo(List.of(modifiedFile()));
    }

    @Test
    void shouldReadADiffLargerThanAPipeHoldsWhileEveryCommonPoolThreadIsBusy() throws Exception {
        Path gh = ghPrintingALargeDiff(root);

        PullRequestDiff read = whileEveryCommonPoolThreadIsBusy(() -> diffRepositoryOver(gh).diff(projectAt(root), pullRequestNumber()));

        assertThat(read.files().getFirst().hunks().getFirst().lines())
                .as("gh blocks on a full pipe until its output is read, so a reader queued behind busy shared threads stalls it")
                .hasSize(linesOfALargeDiff());
    }

    @Test
    void shouldAskForThePullRequestsDiffWithoutColour() throws IOException {
        PullRequestDiff read = diffRepositoryOver(ghDiffingWithItsArguments(root)).diff(projectAt(root), pullRequestNumber());

        assertThat(((AddedLine) read.files().getFirst().hunks().getFirst().lines().getFirst()).content())
                .isEqualTo(new DiffLineContent("pr diff 23 --color never"));
    }

    @Test
    void shouldAskInTheProjectsDirectoryWhereverLazyclaudeWasStarted() throws IOException {
        PullRequestDiff read = diffRepositoryOver(ghDiffingWithItsDirectory(root)).diff(projectAt(root), pullRequestNumber());

        assertThat(((AddedLine) read.files().getFirst().hunks().getFirst().lines().getFirst()).content())
                .isEqualTo(new DiffLineContent(root.toRealPath().toString()));
    }

    @Test
    void shouldBeUnavailableSayingWhatGhSaidWhenItExitsNonZero() throws IOException {
        GhPullRequestDiffRepository repository = diffRepositoryOver(ghFailing(root, 4, "gh: not logged in"));

        assertThatThrownBy(() -> repository.diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff exited with 4")
                .hasMessageContaining("gh: not logged in");
    }

    @Test
    void shouldBeTooLargeWhenGitHubRefusesToProduceTheDiff() throws IOException {
        GhPullRequestDiffRepository repository = diffRepositoryOver(ghRefusingALargeDiff(root));

        assertThatThrownBy(() -> repository.diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestDiffTooLargeException.class)
                .hasMessageContaining("exceeded the maximum number of lines");
    }

    @Test
    void shouldBeUnavailableNotTooLargeForAnyOtherFailureOfGh() throws IOException {
        GhPullRequestDiffRepository repository = diffRepositoryOver(ghFailing(root, 1, "HTTP 502: Bad Gateway"));

        assertThatThrownBy(() -> repository.diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class);
    }

    @Test
    void shouldBeUnavailableWhenGhIsNotOnThePath() {
        assertThatThrownBy(() -> diffRepositoryOverMissingGh().diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhPullRequestDiffRepository repository = impatientDiffRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsAHunkItCannotNumber() throws IOException {
        GhPullRequestDiffRepository repository = diffRepositoryOver(ghPrinting(root, "diff --git a/x b/x\n@@ nonsense @@\n+x"));

        assertThatThrownBy(() -> repository.diff(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh pr diff does not parse");
    }
}
