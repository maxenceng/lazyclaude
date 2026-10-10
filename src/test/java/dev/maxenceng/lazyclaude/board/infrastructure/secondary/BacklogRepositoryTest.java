package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BacklogRepositoryTest {

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

    private List<BacklogEntry> read() {
        return new BacklogRepository().entries(new Project(root));
    }

    private List<Ticket> tickets() {
        return read().stream()
                .flatMap(entry -> entry instanceof TicketFile file ? Stream.of(file.ticket()) : Stream.empty())
                .toList();
    }

    private List<UnreadableTicket> unreadable() {
        return read().stream()
                .flatMap(entry -> entry instanceof UnreadableTicket refused ? Stream.of(refused) : Stream.empty())
                .toList();
    }

    @Test
    void shouldReadATicketsFrontmatterAndTitle() throws IOException {
        ticket("PROJ-1-board.md", "PROJ-1", "in-progress", "A board from the backlog");

        Ticket ticket = tickets().getFirst();

        assertThat(ticket.id().value()).isEqualTo("PROJ-1");
        assertThat(ticket.status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(ticket.title()).isEqualTo(new Title("A board from the backlog"));
        assertThat(ticket.context()).isEqualTo(new ContextName("board"));
        assertThat(ticket.type()).isEqualTo(new TypeName("feature"));
    }

    @Test
    void shouldSkipFilesWhoseNameStartsWithAnUnderscore() throws IOException {
        ticket("_template.md", "PROJ-9", "draft", "Template");

        assertThat(read()).isEmpty();
    }

    @Test
    void shouldSkipAFileWithFrontmatterButNoId() throws IOException {
        file("ROADMAP.md", "---\ntitle: Roadmap\n---\n\n# Roadmap\n");

        assertThat(unreadable()).isEmpty();
    }

    @Test
    void shouldSkipAFileWithNoFrontmatter() throws IOException {
        file("README.md", "# Backlog\n\nSee tickets.\n");

        assertThat(read()).isEmpty();
    }

    @Test
    void shouldSkipFilesThatAreNotMarkdown() throws IOException {
        file("notes.txt", "---\nid: PROJ-3\nstatus: draft\n---\n");

        assertThat(read()).isEmpty();
    }

    @Test
    void shouldReadATicketWithNoHeadingAsHavingNoTitle() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n\nJust prose.\n");

        assertThat(tickets()).singleElement().satisfies(ticket -> assertThat(ticket.title()).isEqualTo(new NoTitle()));
        assertThat(unreadable()).isEmpty();
    }

    @Test
    void shouldNotTakeAHeadingInsideACodeFenceForTheTitle() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n\n```\n# not a title\n```\n");

        assertThat(tickets().getFirst().title()).isEqualTo(new NoTitle());
    }

    @Test
    void shouldListAFileWithAnUnknownStatusAsUnreadableAndStillLoadTheOthers() throws IOException {
        ticket("PROJ-1.md", "PROJ-1", "todo", "Fine");
        ticket("PROJ-2.md", "PROJ-2", "blocked", "Odd");

        assertThat(tickets()).hasSize(1);
        assertThat(unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo(new FileName("PROJ-2.md"));
            assertThat(unreadable.reason().value()).contains("blocked");
        });
    }

    @Test
    void shouldListAFileWithAnIdAndBrokenFrontmatterAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: [todo\n---\n\n# Broken\n");
        ticket("PROJ-2.md", "PROJ-2", "todo", "Fine");

        assertThat(unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo(new FileName("PROJ-1.md"));
            assertThat(unreadable.reason().value()).isNotBlank();
        });
        assertThat(tickets()).hasSize(1);
    }

    @Test
    void shouldListAFileWhoseFrontmatterNeverEndsAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: todo\n\n# No closing line\n");

        assertThat(unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason().value())
                .contains("closing"));
    }

    @Test
    void shouldSkipBrokenFrontmatterThatHasNoId() throws IOException {
        file("NOTES.md", "---\ntitle: [oops\n---\n");

        assertThat(unreadable()).isEmpty();
    }

    @Test
    void shouldListAnIdThatIsNotATicketIdAsUnreadable() throws IOException {
        ticket("odd.md", "odd", "todo", "Odd");

        assertThat(unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason().value())
                .contains("odd"));
    }

    @Test
    void shouldListAFileWithAnIdAndNoStatusAsUnreadable() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\n---\n\n# No status\n");

        assertThat(unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason().value())
                .contains("status"));
    }

    @Test
    void shouldReportEveryFileClaimingAnIdInFileNameOrder() throws IOException {
        ticket("PROJ-1-b.md", "PROJ-1", "done", "Second");
        ticket("PROJ-1-a.md", "PROJ-1", "todo", "First");

        assertThat(read())
                .extracting(BacklogEntry::file)
                .as("file order decides which claim on an id the board keeps")
                .containsExactly(new FileName("PROJ-1-a.md"), new FileName("PROJ-1-b.md"));
    }

    @Test
    void shouldListANumericIdAsUnreadable() throws IOException {
        file("odd.md", "---\nid: 12\nstatus: todo\n---\n");

        assertThat(unreadable()).singleElement().satisfies(unreadable -> assertThat(unreadable.reason().value())
                .contains("12"));
    }

    @Test
    void shouldListAFileWithAnIdThatIsNotValidUtf8AsUnreadable() throws IOException {
        Files.write(backlog.resolve("PROJ-1.md"), "---\nid: PROJ-1\nstatus: todo\n---\n# \u00ff\n"
                .getBytes(StandardCharsets.ISO_8859_1));

        assertThat(unreadable()).singleElement().satisfies(unreadable -> {
            assertThat(unreadable.file()).isEqualTo(new FileName("PROJ-1.md"));
            assertThat(unreadable.reason().value()).contains("not valid UTF-8");
        });
    }

    @Test
    void shouldSkipAFileWithNoIdThatIsNotValidUtf8() throws IOException {
        Files.write(backlog.resolve("NOTES.md"), new byte[] {'-', '-', '-', '\n', (byte) 0xff, (byte) 0xfe, '\n'});

        assertThat(unreadable()).as("a file with no id never appears, readable or not").isEmpty();
    }

    @Test
    void shouldReadATicketSavedWithAByteOrderMark() throws IOException {
        file("PROJ-3.md", "\uFEFF---\nid: PROJ-3\nstatus: todo\n---\n# Marked\n");

        assertThat(tickets())
                .extracting(Ticket::title)
                .as("a byte-order mark is an encoding detail, not content")
                .containsExactly(new Title("Marked"));
    }

    @Test
    void shouldSkipBrokenFrontmatterWhoseIdIsEmpty() throws IOException {
        file("NOTES.md", "---\nid:\ntitle: [oops\n---\n");

        assertThat(unreadable()).as("an empty id is no id").isEmpty();
    }

    @Test
    void shouldReadAnEmptyBacklogAsNoEntries() {
        assertThat(read()).isEmpty();
    }

    @Test
    void shouldSayWhichBacklogIsMissing() throws IOException {
        Files.delete(backlog);

        assertThatThrownBy(this::read)
                .isInstanceOf(BacklogNotFoundException.class)
                .hasMessage("the main worktree has no %s".formatted(backlog));
    }

    @Test
    void shouldReadAMissingContextAndTypeAsAbsent() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n# Bare\n");

        assertThat(tickets()).singleElement().satisfies(ticket -> {
            assertThat(ticket.context()).isEqualTo(new NoContext());
            assertThat(ticket.type()).isEqualTo(new NoType());
        });
    }

    @Test
    void shouldReadABlankContextAndTypeAsAbsent() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\ncontext: ' '\ntype: ''\n---\n");

        assertThat(tickets()).singleElement().satisfies(ticket -> {
            assertThat(ticket.context()).isEqualTo(new NoContext());
            assertThat(ticket.type()).isEqualTo(new NoType());
        });
    }

    @Test
    void shouldReadAnEmptyHeadingAsNoTitle() throws IOException {
        file("PROJ-1.md", "---\nid: PROJ-1\nstatus: draft\n---\n# \n# Later\n");

        assertThat(tickets()).singleElement().satisfies(ticket -> assertThat(ticket.title())
                .as("the first heading is the title, even when it is empty")
                .isEqualTo(new NoTitle()));
    }
}
