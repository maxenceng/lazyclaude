package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.anotherMachinesDomain;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.machineIdFile;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.procWithNamespace;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.record;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.recordIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registered;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredBusy;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredPid;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.runningProcess;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.sdkCliRecord;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.waitingRecord;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.writeRecord;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SdkCliEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.SourceFallback;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;

@ExtendWith(MockitoExtension.class)
class SessionRegistryRepositoryTest {

    @Mock
    private SourceFallback fallback;


    private static final Path CWD = Path.of("/work/app");
    private static final String PROC_START = "175781";

    @TempDir
    Path root;

    private Path sessions;
    private Path proc;
    private SessionRegistryRepository registry;

    @BeforeEach
    void setUp() throws IOException {
        sessions = root.resolve("sessions");
        proc = procWithNamespace(root);
        registry = new SessionRegistryRepository(sessions, proc, machineIdFile(root));
    }

    @Test
    void shouldListALiveSessionWithEveryFieldTheRegistryHolds() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registeredBusy(CWD));
    }

    @Test
    void shouldNotListARecordFromAnotherPidDomain() throws IOException {
        writeRecord(
                sessions,
                registeredPid() + ".json",
                recordIn(anotherMachinesDomain(), registeredPid(), registeredSessionId(), CWD, PROC_START, "busy"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions())
                .as("a pid from another machine or namespace names some other process here")
                .isEmpty();
    }

    @Test
    void shouldNotListARecordWhoseProcessIsGone() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));

        assertThat(registry.liveSessions()).as("a record is never rewritten when its process dies").isEmpty();
    }

    @Test
    void shouldNotListARecordWhoseProcessStartedAtAnotherTime() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        runningProcess(proc, registeredPid(), "claude", "999999");

        assertThat(registry.liveSessions())
                .as("the pid was reused by another process, whatever the record says")
                .isEmpty();
    }

    @Test
    void shouldReadFieldTwentyTwoOfAStatWhoseCommandHasSpacesAndParentheses() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        runningProcess(proc, registeredPid(), "a b) S 1 (c", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registeredBusy(CWD));
    }

    @Test
    void shouldIgnoreAKeyFile() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        writeRecord(sessions, registeredPid() + ".0123abcd.key", "not json at all");
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registeredBusy(CWD));
    }

    @Test
    void shouldReportAnSdkCliRecordForTheDomainToExclude() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", sdkCliRecord(registeredPid(), registeredSessionId(), CWD));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions())
                .singleElement()
                .extracting(session -> session.entrypoint())
                .isEqualTo(new SdkCliEntrypoint());
    }

    @Test
    void shouldCarryAPermissionPromptAsTheReasonItWaitsFor() throws IOException {
        writeRecord(
                sessions,
                registeredPid() + ".json",
                waitingRecord(registeredPid(), registeredSessionId(), CWD, "permission prompt"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registered(CWD, ReportedStatus.WAITING, permissionPrompt()));
    }

    @Test
    void shouldCarryAnyOtherReasonItWaitsFor() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", waitingRecord(registeredPid(), registeredSessionId(), CWD, "input needed"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registered(CWD, ReportedStatus.WAITING, inputNeeded()));
    }

    @Test
    void shouldBeUnavailableWhenTheRegistryDirectoryIsMissing() {
        assertThatThrownBy(() -> registry.liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining(sessions.toString());
    }

    @Test
    void shouldBeUnavailableNamingTheRecordThatNoLongerParses() throws IOException {
        writeRecord(sessions, "17234.json", "{\"pid\": ");
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThatThrownBy(() -> registry.liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("17234.json");
    }

    @Test
    void shouldSkipARecordThatNoLongerParsesWhenNoProcessHasItsPid() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        writeRecord(sessions, "22862.json", "{\"pid\": ");
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions())
                .as("a dead process never rewrites its record, so a truncated one would pin the fallback for good")
                .containsExactly(registeredBusy(CWD));
    }

    @Test
    void shouldBeUnavailableWhenACwdIsNoPath() throws IOException {
        writeRecord(sessions, "17234.json", record(registeredPid(), registeredSessionId(), CWD, "busy").replace("/work/app", "/work/\\u0000app"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThatThrownBy(() -> registry.liveSessions())
                .as("an unchecked failure here would end the watcher thread and freeze the panel")
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("17234.json");
    }

    @Test
    void shouldBeUnavailableWhenARecordLacksAField() throws IOException {
        writeRecord(sessions, "17234.json", record(registeredPid(), registeredSessionId(), CWD, "busy").replace("\"name\":\"lazyclaude-b6\",", ""));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThatThrownBy(() -> registry.liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("17234.json");
    }

    @Test
    void shouldReportAStatusItDoesNotKnowAsUnknown() throws IOException {
        writeRecord(sessions, "17234.json", record(registeredPid(), registeredSessionId(), CWD, "napping"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions())
                .as("one session's status from a newer Claude Code must not make every session unavailable")
                .containsExactly(registered(CWD, ReportedStatus.UNKNOWN, new NoWaitingReason()));
    }

    @Test
    void shouldReportAMissingStatusAsUnknown() throws IOException {
        writeRecord(sessions, "17234.json", record(registeredPid(), registeredSessionId(), CWD, "busy").replace("\"status\":\"busy\",", ""));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions()).containsExactly(registered(CWD, ReportedStatus.UNKNOWN, new NoWaitingReason()));
    }

    @Test
    void shouldBeUnavailableWhenThisMachinesPidDomainCannotBeRead() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        Files.delete(root.resolve("machine-id"));

        assertThatThrownBy(() -> registry.liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("machine-id");
    }

    @Test
    void shouldListNothingWhenTheRegistryIsEmpty() throws IOException {
        Files.createDirectories(sessions);

        assertThat(registry.liveSessions()).isEmpty();
    }

    @Test
    void shouldTellTheFallbackNothingSinceThereIsNoOtherSourceToFallBackTo() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        runningProcess(proc, registeredPid(), "claude", PROC_START);

        assertThat(registry.liveSessions(fallback)).containsExactly(registeredBusy(CWD));
        verifyNoInteractions(fallback);
    }
}
