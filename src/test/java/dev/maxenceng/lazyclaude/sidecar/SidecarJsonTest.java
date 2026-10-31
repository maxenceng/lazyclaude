package dev.maxenceng.lazyclaude.sidecar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SidecarJsonTest {

    @Test
    void writes_an_enum_constant_as_its_name_whatever_its_text() {
        assertThat(SidecarJson.tree(Weather.SUNNY).asString()).as("the schema lists names, so the writer writes names").isEqualTo("SUNNY");
    }
}
