package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.ProtocolSchemaFixture.committedProtocolSchema;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ProtocolSchemaDriftTest {

    @Test
    void the_committed_schema_is_what_the_views_generate() throws IOException {
        assertThat(committedProtocolSchema())
                .as("docs/protocol/protocol.schema.json is stale: run make protocol-schema and commit it")
                .isEqualTo(Sidecar.protocolSchema());
    }
}
