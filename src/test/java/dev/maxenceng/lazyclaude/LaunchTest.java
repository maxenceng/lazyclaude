package dev.maxenceng.lazyclaude;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class LaunchTest {

    @Test
    void shouldLaunchTheShellWithoutAFlag() {
        assertThat(Launch.from(List.of())).isInstanceOf(ShellLaunch.class);
    }

    @Test
    void shouldLaunchTheSidecarOnTheSidecarFlag() {
        assertThat(Launch.from(List.of("--sidecar"))).isInstanceOf(SidecarLaunch.class);
    }

    @Test
    void shouldPrintTheSchemaOnTheProtocolSchemaFlagEvenWithTheSidecarOne() {
        assertThat(Launch.from(List.of("--sidecar", "--protocol-schema")))
                .as("the schema builds nothing, so it wins over a launch that would")
                .isInstanceOf(SchemaLaunch.class);
    }
}
