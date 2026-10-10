package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.fromClaudeAgents;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.fromTheRegistry;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class SourcesContractTest {

    @Test
    void shouldAgreeOnPidSessionIdDirectoryNameAndStateOverTheLiveSessions() {
        assertThat(fromTheRegistry())
                .as("the registry and claude agents --json must describe the same live sessions")
                .containsExactlyInAnyOrderElementsOf(fromClaudeAgents());
    }
}
