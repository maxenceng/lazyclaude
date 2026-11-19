package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrinting;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrintingItsArguments;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrintingItsDirectory;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.impatientRepositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.issueJson;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.issueJsonLinkedTo;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.listOfIssues;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.pageOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.pagesOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.repositoryOverMissingGh;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;

class IssuesRepositoryTest {

    private static final int PAGE = 600;
    private static final int CAP = 1000;
    private static final int OVER_CAP = 1001;

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
                .containsExactlyInAnyOrder("1", "2");
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
        assertThat(repositoryOver(ghPrinting(root, pagesOf(pageOf()))).entries(projectAt(root))).isEmpty();
    }

    @Test
    void shouldAskForEveryStateWithTheFieldsTheBoardReads() throws IOException {
        Path gh = ghPrintingItsArguments(root);

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.title().value())
                        .startsWith("api graphql --paginate --slurp -F owner={owner} -F name={repo} -f query=")
                        .contains("states: [OPEN, CLOSED]", "orderBy: {field: CREATED_AT, direction: DESC}", "first: 100")
                        .contains("number title body state stateReason closedAt")
                        .contains("labels(first: 100) { nodes { name } }", "linkedBranches(first: 1) { nodes { ref { name } } }")
                        .contains("pageInfo { hasNextPage endCursor }"));
    }

    @Test
    void shouldReadEveryPageOfIssuesTheSlurpedAnswerHolds() throws IOException {
        Path gh = ghPrinting(root, pagesOf(pageOf(issueJson(2, "Second", "OPEN", "")), pageOf(issueJson(1, "First", "OPEN", ""))));

        assertThat(repositoryOver(gh).entries(projectAt(root))).extracting(entry -> entry.id().value()).containsExactly("2", "1");
    }

    @Test
    void shouldReadNothingMoreThanTheThousandNewestIssues() throws IOException {
        Path gh = ghPrinting(
                root,
                pagesOf(
                        pageOf(IntStream.rangeClosed(1, PAGE).mapToObj(number -> issueJson(number, "t", "OPEN", "")).toArray(String[]::new)),
                        pageOf(IntStream.rangeClosed(PAGE + 1, OVER_CAP).mapToObj(number -> issueJson(number, "t", "OPEN", "")).toArray(String[]::new))));

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .hasSize(CAP)
                .extracting(entry -> entry.id().value())
                .doesNotContain(String.valueOf(OVER_CAP));
    }

    @Test
    void shouldBranchTheTicketOnTheBranchGitHubLinksNotOnTheTitleSlug() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJsonLinkedTo(7, "The ticket's branch", "OPEN", "", "7-the-tickets-branch")));

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.branch()).isEqualTo(new TicketBranch("7-the-tickets-branch")));
    }

    @Test
    void shouldBranchTheTicketOnTheSlugWhenGitHubLinksNoBranch() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJson(7, "The ticket's branch", "OPEN", "")));

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.branch()).isEqualTo(new TicketBranch("7-the-ticket-s-branch")));
    }

    @Test
    void shouldReadAnIssueWhoseNodeHasNoLinkedBranchesFieldAsNothingLinked() throws IOException {
        Path gh = ghPrinting(root, pagesOf(pageOf("{\"number\":7,\"title\":\"Plain\",\"state\":\"OPEN\"}")));

        assertThat(repositoryOver(gh).entries(projectAt(root)))
                .singleElement()
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.branch()).isEqualTo(new TicketBranch("7-plain")));
    }

    @Test
    void shouldRefuseAnAnswerWithNoPageRatherThanReadAnEmptyBoard() throws IOException {
        Path gh = ghPrinting(root, "");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .as("gh writes one page even for a repository with no issue, so none at all is not an empty backlog")
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh api graphql wrote no page of issues");
    }

    @Test
    void shouldRefuseAnIssueWrittenAsNull() throws IOException {
        Path gh = ghPrinting(root, pagesOf(pageOf("null")));

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh api graphql wrote an issue that is not an object: null");
    }

    @Test
    void shouldSayWhatGhWroteWhenTheRepositoryIsNotInItsAnswer() throws IOException {
        Path gh = ghPrinting(root, "[{\"data\":{\"repository\":null}}]");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh api graphql wrote a page without issues");
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
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("this project has no GitHub remote, so it has no backlog");
    }

    @Test
    void shouldSayThereIsNoBacklogWhenNoRemoteIsAGitHubHostEvenThoughGhSuggestsLoggingIn() throws IOException {
        Path gh = ghFailing(
                root,
                1,
                "none of the git remotes configured for this repository point to a known GitHub host. To tell gh about a new GitHub host, please use `gh auth login`");

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .hasMessage("this project has no GitHub remote, so it has no backlog");
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
                .hasMessageStartingWith("gh api graphql does not parse: ");
    }

    @Test
    void shouldNameTheFieldAnIssueIsWithout() throws IOException {
        Path gh = ghPrinting(root, pagesOf(pageOf("{\"title\":\"t\",\"state\":\"OPEN\"}")));

        assertThatThrownBy(() -> repositoryOver(gh).entries(projectAt(root)))
                .isExactlyInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh api graphql wrote an issue without number");
    }

    @Test
    void shouldKeepNothingBetweenReads() throws IOException {
        Path gh = ghPrinting(root, listOfIssues(issueJson(1, "t", "OPEN", "")));
        IssuesRepository repository = repositoryOver(gh);

        assertThat(repository.entries(projectAt(root))).isEqualTo(repository.entries(projectAt(root)));
    }
}
