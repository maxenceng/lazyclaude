package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.backlogIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.secondTicketMarkdownWithStatus;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.ticketMarkdown;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BoardWatchFixture.watching;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.branchAtHead;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.checkoutNewBranch;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.commitEverything;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.deleteBranch;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktreeOn;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.packRefs;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.removeLinkedWorktree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.worktreeOnExistingBranch;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.pollInterval;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.settled;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcher;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.WatchFallback;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ChangeProbe;

@ExtendWith(MockitoExtension.class)
class BoardWatchRepositoryTest {

    private static final String TICKET_BRANCH = "PROJ-1-ticket";

    @Mock
    private WatchFallback fallback;

    private final ChangeProbe probe = new ChangeProbe();
    private BoardWatchRepository watcher;

    @TempDir
    Path temp;

    private Path root;

    @BeforeEach
    void createRepository() throws Exception {
        root = repositoryWithBacklog(temp.resolve("app"));
    }

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldReportASavedTicketFileInTheRootBacklog() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(root).resolve("PROJ-1.md"), ticketMarkdown().replace("todo", "in-progress"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportOnceForTheSeveralEventsOfOneSave() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(root).resolve("PROJ-1.md"), ticketMarkdown().replace("todo", "in-progress"));
        Files.writeString(backlogIn(root).resolve("PROJ-1.md"), ticketMarkdown().replace("todo", "in-review"));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportAnAddedTicketFile() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(root).resolve("PROJ-2.md"), secondTicketMarkdownWithStatus("todo"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportARemovedTicketFile() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.delete(backlogIn(root).resolve("PROJ-1.md"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldIgnoreAFileInTheRootBacklogThatIsNotAMarkdownTicketFile() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(root).resolve(".PROJ-1.md.swp"), "x");
        Files.writeString(backlogIn(root).resolve("_template.md"), "x");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportAnEditToTheTicketsOwnFileInAWorktree() throws Exception {
        Path worktree = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve(TICKET_BRANCH), TICKET_BRANCH);
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(worktree).resolve("%s.md".formatted(TICKET_BRANCH)), ticketMarkdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldIgnoreAnotherFileInAWorktreesBacklog() throws Exception {
        Path worktree = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve(TICKET_BRANCH), TICKET_BRANCH);
        watcher = watching(watcher(), root, probe, fallback);

        Files.writeString(backlogIn(worktree).resolve("PROJ-1.md"), ticketMarkdown().replace("todo", "in-progress"));

        assertThat(probe.quietFor(settled()))
                .as("another ticket's file in a worktree is that ticket's business, not this ticket's branch copy")
                .isTrue();
    }

    @Test
    void shouldReportACommitOnATicketBranch() throws Exception {
        Path worktree = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve(TICKET_BRANCH), TICKET_BRANCH);
        watcher = watching(watcher(), root, probe, fallback);

        commitEverything(worktree, "work on the ticket");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportACheckout() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        checkoutNewBranch(root, TICKET_BRANCH);

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportABranchCreated() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        branchAtHead(root, TICKET_BRANCH);

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportABranchDeleted() throws Exception {
        branchAtHead(root, TICKET_BRANCH);
        watcher = watching(watcher(), root, probe, fallback);

        deleteBranch(root, TICKET_BRANCH);

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportABranchWithASlashInItsNameCreated() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        branchAtHead(root, "feature/PROJ-1");
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        commitEverything(root, "on main");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportABranchDeletedThatOnlyPackedRefsHolds() throws Exception {
        branchAtHead(root, TICKET_BRANCH);
        packRefs(root);
        watcher = watching(watcher(), root, probe, fallback);

        deleteBranch(root, TICKET_BRANCH);

        assertThat(probe.changedWithin(within()))
                .as("no loose ref is touched: the branch lives in packed-refs, which git rewrites")
                .isTrue();
    }

    @Test
    void shouldWatchAWorktreeAddedAfterItStarted() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);
        Path worktree = linkedWorktreeOn(root, temp.resolve("app.worktrees").resolve(TICKET_BRANCH), TICKET_BRANCH);
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();

        Files.writeString(backlogIn(worktree).resolve("%s.md".formatted(TICKET_BRANCH)), ticketMarkdown());

        assertThat(probe.changedWithin(within())).as("the worktree's own ticket file is watched without a restart").isTrue();
    }

    @Test
    void shouldWatchTheNewDirectoryWhenAWorktreeRemovedIsAddedAgainAtTheSamePath() throws Exception {
        Path at = temp.resolve("app.worktrees").resolve(TICKET_BRANCH);
        Path worktree = linkedWorktreeOn(root, at, TICKET_BRANCH);
        watcher = watching(watcher(), root, probe, fallback);
        removeLinkedWorktree(root, worktree);
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        Path again = worktreeOnExistingBranch(root, at, TICKET_BRANCH);
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();

        Files.writeString(backlogIn(again).resolve("%s.md".formatted(TICKET_BRANCH)), ticketMarkdown());

        assertThat(probe.changedWithin(within()))
                .as("a watch kept on the removed directory would name an inode that is gone, and miss this save")
                .isTrue();
    }

    @Test
    void shouldSayNothingWhenTheWatchIsSetUp() {
        watcher = watching(watcher(), root, probe, fallback);

        verifyNoInteractions(fallback);
    }

    @Test
    void shouldTellTheFallbackOnceForEachTreeThatCannotBeWatchedNamingTheSysctl() {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback, times(2)).polling(cause.capture(), eq(pollInterval()));
        assertThat(cause.getAllValues())
                .extracting(Reason::value)
                .anySatisfy(value -> assertThat(value).contains(backlogIn(root).toString(), "fs.inotify.max_user_watches"))
                .anySatisfy(value -> assertThat(value).contains(root.resolve(".git").toString()));
    }

    @Test
    void shouldPollForAChangeToATicketFileWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        Files.writeString(backlogIn(root).resolve("PROJ-2.md"), secondTicketMarkdownWithStatus("todo"));

        assertThat(probe.changedWithin(within())).isTrue();
    }
}
