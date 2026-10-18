package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.domain.ReviewThreadFixture.threadOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.commentJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghAnsweringThreadsWithItsArguments;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.impatientThreadRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.pendingCommentJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.readThreadsPrinted;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadJson;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadRepositoryOver;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadRepositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.review.infrastructure.secondary.FakeGhFixture.threadsPageJson;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.PendingIdFixture.threadId;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.answer;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.comment;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.question;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.leftOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;

class GhThreadRepositoryTest {

    private static final String QUESTION = commentJson("alice", "Why not reuse it?");
    private static final String ANSWER = commentJson("bob", "It assumes markdown.");

    @TempDir
    Path root;

    @Test
    void shouldReadAThreadOnOneLineOfTheNewSideAsRunningFromThatLineToItself() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "3", "null", "RIGHT", false, false, QUESTION, ANSWER));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), rightOn(3), question(), answer()));
    }

    @Test
    void shouldReadAThreadWhoseStartLineIsItsOwnLineAsOnOneLine() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "5", "5", "RIGHT", false, false, QUESTION));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), rightOn(5), question()));
    }

    @Test
    void shouldReadARangeFromItsStartLineToItsLine() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "18", "12", "RIGHT", false, false, QUESTION));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), rightRange(12, 18), question()));
    }

    @Test
    void shouldReadAThreadOnTheOldSideAsLeft() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "7", "null", "LEFT", false, false, QUESTION));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), leftOn(7), question()));
    }

    @Test
    void shouldReadAThreadWithNoLineAsOutdated() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "null", "null", "RIGHT", false, true, QUESTION));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), outdated(), question()));
    }

    @Test
    void shouldReadAThreadGitHubMarksOutdatedAsOutdatedEvenWhenItStillNamesALine() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "3", "null", "RIGHT", false, true, QUESTION));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(diffPath(), outdated(), question()));
    }

    @Test
    void shouldReadWhetherAThreadIsResolved() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "3", "null", "RIGHT", true, false, QUESTION));

        assertThat(readThreadsPrinted(root, output)).extracting(ReviewThread::resolved).containsExactly(true);
    }

    @Test
    void shouldNameTheAuthorOfADeletedAccountGhost() throws IOException {
        String output = threadsPageJson(threadJson("src/Board.java", "3", "null", "RIGHT", false, false, commentJson(null, "gone")));

        assertThat(readThreadsPrinted(root, output).getFirst().comments()).containsExactly(comment("ghost", "gone"));
    }

    @Test
    void shouldReadTheIdOfAThreadAndSkipTheCommentsOfAPendingReview() throws IOException {
        String output = threadsPageJson(threadJson(
                "src/Board.java", "3", "null", "RIGHT", false, false, QUESTION, pendingCommentJson("me", "my reply")));

        assertThat(readThreadsPrinted(root, output))
                .as("a pending comment is shown as one, never also as a comment of a thread")
                .containsExactly(threadOn(diffPath(), rightOn(3), question()));
        assertThat(readThreadsPrinted(root, output).getFirst().id()).isEqualTo(threadId());
    }

    @Test
    void shouldLeaveOutAThreadWhoseOnlyCommentsArePending() throws IOException {
        String output = threadsPageJson(
                threadJson("src/Board.java", "3", "null", "RIGHT", false, false, pendingCommentJson("me", "mine")),
                threadJson("src/Docs.java", "4", "null", "RIGHT", false, false, ANSWER));

        assertThat(readThreadsPrinted(root, output)).containsExactly(threadOn(secondDiffPath(), rightOn(4), answer()));
    }

    @Test
    void shouldReadEveryPageOfThreadsInTheOrderGhPrintsThem() throws IOException {
        String first = threadsPageJson(threadJson("src/Board.java", "3", "null", "RIGHT", false, false, QUESTION));
        String second = threadsPageJson(threadJson("src/Docs.java", "4", "null", "RIGHT", false, false, ANSWER));

        assertThat(readThreadsPrinted(root, first + second))
                .containsExactly(threadOn(diffPath(), rightOn(3), question()), threadOn(secondDiffPath(), rightOn(4), answer()));
    }

    @Test
    void shouldListNothingWhenThePullRequestHasNoThread() throws IOException {
        assertThat(readThreadsPrinted(root, threadsPageJson())).isEmpty();
    }

    @Test
    void shouldAskGraphqlForThePullRequestsThreadsOfTheRepositoryTheDirectoryBelongsTo() throws IOException {
        List<ReviewThread> read = threadRepositoryOver(ghAnsweringThreadsWithItsArguments(root))
                .threads(projectAt(root), pullRequestNumber());

        assertThat(read.getFirst().comments().getFirst().body().value())
                .startsWith("api graphql --paginate -F owner={owner} -F repo={repo} -F number=23 -f query=query(")
                .contains("reviewThreads(first: 100, after: $endCursor)");
    }

    @Test
    void shouldBeUnavailableSayingWhatGhSaidWhenItExitsNonZero() throws IOException {
        GhThreadRepository repository = threadRepositoryOver(ghFailing(root, 4, "gh: not logged in"));

        assertThatThrownBy(() -> repository.threads(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql exited with 4")
                .hasMessageContaining("gh: not logged in");
    }

    @Test
    void shouldBeUnavailableWhenGhIsNotOnThePath() {
        assertThatThrownBy(() -> threadRepositoryOverMissingGh().threads(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenGhDoesNotAnswerInTime() throws IOException {
        GhThreadRepository repository = impatientThreadRepositoryOver(ghHanging(root));

        assertThatThrownBy(() -> repository.threads(projectAt(root), pullRequestNumber()))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    void shouldBeUnavailableWhenGhPrintsSomethingThatIsNotJson() {
        assertThatThrownBy(() -> readThreadsPrinted(root, "Welcome to gh"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("gh api graphql does not parse");
    }

    @Test
    void shouldBeUnavailableWhenTheAnswerHoldsNoPullRequest() {
        assertThatThrownBy(() -> readThreadsPrinted(root, "{\"data\":{\"repository\":{\"pullRequest\":null}}}"))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("holds no review threads");
    }

    @Test
    void shouldBeUnavailableWhenAThreadLacksItsPath() {
        String output = threadsPageJson(threadJson(null, "3", "null", "RIGHT", false, false, QUESTION));

        assertThatThrownBy(() -> readThreadsPrinted(root, output))
                .isInstanceOf(PullRequestsUnavailableException.class)
                .hasMessageContaining("is not a review thread");
    }
}
