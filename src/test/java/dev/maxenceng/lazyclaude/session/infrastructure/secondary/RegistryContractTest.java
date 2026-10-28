package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.hostOfTheRealClaude;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.recordOnceIdle;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.recordOnceNamingAnotherSessionThan;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.recordOnceWritten;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.recordText;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.spawnedInTheProjectRoot;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryContractFixture.typedThenEntered;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.Version;

@Tag("contract")
class RegistryContractTest {

    @Test
    void shouldWriteTheRecordOfAHostedClaudeUnderItsSpawnedPidWithEveryFieldTheDomainReads() throws InterruptedException {
        PtyHostRepository host = hostOfTheRealClaude();
        SessionId id = SessionId.random();
        ProcessId pid = spawnedInTheProjectRoot(host, id);
        try {
            assertThat(recordOnceWritten(pid))
                    .as("a hosted tab finds its record by the spawned pid, and a status lazyclaude does not know would be refused here")
                    .hasValueSatisfying(record ->
                            assertThat(record.version())
                                    .as("the version a newer Claude Code stops writing would show here first")
                                    .isInstanceOf(Version.class));
        } finally {
            host.end(id);
        }
    }

    @Test
    void shouldRecordTheSessionIdTheHostedClaudeWasLaunchedWith() throws InterruptedException, IOException {
        PtyHostRepository host = hostOfTheRealClaude();
        SessionId id = SessionId.random();
        ProcessId pid = spawnedInTheProjectRoot(host, id);
        try {
            recordOnceWritten(pid);

            assertThat(recordText(pid))
                    .as("a row and a notification carry the id lazyclaude chose, so claude --session-id must have kept it")
                    .contains(id.value());
        } finally {
            host.end(id);
        }
    }

    @Test
    void shouldNameAnotherSessionInTheRecordOnceTheHostedClaudeIsCleared() throws InterruptedException {
        PtyHostRepository host = hostOfTheRealClaude();
        SessionId id = SessionId.random();
        ProcessId pid = spawnedInTheProjectRoot(host, id);
        try {
            recordOnceIdle(pid);

            typedThenEntered(host, id, "/clear");

            assertThat(recordOnceNamingAnotherSessionThan(pid, id))
                    .as("a hosted row follows the record's session, which /clear replaces under the same pid")
                    .hasValueSatisfying(record -> assertThat(record.session()).isNotEqualTo(id));
        } finally {
            host.end(id);
        }
    }
}
