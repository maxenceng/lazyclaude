package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterFixture.datedFrontmatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.bareTicketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.backlogIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.secondTicketMarkdownWithStatus;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.ticketMarkdown;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.secondFileName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BacklogRepositoryTest {

    private final BacklogRepository repository = new BacklogRepository();

    @TempDir
    Path root;

    private Path backlog;

    @BeforeEach
    void createBacklog() throws IOException {
        backlog = backlogIn(root);
    }

    @Test
    void shouldReadATicketsFrontmatterAndTitle() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), ticketMarkdown());

        assertThat(repository.entries(new Project(root))).containsExactly(ticketFile());
    }

    @Test
    void shouldSkipFilesWhoseNameStartsWithAnUnderscore() throws IOException {
        Files.writeString(backlog.resolve("_template.md"), ticketMarkdown());

        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldSkipAFileWithFrontmatterButNoId() throws IOException {
        Files.writeString(backlog.resolve("ROADMAP.md"), "---\ntitle: Roadmap\n---\n\n# Roadmap\n");

        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldSkipAFileWithNoFrontmatter() throws IOException {
        Files.writeString(backlog.resolve("README.md"), "# Backlog\n\nSee tickets.\n");

        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldSkipFilesThatAreNotMarkdown() throws IOException {
        Files.writeString(backlog.resolve("notes.txt"), ticketMarkdown());

        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldReadATicketWithNoHeadingAsHavingNoTitle() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\ncontext: board\ntype: feature\n---\n\nJust prose.\n");

        assertThat(repository.entries(new Project(root)))
                .as("a ticket without a heading is still a ticket, not an unreadable one")
                .containsExactly(TicketFile.builder()
                        .file(new FileName("PROJ-1.md"))
                        .ticket(ticketBuilder().title(new NoTitle()).body(new Body("\nJust prose.")).build())
                        .build());
    }

    @Test
    void shouldNotTakeAHeadingInsideACodeFenceForTheTitle() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n\n```\n# not a title\n```\n");

        assertThat(repository.entries(new Project(root))).containsExactly(TicketFile.builder()
                .file(new FileName("PROJ-1.md"))
                .ticket(bareTicketBuilder().body(new Body("\n```\n# not a title\n```")).build())
                .build());
    }

    @Test
    void shouldReadAnEmptyHeadingAsNoTitle() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n# \n# Later\n");

        assertThat(repository.entries(new Project(root)))
                .as("the first heading is the title, even when it is empty")
                .containsExactly(TicketFile.builder().file(new FileName("PROJ-1.md")).ticket(bareTicketBuilder().body(new Body("# \n# Later")).build()).build());
    }

    @Test
    void shouldReadAMissingOrBlankContextAndTypeAsAbsent() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\ncontext: ' '\n---\n");

        assertThat(repository.entries(new Project(root)))
                .containsExactly(TicketFile.builder()
                        .file(new FileName("PROJ-1.md"))
                        .ticket(bareTicketBuilder()
                                .build())
                        .build());
    }

    @Test
    void shouldReadATicketSavedWithAByteOrderMark() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "﻿" + ticketMarkdown());

        assertThat(repository.entries(new Project(root)))
                .as("a byte-order mark is an encoding detail, not content")
                .containsExactly(ticketFile());
    }

    @Test
    void shouldListAFileWithAnUnknownStatusAsUnreadableAndStillLoadTheOthers() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), ticketMarkdown());
        Files.writeString(backlog.resolve("PROJ-2.md"), secondTicketMarkdownWithStatus("blocked"));

        assertThat(repository.entries(new Project(root))).containsExactly(
                ticketFile(),
                UnreadableTicket.builder()
                        .file(secondFileName())
                        .reason(new Reason("status 'blocked' is not one of draft, todo, in-progress, in-review, done"))
                        .build());
    }

    @Test
    void shouldListAFileWithAnIdAndNoStatusAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("PROJ-2.md"), "---\nid: PROJ-2\n---\n\n# No status\n");

        assertThat(repository.entries(new Project(root))).containsExactly(UnreadableTicket.builder()
                .file(secondFileName())
                .reason(new Reason("status is mandatory and was not set"))
                .build());
    }

    @Test
    void shouldListAnIdThatIsNotATicketIdAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("odd.md"), "---\nid: odd\nstatus: todo\n---\n");

        assertThat(repository.entries(new Project(root))).containsExactly(UnreadableTicket.builder()
                .file(new FileName("odd.md"))
                .reason(new Reason("id 'odd' is not PREFIX-number"))
                .build());
    }

    @Test
    void shouldListANumericIdAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("odd.md"), "---\nid: 12\nstatus: todo\n---\n");

        assertThat(repository.entries(new Project(root))).containsExactly(UnreadableTicket.builder()
                .file(new FileName("odd.md"))
                .reason(new Reason("id '12' is not PREFIX-number"))
                .build());
    }

    @Test
    void shouldListAFileWithAnIdAndBrokenFrontmatterAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("PROJ-2.md"), "---\nid: PROJ-2\nstatus: [todo\n---\n\n# Broken\n");

        assertThat(repository.entries(new Project(root))).singleElement().isInstanceOfSatisfying(
                UnreadableTicket.class,
                unreadable -> assertThat(unreadable.reason().value()).startsWith("frontmatter is not valid YAML: "));
    }

    @Test
    void shouldListAFileThatWritesAKeyTwiceAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("PROJ-2.md"), "---\nid: PROJ-2\nstatus: todo\nstatus: done\n---\n\n# Twice\n");

        assertThat(repository.entries(new Project(root))).singleElement().isInstanceOfSatisfying(
                UnreadableTicket.class,
                unreadable -> assertThat(unreadable.reason().value()).startsWith("frontmatter is not valid YAML: "));
    }

    @Test
    void shouldListAFileWhoseFrontmatterNeverEndsAsUnreadable() throws IOException {
        Files.writeString(backlog.resolve("PROJ-2.md"), "---\nid: PROJ-2\nstatus: todo\n\n# No closing line\n");

        assertThat(repository.entries(new Project(root))).containsExactly(UnreadableTicket.builder()
                .file(secondFileName())
                .reason(new Reason("frontmatter has no closing ---"))
                .build());
    }

    @Test
    void shouldListAFileWithAnIdThatIsNotValidUtf8AsUnreadable() throws IOException {
        Files.write(backlog.resolve("PROJ-2.md"), "---\nid: PROJ-2\nstatus: todo\n---\n# ÿ\n".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(repository.entries(new Project(root))).containsExactly(UnreadableTicket.builder()
                .file(secondFileName())
                .reason(new Reason("cannot be read: not valid UTF-8"))
                .build());
    }

    @Test
    void shouldSkipBrokenFrontmatterThatHasNoId() throws IOException {
        Files.writeString(backlog.resolve("NOTES.md"), "---\ntitle: [oops\n---\n");

        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldSkipBrokenFrontmatterWhoseIdIsEmpty() throws IOException {
        Files.writeString(backlog.resolve("NOTES.md"), "---\nid:\ntitle: [oops\n---\n");

        assertThat(repository.entries(new Project(root))).as("an empty id is no id").isEmpty();
    }

    @Test
    void shouldSkipAFileWithNoIdThatIsNotValidUtf8() throws IOException {
        Files.write(backlog.resolve("NOTES.md"), new byte[] {'-', '-', '-', '\n', (byte) 0xff, (byte) 0xfe, '\n'});

        assertThat(repository.entries(new Project(root))).as("a file with no id never appears, readable or not").isEmpty();
    }

    @Test
    void shouldReportEveryFileClaimingAnIdInFileNameOrder() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1-b.md"), ticketMarkdown());
        Files.writeString(backlog.resolve("PROJ-1-a.md"), ticketMarkdown());

        assertThat(repository.entries(new Project(root)))
                .extracting(BacklogEntry::file)
                .as("file order decides which claim on an id the board keeps")
                .containsExactly(new FileName("PROJ-1-a.md"), new FileName("PROJ-1-b.md"));
    }

    @Test
    void shouldReadAnEmptyBacklogAsNoEntries() {
        assertThat(repository.entries(new Project(root))).isEmpty();
    }

    @Test
    void shouldSayWhichBacklogIsMissing() throws IOException {
        Files.delete(backlog);

        assertThatThrownBy(() -> repository.entries(new Project(root)))
                .isExactlyInstanceOf(BacklogNotFoundException.class)
                .hasMessage("the main worktree has no %s".formatted(backlog));
    }

    @Test
    void shouldStripTheHeadingItReadsAsTheTitle() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n#   Spaced out  \n");

        assertThat(repository.entries(new Project(root))).singleElement().isInstanceOfSatisfying(
                TicketFile.class, file -> assertThat(file.ticket().title()).isEqualTo(new Title("Spaced out")));
    }

    @Test
    void shouldKeepTheOtherFrontmatterKeysInFileOrderAsDisplayText() throws IOException {
        Files.writeString(
                backlog.resolve("PROJ-1.md"),
                "---\nid: PROJ-1\nstatus: todo\ncontext: board\ntype: feature\ncreated: 2026-10-10\nblocked_by: [PROJ-8, PROJ-9]\n---\n\n# A board from the backlog\n\nBody.\n");

        assertThat(repository.entries(new Project(root)))
                .singleElement()
                .isInstanceOfSatisfying(TicketFile.class, file -> assertThat(file.ticket().frontmatter())
                        .as("the fields the board models are left out, a date stays as written and a list reads as [A, B]")
                        .isEqualTo(datedFrontmatter()));
    }

    @Test
    void shouldKeepTheBodyAfterTheClosingDelimiter() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\nFirst\n\nSecond\n");

        assertThat(repository.entries(new Project(root)))
                .singleElement()
                .isInstanceOfSatisfying(
                        TicketFile.class, file -> assertThat(file.ticket().body()).isEqualTo(new Body("First\n\nSecond")));
    }

    @Test
    void shouldKeepADelimiterInsideTheBodyInTheBody() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n\nabove\n\n---\n\nbelow: this\n");

        assertThat(repository.entries(new Project(root)))
                .singleElement()
                .isInstanceOfSatisfying(TicketFile.class, file -> assertThat(file.ticket().body())
                        .as("only the first closing delimiter ends the frontmatter")
                        .isEqualTo(new Body("\nabove\n\n---\n\nbelow: this")));
    }

    @Test
    void shouldReadAnEmptyBodyAsNoBody() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n");

        assertThat(repository.entries(new Project(root)))
                .singleElement()
                .isInstanceOfSatisfying(
                        TicketFile.class, file -> assertThat(file.ticket().body()).isEqualTo(new NoBody()));
    }

    @Test
    void shouldReadAWhitespaceOnlyBodyAsNoBody() throws IOException {
        Files.writeString(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n\n  \n\t\n");

        assertThat(repository.entries(new Project(root)))
                .singleElement()
                .isInstanceOfSatisfying(
                        TicketFile.class, file -> assertThat(file.ticket().body()).isEqualTo(new NoBody()));
    }
}
