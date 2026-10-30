package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.TicketTemplateFixture.templateSections;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.captureRepositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.captureRepositoryOverMissingGh;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.createdIssueAddress;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghFailing;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghHanging;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghPrinting;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.ghRecordingItsArguments;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.impatientCaptureRepositoryOver;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.issueAddressWithoutANumber;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.missingCommand;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.FakeGhFixture.projectAt;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.TicketCaptureRefusedException;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketId;

class TicketCaptureRepositoryTest {

    @TempDir
    Path root;

    @Test
    void shouldAnswerTheNumberInTheAddressGhPrints() throws IOException {
        Path gh = ghPrinting(root, createdIssueAddress());

        assertThat(captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody())).isEqualTo(new TicketId("12"));
    }

    @Test
    void shouldCreateTheIssueWithTheTitleAndTheBodyAndNoLabel() throws IOException {
        Path record = root.resolve("arguments");
        Path gh = ghRecordingItsArguments(root, record, createdIssueAddress());

        captureRepositoryOver(gh).create(projectAt(root), title(), new Body(templateSections()));

        assertThat(Files.readString(record))
                .isEqualTo("issue\ncreate\n--title\n%s\n--body\n%s\n".formatted(title().value(), templateSections()));
    }

    @Test
    void shouldCreateTheIssueWithAnEmptyBodyWhenThereIsNone() throws IOException {
        Path record = root.resolve("arguments");
        Path gh = ghRecordingItsArguments(root, record, createdIssueAddress());

        captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody());

        assertThat(Files.readString(record)).isEqualTo("issue\ncreate\n--title\n%s\n--body\n\n".formatted(title().value()));
    }

    @Test
    void shouldRefuseWhenGhPrintsNoIssueAddress() throws IOException {
        Path gh = ghPrinting(root, "created");

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessage("gh issue create printed no issue number: created");
    }

    @Test
    void shouldRefuseWhenTheAddressGhPrintsEndsInNoIssueNumber() throws IOException {
        Path gh = ghPrinting(root, issueAddressWithoutANumber());

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessage("gh issue create printed no issue number: " + issueAddressWithoutANumber());
    }

    @Test
    void shouldSayThereIsNoRemoteWhenGhFindsNoGitHubRemote() throws IOException {
        Path gh = ghFailing(root, 1, "no git remotes found");

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessage("this project has no GitHub remote, so it has no backlog");
    }

    @Test
    void shouldNameGhAuthLoginWhenGhIsNotLoggedIn() throws IOException {
        Path gh = ghFailing(root, 4, "To get started with GitHub CLI, please run:  gh auth login");

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessage("gh is not logged in; run gh auth login");
    }

    @Test
    void shouldGiveGhsOwnWordsWhenGitHubCannotBeReached() throws IOException {
        Path gh = ghFailing(root, 1, "error connecting to api.github.com");

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessage("cannot create the ticket on GitHub: error connecting to api.github.com");
    }

    @Test
    void shouldGiveGitHubsRefusalInItsOwnWords() throws IOException {
        Path gh = ghFailing(root, 1, "GraphQL: Resource not accessible by personal access token");

        assertThatThrownBy(() -> captureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .hasMessage("cannot create the ticket on GitHub: GraphQL: Resource not accessible by personal access token");
    }

    @Test
    void shouldSayWhyWhenGhIsNotInstalled() {
        assertThatThrownBy(() -> captureRepositoryOverMissingGh().create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessageContaining("cannot run " + missingCommand());
    }

    @Test
    void shouldGiveUpOnAGhThatDoesNotAnswerInTime() throws IOException {
        Path gh = ghHanging(root);

        assertThatThrownBy(() -> impatientCaptureRepositoryOver(gh).create(projectAt(root), title(), new NoBody()))
                .isExactlyInstanceOf(TicketCaptureRefusedException.class)
                .hasMessageContaining("did not answer within 0 s");
    }
}
