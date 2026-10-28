package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.aiTitleLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.append;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.assistantLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.beyondEpochMillis;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.cwd;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.everyLoggedSession;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.factsOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.folder;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.lastActivityOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.later;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.metaLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.modifiedAt;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.notJsonLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.otherSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.readOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.rewriteInPlace;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.scanned;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.started;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.toolResultLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.touch;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.userLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.userLineWithBlocks;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeSubagentTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.repositoryOver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.TranscriptFacts;
import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.FirstPrompt;
import dev.maxenceng.lazyclaude.session.published.NoFirstPrompt;
import dev.maxenceng.lazyclaude.session.published.NoTitle;
import dev.maxenceng.lazyclaude.session.published.ReadableTranscript;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.UnreadableTranscript;

@ExtendWith(MockitoExtension.class)
class TranscriptRepositoryTest {

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
    void shouldAnswerWhatIsReadWhileARescanIsStillRunning() throws Exception {
        Path blocker = root.resolve("blocker");
        Files.writeString(blocker, "a file where the cache folder should be", StandardCharsets.UTF_8);
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = new TranscriptRepository(
                projects, new TranscriptCacheFile(blocker.resolve("lazyclaude/past-sessions.json")));
        CountDownLatch scanning = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(call -> {
                    scanning.countDown();
                    release.await();
                    return null;
                })
                .when(unwritable)
                .unwritable(any());
        Thread rescan = Thread.ofPlatform().start(() -> repository.rescanAll(everyLoggedSession(), unwritable, unlistable));
        scanning.await();

        try {
            assertThat(CompletableFuture.supplyAsync(() -> readOf(repository)).get(5, TimeUnit.SECONDS))
                    .as("a live state change asks for the past sessions on the registry watcher's thread, and must not wait for a scan")
                    .hasSize(1);
        } finally {
            release.countDown();
            rescan.join();
        }
    }

    @Test
    void shouldReadOnlyTheTranscriptsOfTheLoggedSessions() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Mine"));
        writeTranscript(projects, folder(), otherSessionId(), userLine(cwd(), later(), "Started in a plain terminal"));
        Set<SessionId> logged = Set.of(new SessionId(sessionId()));

        TranscriptRepository repository = scanned(projects, cacheFile, logged, unwritable, unlistable);

