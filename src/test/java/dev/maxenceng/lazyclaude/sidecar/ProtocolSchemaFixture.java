package dev.maxenceng.lazyclaude.sidecar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.jackson.databind.JsonNode;

public final class ProtocolSchemaFixture {

    private static final Path COMMITTED = Path.of("docs", "protocol", "protocol.schema.json");

    private ProtocolSchemaFixture() {}

    public static JsonNode protocolSchema() {
        return SidecarJson.parse(Sidecar.protocolSchema());
    }

    public static JsonNode definition(String name) {
        return protocolSchema().path("$defs").path(name);
    }

    public static String schemaOf(ProtocolMethod<?, ?> method) {
        return ProtocolSchema.text(new Protocol(List.of(method), List.of(), List.of()));
    }

    public static JsonNode definitionOf(ProtocolMethod<?, ?> method, String name) {
        return SidecarJson.parse(schemaOf(method)).path("$defs").path(name);
    }

    public static String committedProtocolSchema() throws IOException {
        return Files.readString(COMMITTED);
    }
}
