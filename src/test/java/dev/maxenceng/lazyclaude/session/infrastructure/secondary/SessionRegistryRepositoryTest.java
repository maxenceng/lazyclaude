package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.record;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.recordFileName;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registered;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredBusy;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredProcess;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.waitingRecord;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.writeRecord;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SessionRecordUnreadableException;

class SessionRegistryRepositoryTest {

    @TempDir
    Path root;

    private Path sessions;
    private SessionRegistryRepository registry;

    @BeforeEach
    void setUp() {
        sessions = root.resolve("sessions");
        registry = new SessionRegistryRepository(sessions);
    }

    @Test
    void shouldReadTheRecordOfThePidWithEveryFieldTheDomainReads() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy"));

        assertThat(registry.recordOf(registeredProcess())).contains(registeredBusy());
    }

    @Test
    void shouldReadOnlyTheRecordNamedByThePid() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy"));
        writeRecord(sessions, secondProcessId().value() + ".json", "not json at all");

        assertThat(registry.recordOf(registeredProcess()))
                .as("another process's record, whatever it holds, is not this session's business")
                .contains(registeredBusy());
    }

    @Test
    void shouldFindNoRecordForAPidClaudeHasNotWrittenOneFor() throws IOException {
        Files.createDirectories(sessions);

        assertThat(registry.recordOf(registeredProcess())).isEmpty();
    }

    @Test
    void shouldFindNoRecordWhenTheRegistryDirectoryDoesNotExistYet() {
        assertThat(registry.recordOf(registeredProcess())).isEmpty();
    }

    @Test
    void shouldCarryAPermissionPromptAsTheReasonItWaitsFor() throws IOException {
        writeRecord(sessions, recordFileName(), waitingRecord("permission prompt"));

        assertThat(registry.recordOf(registeredProcess()))
                .contains(registered(ReportedStatus.WAITING, permissionPrompt()));
    }

    @Test
    void shouldCarryAnyOtherReasonItWaitsFor() throws IOException {
        writeRecord(sessions, recordFileName(), waitingRecord("input needed"));

        assertThat(registry.recordOf(registeredProcess())).contains(registered(ReportedStatus.WAITING, inputNeeded()));
    }

    @Test
    void shouldRefuseARecordThatDoesNotParseNamingIt() throws IOException {
        writeRecord(sessions, recordFileName(), "{\"pid\": ");

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName());
    }

    @Test
    void shouldRefuseARecordThatIsNotAnObject() throws IOException {
        writeRecord(sessions, recordFileName(), "[1, 2]");

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName());
    }

    @Test
    void shouldRefuseARecordThatHoldsOnlyNull() throws IOException {
        writeRecord(sessions, recordFileName(), "null");

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .as("a NullPointerException here would escape to the watch thread and stop every session's updates")
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName());
    }

    @Test
    void shouldRefuseARecordThatLacksANameNamingTheField() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy").replace("\"name\":\"lazyclaude-b6\",", ""));

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName())
                .hasMessageContaining("name");
    }

    @Test
    void shouldRefuseARecordThatLacksAStartTimeNamingTheField() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy").replace("\"startedAt\":1791662734987,", ""));

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining("startedAt");
    }

    @Test
    void shouldRefuseARecordThatCannotBeReadAsAFile() throws IOException {
        Files.createDirectories(sessions.resolve(recordFileName()));

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .as("a record that is there and cannot be read is a failure of that session, not an absent record")
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName());
    }

    @Test
    void shouldRefuseAStatusItDoesNotKnowNamingIt() throws IOException {
        writeRecord(sessions, recordFileName(), record("napping"));

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .as("what a newer Claude Code means by a status cannot be guessed, so quitting must ask rather than assume it idle")
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining(recordFileName())
                .hasMessageContaining("napping");
    }

    @Test
    void shouldRefuseARecordThatLacksASessionIdNamingTheField() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy").replaceFirst("\"sessionId\":\"[^\"]*\",", ""));

        assertThatThrownBy(() -> registry.recordOf(registeredProcess()))
                .as("the row follows the session its record names, so a record that names none cannot be used")
                .isInstanceOf(SessionRecordUnreadableException.class)
                .hasMessageContaining("sessionId");
    }

    @Test
    void shouldReportNoSessionYetForARecordWrittenBeforeItsFirstStatus() throws IOException {
        writeRecord(sessions, recordFileName(), record("busy").replace("\"status\":\"busy\",", ""));

        assertThat(registry.recordOf(registeredProcess()))
                .as("claude writes its record a moment before its first status, so the session is still starting, not unknown")
                .isEmpty();
    }
}