        assertThat(repository.transcriptsOf(everyLoggedSession()).read().keySet())
                .as("a project with hundreds of transcripts reads none it did not start")
                .containsExactly(new SessionId(sessionId()));
        assertThat(Files.readString(cacheFile)).doesNotContain(otherSessionId());
    }

    @Test
    void shouldKeepInTheCacheTheTranscriptOfASessionThisProjectDidNotLog() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Mine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        repository.rescanAll(Set.of(), unwritable, unlistable);

        assertThat(repository.transcriptsOf(Set.of()).read()).isEmpty();
        assertThat(Files.readString(cacheFile))
                .as("the cache is shared by every project, so a scan of one forgets only what is gone")
                .contains(sessionId());
    }

    @Test
    void shouldCallNoLoggedSessionGoneWhileAFolderOfTranscriptsCannotBeListed() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        Files.setPosixFilePermissions(projects.resolve(folder()), PosixFilePermissions.fromString("---------"));
        Set<SessionId> logged = Set.of(new SessionId(sessionId()));

        TranscriptRepository repository = scanned(projects, cacheFile, logged, unwritable, unlistable);

        assertThat(repository.transcriptsOf(logged).gone())
                .as("a folder that cannot be listed may hold the transcript, and a gone session is dropped from the log for good")
                .isEmpty();
    }

    @Test
    void shouldNotCallALoggedSessionGoneWhenWhetherItsTranscriptExistsCannotBeTold() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        Set<SessionId> logged = Set.of(new SessionId(sessionId()));
        TranscriptRepository repository = scanned(projects, cacheFile, logged, unwritable, unlistable);
        Files.setPosixFilePermissions(projects.resolve(folder()), PosixFilePermissions.fromString("---------"));

        repository.rescan(List.of(file), logged, unwritable);

        assertThat(repository.transcriptsOf(logged).gone())
                .as("a transcript that cannot be looked at is not a transcript that is gone")
                .isEmpty();
    }

    @Test
    void shouldCallALoggedSessionGoneOnceAScanFoundNoTranscriptOfItsName() throws IOException {
        TranscriptRepository repository = new TranscriptRepository(projects, new TranscriptCacheFile(cacheFile));
        Set<SessionId> logged = Set.of(new SessionId(sessionId()));
        assertThat(repository.transcriptsOf(logged).gone()).as("nothing was listed yet, so nothing is known gone").isEmpty();

        repository.rescanAll(logged, unwritable, unlistable);

        assertThat(repository.transcriptsOf(logged).gone()).containsExactly(new SessionId(sessionId()));
    }

    @Test
    void shouldNotCallALoggedSessionGoneWhileItsTranscriptIsThereEvenIfItCannotBeReadYet() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));

        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        assertThat(repository.transcriptsOf(Set.of(new SessionId(sessionId()))).gone())
                .as("a transcript that cannot be read is not a transcript that is gone")
                .isEmpty();
    }

    @Test
    void shouldCallALoggedSessionGoneOnceItsTranscriptIsDeleted() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        Files.delete(file);
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(repository.transcriptsOf(Set.of(new SessionId(sessionId()))).gone()).containsExactly(new SessionId(sessionId()));
    }

    @Test
    void shouldCallNoLoggedSessionGoneWhenTheProjectsDirectoryCannotBeListed() throws IOException {
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);
        Files.setPosixFilePermissions(projects, PosixFilePermissions.fromString("---------"));

        try {
            repository.rescanAll(everyLoggedSession(), unwritable, unlistable);
        } finally {
            Files.setPosixFilePermissions(projects, PosixFilePermissions.fromString("rwx------"));
        }

        assertThat(repository.transcriptsOf(Set.of(new SessionId(sessionId()))).gone())
                .as("a root that cannot be listed says nothing about which transcripts are gone")
                .isEmpty();
    }

    @Test
    void shouldListATopLevelTranscriptWithWhatItSays() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "Refine the ticket"),
                assistantLine(cwd(), later()),
                aiTitleLine("Refine PROJ-1"));

        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        assertThat(readOf(repository))
                .containsExactly(factsOf(new AiTitle("Refine PROJ-1"), new FirstPrompt("Refine the ticket"), later()));
    }

    @Test
    void shouldNeverListASubagentTranscript() throws IOException {
        writeSubagentTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Explore"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable))).as("a subagents/ file is not a session").isEmpty();
    }

    @Test
    void shouldTitleASessionByItsLastAiTitle() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "Refine"),
                aiTitleLine("First title"),
                assistantLine(cwd(), later()),
                aiTitleLine("Last title"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable))).singleElement().satisfies(session -> assertThat(session.title()).isEqualTo(new AiTitle("Last title")));
    }

    @Test
    void shouldTitleASessionWhoseAiTitleIsBlankByItsFirstPrompt() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"), assistantLine(cwd(), later()), aiTitleLine(" "));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .as("a blank title says nothing, so the session is still listed")
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo(new FirstPrompt("Refine")));
    }

    @Test
    void shouldTitleAnUntitledSessionByItsFirstPromptCutToOneLine() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "Fix the\n  status line"),
                userLine(cwd(), later(), "A second prompt"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo(new FirstPrompt("Fix the status line")));
    }

    @Test
    void shouldTakeTheFirstPromptFromATextBlock() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                toolResultLine(cwd(), started()),
                userLineWithBlocks(cwd(), later(), "Review this change"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session.title())
                        .as("a tool result is not what the session was asked")
                        .isEqualTo(new FirstPrompt("Review this change")));
    }

    @Test
    void shouldNotTakeAnInjectedMetaEntryForThePrompt() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                metaLine(cwd(), started(), "<system-reminder>context</system-reminder>"),
                userLine(cwd(), later(), "The real prompt"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo(new FirstPrompt("The real prompt")));
    }

    @Test
    void shouldKeepWhatTheSessionWasFirstAskedBesideItsAiTitle() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "/ticket start PROJ-1"),
                assistantLine(cwd(), later()),
                aiTitleLine("Refine PROJ-1"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session)
                        .extracting(TranscriptFacts::title, TranscriptFacts::opening)
                        .containsExactly(new AiTitle("Refine PROJ-1"), new FirstPrompt("/ticket start PROJ-1")));
    }

    @Test
    void shouldHoldNoOpeningPromptForATranscriptThatHoldsNone() throws IOException {
        writeTranscript(projects, folder(), sessionId(), assistantLine(cwd(), started()));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session)
                        .extracting(TranscriptFacts::title, TranscriptFacts::opening)
                        .containsExactly(new NoTitle(), new NoFirstPrompt()));
    }

    @Test
    void shouldListASessionWithNeitherTitleNorPromptAsUntitled() throws IOException {
        writeTranscript(projects, folder(), sessionId(), assistantLine(cwd(), started()));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo(new NoTitle()));
    }

    @Test
    void shouldNotListATranscriptWithNoEntryCarryingATimestamp() throws IOException {
        writeTranscript(projects, folder(), sessionId(), aiTitleLine("Only a title"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .as("without a timestamp there is no last activity to order it by")
                .isEmpty();
    }

    @Test
    void shouldSkipALineThatDoesNotParseAndCountTheRest() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "Refine"),
                notJsonLine(),
                assistantLine(cwd(), later()));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .singleElement()
                .satisfies(session -> assertThat(session.lastActivity().value()).isEqualTo(Instant.parse(later())));
    }

    @Test
    void shouldSkipALineWhoseTimestampParsesButCannotBeCounted() throws IOException {
        writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "Refine"),
                assistantLine(cwd(), beyondEpochMillis()),
                assistantLine(cwd(), later()));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable)))
                .as("an instant past what milliseconds can hold is a bad line, not a reason to stop reading")
                .singleElement()
                .satisfies(session -> assertThat(session.lastActivity().value()).isEqualTo(Instant.parse(later())));
    }

    @Test
    void shouldReadAGrownTranscriptOnlyFromTheOffsetTheCacheHolds() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"), aiTitleLine("AAAA"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        rewriteInPlace(file, userLine(cwd(), started(), "Refine") + aiTitleLine("BBBB") + assistantLine(cwd(), later()));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository)).singleElement().satisfies(session -> {
            assertThat(session.title())
                    .as("the title sits before the offset, so it is not read again")
                    .isEqualTo(new AiTitle("AAAA"));
            assertThat(session.lastActivity().value()).isEqualTo(Instant.parse(later()));
        });
    }

    @Test
    void shouldNotConsumeALastLineWithNoNewlineYetAndTakeItWholeNextTime() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        String half = assistantLine(cwd(), later()).strip();

        append(file, half.substring(0, 30));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);
        assertThat(lastActivityOf(repository)).as("half a line is not an entry").isEqualTo(Instant.parse(started()));
        append(file, half.substring(30) + "\n");
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(lastActivityOf(repository)).as("the whole line is read from before its first byte").isEqualTo(Instant.parse(later()));
    }

    @Test
    void shouldReadAShrunkTranscriptAgainInFull() throws IOException {
        Path file = writeTranscript(
                projects,
                folder(),
                sessionId(),
                userLine(cwd(), started(), "A long first prompt that makes the file big"),
                assistantLine(cwd(), later()));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        rewriteInPlace(file, userLine(cwd(), started(), "Short"));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository))
                .singleElement()
                .satisfies(session -> {
                    assertThat(session.title()).isEqualTo(new FirstPrompt("Short"));
                    assertThat(session.lastActivity().value()).isEqualTo(Instant.parse(started()));
                });
    }

    @Test
    void shouldReadATranscriptAgainInFullWhenItsFileKeyChanges() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "AAAA"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        Path replacement = projects.resolve(folder()).resolve("replacement.tmp");
        Files.writeString(replacement, userLine(cwd(), started(), "BBBB") + assistantLine(cwd(), later()), StandardCharsets.UTF_8);

        Files.move(replacement, file, StandardCopyOption.REPLACE_EXISTING);
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository))
                .singleElement()
                .satisfies(session -> assertThat(session.title())
                        .as("a new file under the old name is not a continuation of the old one")
                        .isEqualTo(new FirstPrompt("BBBB")));
    }

    @Test
    void shouldNotReadATranscriptAgainWhenItsSizeModificationTimeAndFileKeyAreTheCaches() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "AAAA"));
        Instant modified = modifiedAt(file);
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        rewriteInPlace(file, userLine(cwd(), started(), "BBBB"));
        touch(file, modified);
        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        assertThat(readOf(repository))
                .singleElement()
                .satisfies(session -> assertThat(session.title()).isEqualTo(new FirstPrompt("AAAA")));
    }

    @Test
    void shouldForgetADeletedTranscriptAndRewriteTheCacheWithoutIt() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        Files.delete(file);
        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        assertThat(readOf(repository)).isEmpty();
        assertThat(readOf(repositoryOver(projects, cacheFile))).as("deleted by Claude Code's retention, so gone from the cache").isEmpty();
    }

    @Test
    void shouldForgetADeletedTranscriptWhenOnlyThatFileIsRescanned() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        Files.delete(file);
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository)).isEmpty();
    }

    @Test
    void shouldForgetEveryTranscriptWhenTheProjectsDirectoryIsGone() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        Files.delete(file);
        Files.delete(file.getParent());
        Files.delete(projects);
        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        assertThat(readOf(repository)).isEmpty();
    }

    @Test
    void shouldListNothingAndWarnOfNothingWhenTheProjectsDirectoryIsMissing() {
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        assertThat(readOf(repository)).isEmpty();
        verifyNoInteractions(unwritable, unlistable);
    }

    @Test
    void shouldNotListATranscriptItCannotReadAndReadItOnItsNextChange() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        assertThat(readOf(repository)).as("unreadable, so not listed").isEmpty();

        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository)).as("retried when it changes").hasSize(1);
    }

    @Test
    void shouldKeepTheRowAndFactsOfAKnownTranscriptItCanNoLongerReadWithWhyUntilItReadsAgain() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        append(file, assistantLine(cwd(), later()));
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository))
                .as("a session already listed stays listed with what the last read said, so it can still be resumed, and says why it cannot be read now")
                .singleElement()
                .satisfies(located -> assertThat(located)
                        .extracting(TranscriptFacts::title, TranscriptFacts::opening, TranscriptFacts::reading)
                        .containsExactly(
                                new FirstPrompt("Refine"),
                                new FirstPrompt("Refine"),
                                new UnreadableTranscript(new Reason("access denied to " + file))));

        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        repository.rescan(List.of(file), everyLoggedSession(), unwritable);

        assertThat(readOf(repository))
                .singleElement()
                .satisfies(located -> assertThat(located.reading()).isEqualTo(new ReadableTranscript()));
    }

    @Test
    void shouldListFromTheCacheWithoutReadingAnyTranscript() throws IOException {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"), assistantLine(cwd(), later()));
        scanned(projects, cacheFile, unwritable, unlistable);

        Files.delete(file);

        assertThat(readOf(repositoryOver(projects, cacheFile)))
                .as("the first frame is drawn from the cache before any transcript is touched")
                .containsExactly(factsOf(new FirstPrompt("Refine"), new FirstPrompt("Refine"), later()));
    }

    @Test
    void shouldListEveryTranscriptOfEveryFolder() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "One"));
        writeTranscript(projects, "-work-other", otherSessionId(), userLine(cwd(), later(), "Two"));

        assertThat(readOf(scanned(projects, cacheFile, unwritable, unlistable))).hasSize(2);
    }

    @Test
    void shouldRaiseTheCacheFallbackWhenTheCacheCannotBeWrittenAndStillListTheSessions() throws IOException {
        Path blocker = root.resolve("blocker");
        Files.writeString(blocker, "a file where the cache folder should be", StandardCharsets.UTF_8);
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = new TranscriptRepository(
                projects, new TranscriptCacheFile(blocker.resolve("lazyclaude/past-sessions.json")));

        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        verify(unwritable).unwritable(any());
        assertThat(readOf(repository)).as("past sessions still show, read in full on every start").hasSize(1);
    }

    @Test
    void shouldNameAnAccessDeniedCacheFolderInTheCacheFallback() throws IOException {
        Path locked = root.resolve("locked");
        Files.createDirectories(locked);
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("r-x------"));
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = new TranscriptRepository(projects, new TranscriptCacheFile(locked.resolve("past-sessions.json")));

        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        verify(unwritable)
                .unwritable(new Reason("cannot write the cache %s: access denied to %s"
                        .formatted(locked.resolve("past-sessions.json"), locked.resolve("past-sessions.json.tmp"))));
    }

    @Test
    void shouldRaiseTheRootFallbackWhenTheProjectsDirectoryCannotBeListedAndKeepWhatTheCacheHolds() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        Files.setPosixFilePermissions(projects, PosixFilePermissions.fromString("---------"));

        try {
            repository.rescanAll(everyLoggedSession(), unwritable, unlistable);
        } finally {
            Files.setPosixFilePermissions(projects, PosixFilePermissions.fromString("rwx------"));
        }

        verify(unlistable).unlistable(new Reason("cannot list %s: access denied to %s".formatted(projects, projects)));
        assertThat(readOf(repository)).as("a root that cannot be listed says nothing about which transcripts are gone").hasSize(1);
    }

    @Test
    void shouldNotTouchTheCacheWhenNothingChanged() throws IOException {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);
        Instant written = modifiedAt(cacheFile);
        touch(cacheFile, written.minusSeconds(60));

        repository.rescanAll(everyLoggedSession(), unwritable, unlistable);

        assertThat(modifiedAt(cacheFile)).as("a sweep that finds nothing writes nothing").isEqualTo(written.minusSeconds(60));
        verify(unwritable, never()).unwritable(any());
    }

    @Test
    void shouldTreatACacheItCannotUseAsEmptyAndRewriteItWithoutAWarning() throws IOException {
        Files.createDirectories(cacheFile.getParent());
        Files.writeString(cacheFile, "garbage", StandardCharsets.UTF_8);
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));

        TranscriptRepository repository = scanned(projects, cacheFile, unwritable, unlistable);

        assertThat(readOf(repositoryOver(projects, cacheFile))).hasSize(1);
        assertThat(readOf(repository)).hasSize(1);
        verifyNoInteractions(unwritable);
    }
}
