package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.claudeFailing;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.claudeHanging;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.claudePrinting;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.FakeClaudeFixture.missingCommand;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.agent;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.agentsJson;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredPid;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.registeredSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.reportedByAgents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.published.Busy;

class ClaudeAgentsRepositoryTest {

    private static final Path CWD = Path.of("/work/app");
    private static final Duration PATIENCE = Duration.ofSeconds(10);

    @TempDir
    Path root;

    @Test
    void shouldListWhatClaudeAgentsReportsWithoutAVersionOrAnEntrypoint() throws IOException {
        Path claude = claudePrinting(root, agentsJson(List.of(agent(registeredPid(), registeredSessionId(), CWD, "busy"))));

        assertThat(new ClaudeAgentsRepository(List.of(claude.toString(), "agents", "--json"), PATIENCE).liveSessions())
                .containsExactly(reportedByAgents(CWD, new Busy()));
    }

    @Test
    void shouldListNothingWhenNoSessionIsLive() throws IOException {
        Path claude = claudePrinting(root, "[]");

        assertThat(new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE).liveSessions()).isEmpty();
    }

    @Test
    void shouldBeUnavailableWhenClaudeExitsNonZero() throws IOException {
        Path claude = claudeFailing(root, 3);

        assertThatThrownBy(() -> new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE).liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("exited with 3");
    }

    @Test
    void shouldBeUnavailableWhenClaudeIsNotOnThePath() {
        assertThatThrownBy(() -> new ClaudeAgentsRepository(List.of(missingCommand()), PATIENCE).liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining(missingCommand());
    }

    @Test
    void shouldBeUnavailableWhenClaudePrintsSomethingThatIsNotJson() throws IOException {
        Path claude = claudePrinting(root, "Welcome to Claude");

        assertThatThrownBy(() -> new ClaudeAgentsRepository(List.of(claude.toString()), PATIENCE).liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("claude agents --json");
    }

    @Test
    void shouldBeUnavailableWhenClaudeDoesNotAnswerInTime() throws IOException {
        Path claude = claudeHanging(root);

        assertThatThrownBy(() -> new ClaudeAgentsRepository(List.of(claude.toString()), Duration.ofMillis(300)).liveSessions())
                .isInstanceOf(SessionsUnavailableException.class)
                .hasMessageContaining("did not answer");
    }
}
