package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.fromClaudeAgents;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.fromTheRegistry;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.hostOfTheRealClaude;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.registeredPidsOnceListing;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SourcesContractFixture.spawnedInTheProjectRoot;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

@Tag("contract")
class SourcesContractTest {

    @Test
    void shouldAgreeOnPidSessionIdDirectoryNameAndStateOverTheLiveSessions() {
        assertThat(fromTheRegistry())
                .as("the registry and claude agents --json must describe the same live sessions")
                .containsExactlyInAnyOrderElementsOf(fromClaudeAgents());
    }

    @Test
    void shouldRegisterAHostedClaudeUnderThePidItWasSpawnedWith() throws InterruptedException {
        PtyHostRepository host = hostOfTheRealClaude();
        HostedSessionId id = HostedSessionId.random();
        ProcessId pid = spawnedInTheProjectRoot(host, id);
        try {
            assertThat(registeredPidsOnceListing(pid))
                    .as("a hosted tab finds its registry record by the spawned pid; a launcher that forks would leave it starting")
                    .contains(pid.value());
        } finally {
            host.end(id);
        }
    }
}
