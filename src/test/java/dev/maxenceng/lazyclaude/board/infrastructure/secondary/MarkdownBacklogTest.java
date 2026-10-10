package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.Status;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class MarkdownBacklogTest {

    @TempDir
    Path root;

    private Path backlog;

    @BeforeEach
    void createBacklog() throws IOException {
        backlog = Files.createDirectories(root.resolve("docs/backlog"));
    }

    private void file(String name, String content) throws IOException {
        Files.writeString(backlog.resolve(name), content);
    }

    private void ticket(String name, String id, String status, String title) throws IOException {
        file(name, "---\nid: " + id + "\nstatus: " + status + "\ncontext: board\ntype: feature\n---\n\n# " + title
                + "\n\nBody.\n");
    }

    private Board read() {
        return new MarkdownBacklog().read(new Project(root));
    }

    @Test
    void shouldReadATicketsFrontmatterAndTitle() throws IOException {
        ticket("PROJ-1-board.md", "PROJ-1", "in-progress", "A board from the backlog");

        Ticket ticket = read().tickets(Status.IN_PROGRESS).getFirst();

        assertThat(ticket.id().value()).isEqualTo("PROJ-1");
        assertThat(ticket.title()).isEqualTo("A board from the backlog");
        assertThat(ticket.context()).isEqualTo("board");
        assertThat(ticket.type()).isEqualTo("feature");
    }

    @Test
    void shouldSkipFilesWhoseNameStartsWithAnUnderscore() throws IOException {
        ticket("_template.md", "PROJ-9", "draft", "Template");

        assertThat(read().tickets(Status.DRAFT)).isEmpty();
        assertThat(read().unreadable()).isEmpty();
    }

    @Test
    void shouldSkipAFileWithFrontmatterButNoId() throws IOException {
        file("ROADMAP.md", "---\ntitle: Roadmap\n---\n\n# Roadmap\n");

        assertThat(read().unreadable()).isEmpty();
    }

    @Test
    void shouldSkipAFileWithNoFrontmatter() throws IOException {
        file("README.md", "# Backlog\n\nSee tickets.\n");

        assertThat(read().unreadable()).isEmpty();
        for (Status status : Status.values()) {
            assertThat(read().tickets(status)).isEmpty();
        }
    }

    @Test
    void shouldSkipFilesThatAreNotMarkdown() throws IOException {
        file("notes.txt", "---\nid: PROJ-3\nstatus: draft\n---\n");

        assertThat(read().tickets(Status.DRAFT)).isEmpty();
        assertThat(read().unreadable()).isEmpty();
    }

    @Test
    void shouldShowTheIdAsTitleWhenThereIsNoHeading() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n\nJust prose.\n");

        assertThat(read().tickets(Status.DRAFT)).singleElement().satisfies(ticket -> assertThat(ticket.title())
                .isEqualTo("PROJ-1"));
        assertThat(read().unreadable()).isEmpty();
    }

    @Test
    void shouldNotTakeAHeadingInsideACodeFenceForTheTitle() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n\n```\n# not a title\n```\n");

        assertThat(read().tickets(Status.DRAFT).getFirst().title()).isEqualTo("PROJ-1");
    }

    @Test
    void shouldListAFileWithAnUnknownStatusAsUnreadableAndStillLoadTheOthers() throws IOException {
        ticket("PROJ-1.md", "PROJ-1", "todo", "Fine");
        ticket("PROJ-2.md", "PROJ-2", "blocked", "Odd");

        Board board = read();

        assertThat(board.tickets(Status.TODO)).hasSize(1);
        assertThat(board.unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo("PROJ-2.md");
            assertThat(unreadable.reason()).contains("blocked");
        });
    }

    @Test
    void shouldListAFileWithAnIdAndBrokenFrontmatterAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: [todo\n---\n\n# Broken\n");
        ticket("PROJ-2.md", "PROJ-2", "todo", "Fine");

        Board board = read();

        assertThat(board.unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo("PROJ-1.md");
            assertThat(unreadable.reason()).isNotBlank();
        });
        assertThat(board.tickets(Status.TODO)).hasSize(1);
    }

    @Test
    void shouldListAFileWhoseFrontmatterNeverEndsAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: todo\n\n# No closing line\n");

        assertThat(read().unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason())
                .contains("closing"));
    }

    @Test
    void shouldSkipBrokenFrontmatterThatHasNoId() throws IOException {
        file("NOTES.md", "---\ntitle: [oops\n---\n");

        assertThat(read().unreadable()).isEmpty();
    }

    @Test
    void shouldListAnIdThatIsNotATicketIdAsUnreadable() throws IOException {
        ticket("odd.md", "odd", "todo", "Odd");

        assertThat(read().unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason())
                .contains("odd"));
    }

    @Test
    void shouldListAFileWithAnIdAndNoStatusAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\n---\n\n# No status\n");

        assertThat(read().unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason())
                .contains("status"));
    }

    @Test
    void shouldListTheSecondFileClaimingAnIdAsUnreadable() throws IOException {
        ticket("PROJ-1-a.md", "PROJ-1", "todo", "First");
        ticket("PROJ-1-b.md", "PROJ-1", "done", "Second");

        Board board = read();

        assertThat(board.tickets(Status.TODO)).hasSize(1);
        assertThat(board.tickets(Status.DONE)).isEmpty();
        assertThat(board.unreadable())
                .extracting(UnreadableTicket::file)
                .as("one id is one ticket, so selection by id stays unambiguous")
                .containsExactly("PROJ-1-b.md");
    }

    @Test
    void shouldListANumericIdAsUnreadable() throws IOException {
        file("odd.md", "---\nid: 12\nstatus: todo\n---\n");

        assertThat(read().unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason())
                .contains("12"));
    }

    @Test
    void shouldOrderTicketsNumericallyWithinAGroup() throws IOException {
        ticket("PROJ-10.md", "PROJ-10", "draft", "Ten");
        ticket("PROJ-2.md", "PROJ-2", "draft", "Two");

        assertThat(read().tickets(Status.DRAFT))
                .extracting(ticket -> ticket.id().value())
                .containsExactly("PROJ-2", "PROJ-10");
    }

    @Test
    void shouldListAFileWithAnIdThatIsNotValidUtf8AsUnreadable() throws IOException {
        Files.write(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n# \u00ff\n"
                .getBytes(StandardCharsets.ISO_8859_1));

        assertThat(read().unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo("PROJ-1.md");
            assertThat(unreadable.reason()).contains("not valid UTF-8");
        });
    }

    @Test
    void shouldSkipAFileWithNoIdThatIsNotValidUtf8() throws IOException {
        Files.write(backlog.resolve("NOTES.md"), new byte[] {'-', '-', '-', '\n', (byte) 0xff, (byte) 0xfe, '\n'});

        assertThat(read().unreadable()).as("a file with no id never appears, readable or not").isEmpty();
    }

    @Test
    void shouldReadATicketSavedWithAByteOrderMark() throws IOException {
        file("PROJ-3.md", "\uFEFF---\nid: PROJ-3\nstatus: todo\n---\n# Marked\n");

        assertThat(read().tickets(Status.TODO))
                .extracting(Ticket::title)
                .as("a byte-order mark is an encoding detail, not content")
                .containsExactly("Marked");
    }

    @Test
    void shouldSkipBrokenFrontmatterWhoseIdIsEmpty() throws IOException {
        file("NOTES.md", "---\nid:\ntitle: [oops\n---\n");

        assertThat(read().unreadable()).as("an empty id is no id").isEmpty();
    }

    @Test
    void shouldReadAnEmptyBacklogAsAnEmptyBoard() {
        assertThat(read().unreadable()).isEmpty();
        assertThat(read().tickets(Status.DRAFT)).isEmpty();
    }
}
