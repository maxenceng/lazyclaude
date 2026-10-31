package dev.maxenceng.lazyclaude.sidecar;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;

class SidecarJsonTest {

    @Test
    void writes_an_enum_constant_as_its_name_whatever_its_text() {
        assertThat(SidecarJson.tree(Weather.SUNNY).asString()).as("the schema lists names, so the writer writes names").isEqualTo("SUNNY");
    }

    @Test
    void reads_an_enum_param_by_its_name_whatever_its_text() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"weather\":\"SUNNY\"}"), WeatherPayload.class).weather())
                .as("the schema lists names, so the reader reads names")
                .isEqualTo(Weather.SUNNY);
    }

    @Test
    void reads_a_path_param_holding_a_colon_as_that_path() {
        assertThat(SidecarJson.params(SidecarJson.parse("{\"from\":\"/tmp/a:b\"}"), BoardPayload.class).from())
                .as("the schema states a path as any string, so a colon in it is not a URI scheme")
                .isEqualTo(Path.of("/tmp/a:b"));
    }
}
