package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.ticketClaude;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLog;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.endedLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntryBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.secondLaunchEntry;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.LaunchLogFileFixture.writtenLaunchLog;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.otherProject;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogFallback;
import dev.maxenceng.lazyclaude.session.domain.ResumeClaude;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;

@ExtendWith(MockitoExtension.class)
class LaunchLogRepositoryTest {

    @TempDir
    Path directory;

    @Mock
    private LaunchLogFallback fallback;

    private LaunchLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LaunchLogRepository(directory.resolve("state/lazyclaude"));
    }

    @Test
    void shouldReadAnEmptyLogWhileNoneWasWritten() {
        assertThat(repository.read(project(), fallback)).isEqualTo(emptyLaunchLog());
    }

    @Test
    void shouldReadBackEveryKindOfLaunchWithItsEnd() {
        repository.change(project(), fallback, _ -> launchLogOf(
                endedLaunchEntry(),
                launchEntryBuilder().command(new ResumeClaude(secondSessionId())).build(),
                launchEntryBuilder()
                        .command(ticketClaude(thirdSessionId(), Verb.DESIGN))
                        .build()));

        assertThat(repository.read(project(), fallback))
                .usingRecursiveComparison()
                .isEqualTo(launchLogOf(
                        endedLaunchEntry(),
                        launchEntryBuilder().command(new ResumeClaude(secondSessionId())).build(),
                        launchEntryBuilder()
                                .command(ticketClaude(thirdSessionId(), Verb.DESIGN))
                                .build()));
    }

    @Test
    void shouldApplyAChangeToWhatIsAlreadyLogged() {
        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        repository.change(project(), fallback, log -> log.launched(secondLaunchEntry()));

        assertThat(repository.read(project(), fallback).entries()).containsExactly(launchEntry(), secondLaunchEntry());
    }

    @Test
    void shouldKeepOneLogPerProject() {
        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        assertThat(repository.read(otherProject(), fallback).entries()).isEmpty();
    }

    @Test
    void shouldLeaveNoTemporaryFileBehind() throws IOException {
        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        try (Stream<Path> files = Files.list(directory.resolve("state/lazyclaude"))) {
            assertThat(files.map(file -> file.getFileName().toString())).containsExactly(repository.fileOf(project()).getFileName().toString());
        }
    }

    @Test
    void shouldLoseNoChangeWhenSeveralRunAtOnce() {
        List<CompletableFuture<Void>> changes = IntStream.range(0, 20)
                .mapToObj(number -> CompletableFuture.runAsync(() -> repository.change(
                        project(),
                        fallback,
                        log -> log.launched(launchEntryBuilder()
                                .command(new ResumeClaude(new SessionId("id-%d".formatted(number))))
                                .build()))))
                .toList();
        changes.forEach(CompletableFuture::join);

        assertThat(repository.read(project(), fallback).entries()).hasSize(20);
    }

    @Test
    void shouldTellTheFallbackOnceWhenAReadFailureIsFollowedByAWriteFailure() throws IOException {
        Files.createDirectories(repository.fileOf(project()));
        Path temporary = repository.fileOf(otherProject()).resolveSibling(repository.fileOf(otherProject()).getFileName() + ".tmp");
        Files.createDirectories(temporary.resolve("in the way"));

        repository.read(project(), fallback);
        repository.change(otherProject(), fallback, log -> log.launched(launchEntry()));

        verify(fallback, times(1)).unavailable(any());
    }

    @Test
    void shouldTellTheFallbackOnceWhenTwoWritesFail() throws IOException {
        Path temporary = repository.fileOf(project()).resolveSibling(repository.fileOf(project()).getFileName() + ".tmp");
        Files.createDirectories(temporary.resolve("in the way"));

        repository.change(project(), fallback, log -> log.launched(launchEntry()));
        repository.change(project(), fallback, log -> log.launched(secondLaunchEntry()));

        verify(fallback, times(1)).unavailable(any());
    }

    @Test
    void shouldTellTheFallbackOnceNamingTheFileAndLineWhenTheLogDoesNotParseOnEitherPath() throws IOException {
        Path file = writtenLaunchLog(repository.fileOf(project()), "{\"session\":\"%s\"".formatted(sessionId().value()));

        repository.read(project(), fallback);
        repository.change(project(), fallback, log -> log.launched(launchEntry()));
        repository.read(project(), fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback, times(1)).unavailable(cause.capture());
        assertThat(cause.getValue().value()).startsWith("%s:1: ".formatted(file));
    }

    @Test
    void shouldReadNothingAndLeaveAnUnparseableLogAsItIsAfterTellingTheFallback() throws IOException {
        Path file = writtenLaunchLog(repository.fileOf(project()), "this is not json");

        assertThat(repository.read(project(), fallback).entries()).isEmpty();
        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        assertThat(Files.readString(file))
                .as("a log nobody could read is the only record of what it held, so a launch does not replace it")
                .isEqualTo("this is not json" + System.lineSeparator());
    }

    @Test
    void shouldNameTheLineOfTheFirstEntryItCannotUse() throws IOException {
        repository.change(project(), fallback, log -> log.launched(launchEntry()));
        Path file = repository.fileOf(project());
        Files.writeString(file, Files.readString(file) + "{\"session\":\"\"}\n", StandardCharsets.UTF_8);

        repository.read(project(), fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).unavailable(cause.capture());
        assertThat(cause.getValue().value()).startsWith("%s:2: ".formatted(file));
    }

    @Test
    void shouldRefuseTheSecondLineOfASessionLoggedTwiceAndLeaveTheLogAsItIs() throws IOException {
        repository.change(project(), fallback, _ -> launchLogOf(launchEntry(), secondLaunchEntry(), endedLaunchEntry()));
        Path file = repository.fileOf(project());
        String written = Files.readString(file);

        assertThat(repository.read(project(), fallback)).isEqualTo(emptyLaunchLog());
        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).unavailable(cause.capture());
        assertThat(cause.getValue().value())
                .as("one session is one entry, so a repeated id is a line the log cannot use")
                .isEqualTo("%s:3: session %s is logged twice".formatted(file, sessionId().value()));
        assertThat(Files.readString(file)).isEqualTo(written);
    }

    @Test
    void shouldRefuseAnEntryOfAKindItDoesNotKnow() throws IOException {
        repository.change(project(), fallback, log -> log.launched(launchEntry()));
        Path file = repository.fileOf(project());
        Files.writeString(file, Files.readString(file).replace("\"kind\":\"new\"", "\"kind\":\"fork\""), StandardCharsets.UTF_8);

        repository.read(project(), fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).unavailable(cause.capture());
        assertThat(cause.getValue().value()).startsWith("%s:1: ".formatted(file));
    }

    @Test
    void shouldNameWhyTheLogCannotBeRead() throws IOException {
        Files.createDirectories(repository.fileOf(project()));

        repository.read(project(), fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).unavailable(cause.capture());
        assertThat(cause.getValue().value()).contains("cannot read");
    }

    @Test
    void shouldNameWhyTheLogCannotBeWritten() throws IOException {
        Path temporary = repository.fileOf(project()).resolveSibling(repository.fileOf(project()).getFileName() + ".tmp");
        Files.createDirectories(temporary.resolve("in the way"));

        repository.change(project(), fallback, log -> log.launched(launchEntry()));

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).unavailable(cause.capture());
        assertThat(cause.getValue().value()).contains("cannot write");
    }
}
