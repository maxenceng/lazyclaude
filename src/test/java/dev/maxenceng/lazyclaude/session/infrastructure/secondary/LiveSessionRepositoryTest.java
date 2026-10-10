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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.published.Busy;

class LiveSessionRepositoryTest {

    private static final Path CWD = Path.of("/work/app");
    private static final Duration PATIENCE = Duration.ofSeconds(10);

    @TempDir
    Path root;

    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
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

        assertThat(repositoryOver(registry, said, agents).liveSessions()).containsExactly(registeredBusy(CWD));
        assertThat(said.toString(StandardCharsets.UTF_8)).as("no fallback, so nothing to warn about").isEmpty();
    }

    @Test
    void shouldFallBackToClaudeAgentsWhenTheRegistryIsMissingAndWarnOnceNamingWhy() throws IOException {
        Path claude = claudePrinting(root, agentsJson(List.of(agent(registeredPid(), registeredSessionId(), CWD, "busy"))));
        LiveSessionRepository repository = repositoryOver(registry, said, new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE));

        assertThat(repository.liveSessions()).containsExactly(reportedByAgents(CWD, new Busy()));
        assertThat(repository.liveSessions()).containsExactly(reportedByAgents(CWD, new Busy()));

        assertThat(said.toString(StandardCharsets.UTF_8))
                .contains(sessions.toString(), "claude agents --json", "2 s")
                .hasLineCount(1);
    }

    @Test
    void shouldFallBackWhenARecordNoLongerParses() throws IOException {
        writeRecord(sessions, "17234.json", "{\"pid\": ");
        runningProcess(proc, registeredPid(), "claude", "175781");
        Path claude = claudePrinting(root, "[]");

        assertThat(repositoryOver(registry, said, new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE)).liveSessions()).isEmpty();
        assertThat(said.toString(StandardCharsets.UTF_8)).contains("17234.json");
    }

    @Test
    void shouldNameBothReasonsWhenNeitherSourceAnswers() {
        LiveSessionRepository repository = repositoryOver(registry, said, new ClaudeAgentsRepository(List.of(missingCommand()), PATIENCE));

        assertThatThrownBy(repository::liveSessions)
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining(sessions.toString())
                .hasMessageContaining(missingCommand());
    }
}
