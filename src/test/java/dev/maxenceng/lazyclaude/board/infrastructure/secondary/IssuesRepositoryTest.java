package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrinting;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrintingItsArguments;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrintingItsDirectory;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.impatientRepositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.issueJson;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.listOfIssues;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.repositoryOverMissingGh;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;

class IssuesRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldReadEveryIssueTheListPrintsAsAnEntry() throws IOException {
        Path gh = ghPrinting(
                root,
                listOfIssues(
                        issueJson(2, "Second", "OPEN", "", "status:todo", "context:board", "type:feature"),
                        issueJson(1, "First", "CLOSED", "COMPLETED", "context:board", "type:fix")));

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .extracting(entry -> entry.id().value())
                .containsExactly("1", "2");
    }

    @Test
    void shouldListTheIssuesInNumberOrderNotTheOrderGhPrintsThem() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJson(10, "t", "OPEN", ""), issueJson(9, "t", "OPEN", "")));

        assertThat(repositoryOver(gh).entries(projectAt(root))).extracting(entry -> entry.id().value()).containsExactly("9", "10");
    }

    @Test
    void shouldListAnIssueClosedAsNotPlannedAsUnreadable() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJson(3, "t", "CLOSED", "NOT_PLANNED")));

        List<BacklogEntry> entries = repositoryOver(gh).entries(projectAt(root));

        assertThat(entries).singleElement().isInstanceOfSatisfying(UnreadableTicket.class, unreadable ->
                assertThat(unreadable.reason().value()).isEqualTo("closed as not planned"));
    }

    @Test
    void shouldListNothingWhenThereIsNoIssue() throws IOException {
        assertThat(repositoryOver(ghPrinting(root, "[]")).entries(projectAt(root))).isEmpty();
    }

    @Test
    void shouldAskForEveryStateWithTheFieldsTheBoardReads() throws IOException {
        Path gh = ghPrintingItsArguments(root);

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.title().value())
                        .isEqualTo("issue list --state all --limit 1000 --json number,title,body,labels,state,stateReason"));
    }

    @Test
    void shouldRunInTheProjectsDirectoryForGhToFindTheRemote() throws IOException {
        Path gh = ghPrintingItsDirectory(root);

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.title().value()).isEqualTo(root.toString()));
    }

    @Test
    void shouldSayThereIsNoBacklogWhenGhFindsNoGitHubRemote() throws IOException {
        Path gh = ghFailing(root, 1, "no git remotes found");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogNotFoundException.class)
                .hasMessage("this project has no GitHub remote, so it has no backlog");
    }

    @Test
    void shouldSayThereIsNoBacklogWhenNoRemoteIsAGitHubHostEvenThoughGhSuggestsLoggingIn() throws IOException {
        Path gh = ghFailing(
                root,
                1,
                "none of the git remotes configured for this repository point to a known GitHub host. To tell gh about a new GitHub host, please use `gh auth login`");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root))).isExactlyInstanceOf(BacklogNotFoundException.class);
    }

    @Test
    void shouldNameGhAuthLoginWhenGhIsNotLoggedIn() throws IOException {
        Path gh = ghFailing(root, 4, "To get started with GitHub CLI, please run:  gh auth login");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh is not logged in; run gh auth login");
    }

    @Test
    void shouldGiveGhsOwnWordsWhenGitHubCannotBeReached() throws IOException {
        Path gh = ghFailing(root, 1, "error connecting to api.github.com");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("cannot read the issues from GitHub: error connecting to api.github.com");
    }

    @Test
    void shouldSayWhyWhenGhIsNotInstalled() {
        assertThatThrownBy(() -> repositoryOverMissingGh().entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessageContaining("cannot run " + missingCommand());
    }

    @Test
    void shouldGiveUpOnAGhThatDoesNotAnswerInTime() throws IOException {
        Path gh = ghHanging(root);

        assertThatThrownBy(() -> impatientRepositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessageContaining("did not answer within 0 s");
    }

    @Test
    void shouldSayWhatGhPrintedThatDoesNotParse() throws IOException {
        Path gh = ghPrinting(root, "not json");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessageStartingWith("gh issue list does not parse: ");
    }

    @Test
    void shouldNameTheFieldAnIssueIsWithout() throws IOException {
        Path gh = ghPrinting(root, "[{\"title\":\"t\",\"state\":\"OPEN\"}]");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh issue list wrote an issue without number");
    }

    @Test
    void shouldKeepNothingBetweenReads() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJson(1, "t", "OPEN", "")));
        IssuesRepository repository = repositoryOver(gh);

        assertThat(repository.entries(projectAt(root))).isEqualTo(repository.entries(projectAt(root)));
    }
}
