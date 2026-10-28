package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noRuns;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.append;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.branchLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.commandLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.cwd;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.everyLoggedSession;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.folder;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.later;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.latest;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.linksOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.otherSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.scanned;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.skillLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.skillLineWithoutArgs;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.started;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.repositoryOver;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.published.SessionId;

@ExtendWith(MockitoExtension.class)
class TranscriptLinkFactsTest {

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
    }

    @Test
    void shouldReadTheVerbAndIdsOfATicketCommandFromItsArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ticket", "start PROJ-1"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("start", "PROJ-1")));
    }

    @Test
    void shouldReadATicketCommandOfAPluginToo() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ddd-workflow:ticket", "review PROJ-1 PROJ-2"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("review", "PROJ-1", "PROJ-2")));
    }

    @Test
    void shouldNotReadTheArgsOfAnotherCommand() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/review", "start PROJ-1"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(noRuns());
    }

    @Test
    void shouldReadATicketSkillCallFromItsArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "ticket", "refine PROJ-3"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("refine", "PROJ-3")));
    }

    @Test
    void shouldReadAPluginsTicketSkillCallFromItsArgs() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "ddd-workflow:ticket", "done PROJ-3"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("done", "PROJ-3")));
    }

    @Test
    void shouldNotReadTheArgsOfAnotherSkill() throws IOException {
        writeTranscript(
                projects, folder(), sessionId(), skillLine(cwd(), started(), "main", "superpowers:brainstorming", "Refining PROJ-3"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(noRuns());
    }

    @Test
    void shouldNotReadASkillCallWithoutArgs() throws IOException {
        writeTranscript(projects, folder(), sessionId(), skillLineWithoutArgs(cwd(), started(), "ticket"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(noRuns());
    }

    @Test
    void shouldIgnoreArgsThatNameNoTicket() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                commandLine(cwd(), started(), "main", "/ticket", "new Do the thing"),
                commandLine(cwd(), later(), "main", "/ticket", "done"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .as("args without an id say nothing of a ticket, so the branch decides")
                .isEqualTo(noRuns());
    }

    @Test
    void shouldSkipAWordThatIsNotAnIdAmongTheNamedOnes() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "main", "/ticket", "review PROJ-1 and PROJ-22"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("review", "PROJ-1", "PROJ-22")));
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

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .isEqualTo(ran(ticketRun("start", "PROJ-1"), ticketRun("refine", "PROJ-1")));
    }

    @Test
    void shouldAddWhatAGrownTranscriptSaysToWhatTheCacheHolds() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "PROJ-1-x", "/ticket", "refine PROJ-1"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        append(file, commandLine(cwd(), later(), "PROJ-1-x", "/ticket", "start PROJ-2"));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(linksOf(repository)).isEqualTo(ran(ticketRun("refine", "PROJ-1"), ticketRun("start", "PROJ-2")));
    }

    @Test
    void shouldAnswerTheFactsFromTheCacheWithoutReadingAnyTranscript() throws IOException {
        writeTranscript(projects, folder(), sessionId(), commandLine(cwd(), started(), "PROJ-1-x", "/ticket", "start PROJ-2"));
        scanned(projects, cacheFile, unwritable, unlistable);

        assertThat(linksOf(repositoryOver(projects, cacheFile))).isEqualTo(ran(ticketRun("start", "PROJ-2")));
    }

    @Test
    void shouldAnswerNoFactsBeforeAnyTranscriptWasScanned() {
        assertThat(repositoryOver(projects, cacheFile).transcriptsOf(everyLoggedSession()).read()).isEmpty();
    }

    @Test
    void shouldLinkNothingThroughABranchTheTranscriptRecorded() throws IOException {
        writeTranscript(projects, folder(), sessionId(), branchLine(cwd(), started(), "PROJ-3-ticket-sessions-and-markers"));

        assertThat(linksOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .as("a branch name says where a session happened to run, not what it is about")
                .isEqualTo(noRuns());
    }

    @Test
    void shouldReadOnlyTheLinkFactsOfALoggedSession() throws IOException {
        writeTranscript(projects, folder(), otherSessionId(), commandLine(cwd(), started(), "main", "/ticket", "start PROJ-1"));

        TranscriptRepository repository = scanned(projects, cacheFile, Set.of(new SessionId(sessionId())), unwritable, unlistable);

        assertThat(repository.transcriptsOf(everyLoggedSession()).read()).isEmpty();
    }
}
