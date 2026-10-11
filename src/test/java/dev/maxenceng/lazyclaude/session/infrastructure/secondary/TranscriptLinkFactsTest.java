package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.onBranches;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ranOnBranch;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedInProject;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.append;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.branchLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.branchLineWithoutTimestamp;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.commandLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.cwd;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.folder;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.later;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.latest;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.lineWithoutCwd;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.otherSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.scanned;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.skillLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.skillLineWithoutArgs;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.started;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.userLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.repositoryOver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.LinkFacts;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.TranscriptLocator;
import dev.maxenceng.lazyclaude.session.published.SessionId;

@ExtendWith(MockitoExtension.class)
class TranscriptLinkFactsTest {

    @Mock
    private TranscriptLocator locator;

    @Mock
    private CacheFallback unwritable;

    @Mock
    private RootFallback unlistable;

    @TempDir
    Path root;

    private Path projects;
    private Path cacheFile;

    @BeforeEach
    void setUp() {
        projects = root.resolve("projects");
        cacheFile = root.resolve("cache/lazyclaude/past-sessions.json");
        lenient().when(locator.locate(any())).thenReturn(locatedInProject());
    }

    @Test
    void shouldReadTheVerbAndIdsOfATicketCommandFromItsArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ticket", "start PROJ-1"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("start", "PROJ-1")));
    }

    @Test
    void shouldReadATicketCommandOfAPluginToo() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ddd-workflow:ticket", "review PROJ-1 PROJ-2"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("review", "PROJ-1", "PROJ-2")));
    }

    @Test
    void shouldNotReadTheArgsOfAnotherCommand() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/review", "start PROJ-1"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), LinkFacts.none());
    }

    @Test
    void shouldReadATicketSkillCallFromItsArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "ticket", "refine PROJ-3"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("refine", "PROJ-3")));
    }

    @Test
    void shouldReadAPluginsTicketSkillCallFromItsArgs() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "ddd-workflow:ticket", "done PROJ-3"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("done", "PROJ-3")));
    }

    @Test
    void shouldNotReadTheArgsOfAnotherSkill() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "superpowers:brainstorming", "Refining PROJ-3"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), LinkFacts.none());
    }

    @Test
    void shouldNotReadASkillCallWithoutArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), skillLineWithoutArgs(cwd(), started(), "ticket"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), LinkFacts.none());
    }

    @Test
    void shouldIgnoreArgsThatNameNoTicket() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                commandLine(cwd(), started(), "main", "/ticket", "new Do the thing"),
                commandLine(cwd(), later(), "main", "/ticket", "done"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .as("args without an id say nothing of a ticket, so the branch decides")
                .containsEntry(new SessionId(sessionId()), LinkFacts.none());
    }

    @Test
    void shouldSkipAWordThatIsNotAnIdAmongTheNamedOnes() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ticket", "review PROJ-1 and PROJ-22"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("review", "PROJ-1", "PROJ-22")));
    }

    @Test
    void shouldKeepTheRunsInTheOrderTheyWereRunAndTheLastOfARepeatedOne() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                commandLine(cwd(), started(), "main", "/ticket", "refine PROJ-1"),
                commandLine(cwd(), later(), "main", "/ticket", "start PROJ-1"),
                commandLine(cwd(), latest(), "main", "/ticket", "refine PROJ-1"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), ran(ticketRun("start", "PROJ-1"), ticketRun("refine", "PROJ-1")));
    }

    @Test
    void shouldReadTheTicketIdPrefixOfEachBranchOnce() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                branchLine(cwd(), started(), "PROJ-3-ticket-sessions-and-markers"),
                branchLine(cwd(), later(), "PROJ-3-ticket-sessions-and-markers"),
                branchLine(cwd(), latest(), "proj-4-other"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .as("the case a branch was spelt in is kept raw, the board's spelling is applied when linking")
                .containsEntry(new SessionId(sessionId()), onBranches("PROJ-3", "proj-4"));
    }

    @Test
    void shouldReadNoTicketIdFromMainOrFromABranchWithNoTicketPrefix() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                branchLine(cwd(), started(), "main"),
                branchLine(cwd(), later(), "close-PROJ-1"),
                branchLine(cwd(), latest(), "feature/PROJ-1"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), LinkFacts.none());
    }

    @Test
    void shouldReadABranchFromALineWithNoTimestamp() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"), branchLineWithoutTimestamp("PROJ-9-x"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .containsEntry(new SessionId(sessionId()), onBranches("PROJ-9"));
    }

    @Test
    void shouldAnswerTheFactsOfATranscriptThatIsNotYetASession() throws IOException {
        writeTranscript(projects, folder(), sessionId(), lineWithoutCwd(started()));
        writeTranscript(projects, folder(), otherSessionId(), branchLineWithoutTimestamp("PROJ-9-x"));

        assertThat(scanned(projects, cacheFile, locator, unwritable, unlistable).linkFacts())
                .as("a live session is linked through a transcript that has said little so far")
                .containsEntry(new SessionId(otherSessionId()), onBranches("PROJ-9"));
    }

    @Test
    void shouldAddWhatAGrownTranscriptSaysToWhatTheCacheHolds() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), branchLine(cwd(), started(), "PROJ-1-x"));
        TranscriptRepository repository = scanned(projects, cacheFile, locator, unwritable, unlistable);

        append(file, commandLine(cwd(), later(), "PROJ-1-x", "/ticket", "start PROJ-2"));
        repository.rescan(List.of(file), locator, unwritable);

        assertThat(repository.linkFacts())
                .containsEntry(new SessionId(sessionId()), ranOnBranch(ticketRun("start", "PROJ-2"), "PROJ-1"));
    }

    @Test
    void shouldAnswerTheFactsFromTheCacheWithoutReadingAnyTranscript() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "PROJ-1-x", "/ticket", "start PROJ-2"));
        scanned(projects, cacheFile, locator, unwritable, unlistable);

        assertThat(repositoryOver(projects, cacheFile).linkFacts())
                .containsEntry(new SessionId(sessionId()), ranOnBranch(ticketRun("start", "PROJ-2"), "PROJ-1"));
    }

    @Test
    void shouldAnswerNoFactsBeforeAnyTranscriptWasScanned() {
        assertThat(repositoryOver(projects, cacheFile).linkFacts()).isEmpty();
    }
}
