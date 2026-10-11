package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.claudePrinting;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.missingCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.agent;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.agentsJson;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.machineIdFile;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.procWithNamespace;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.record;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredBusy;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredPid;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.reportedByAgents;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.runningProcess;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.writeRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.SourceFallback;

@ExtendWith(MockitoExtension.class)
class LiveSessionRepositoryTest {

    private static final Path CWD = Path.of("/work/app");
    private static final Duration PATIENCE = Duration.ofSeconds(10);

    @TempDir
    Path root;

    @Mock
    private SourceFallback fallback;

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
    void shouldReadTheRegistryWhenItIsUsable() throws IOException {
        writeRecord(sessions, registeredPid() + ".json", record(registeredPid(), registeredSessionId(), CWD, "busy"));
        runningProcess(proc, registeredPid(), "claude", "175781");
        ClaudeAgentsRepository agents = new ClaudeAgentsRepository(List.of(missingCommand()), PATIENCE);

        assertThat(repositoryOver(registry, agents).liveSessions(fallback)).containsExactly(registeredBusy(CWD));
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldFallBackToClaudeAgentsWhenTheRegistryIsMissingAndWarnOnceNamingWhy() throws IOException {
        Path claude = claudePrinting(root, agentsJson(List.of(agent(registeredPid(), registeredSessionId(), CWD, "busy"))));
        LiveSessionRepository repository = repositoryOver(registry, new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE));

        assertThat(repository.liveSessions(fallback)).containsExactly(reportedByAgents(CWD, ReportedStatus.BUSY));
        assertThat(repository.liveSessions(fallback)).containsExactly(reportedByAgents(CWD, ReportedStatus.BUSY));

        ArgumentCaptor<String> cause = ArgumentCaptor.forClass(String.class);
        verify(fallback, times(1)).askingClaudeAgents(cause.capture(), eq(Duration.ofSeconds(2)));
        assertThat(cause.getValue()).contains(sessions.toString());
    }

    @Test
    void shouldFallBackWhenARecordNoLongerParses() throws IOException {
        writeRecord(sessions, "17234.json", "{\"pid\": ");
        runningProcess(proc, registeredPid(), "claude", "175781");
        Path claude = claudePrinting(root, "[]");

        assertThat(repositoryOver(registry, new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE)).liveSessions(fallback)).isEmpty();
        ArgumentCaptor<String> cause = ArgumentCaptor.forClass(String.class);
        verify(fallback).askingClaudeAgents(cause.capture(), any());
        assertThat(cause.getValue()).contains("17234.json");
    }

    @Test
    void shouldNameBothReasonsWhenNeitherSourceAnswers() {
        LiveSessionRepository repository = repositoryOver(registry, new ClaudeAgentsRepository(List.of(missingCommand()), PATIENCE));

        assertThatThrownBy(() -> repository.liveSessions(fallback))
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining(sessions.toString())
                .hasMessageContaining(missingCommand());
    }
}
