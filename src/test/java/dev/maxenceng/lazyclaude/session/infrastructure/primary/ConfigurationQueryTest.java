package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.defaultConfiguration;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.refusal;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withoutDesktopNotifications;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.defaultConfigurationView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.reboundConfigurationView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.application.ConfigurationApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ClaudeKeybindingsPort;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationPort;
import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

class ConfigurationQueryTest {

    private ConfigurationPort configurations;
    private ConfigurationQuery query;

    @BeforeEach
    void setUp() {
        configurations = mock(ConfigurationPort.class);
        ClaudeKeybindingsPort keybindings = mock(ClaudeKeybindingsPort.class);
        when(keybindings.bindings()).thenReturn(List.of());
        query = new ConfigurationQuery(new ConfigurationApplicationService(configurations, keybindings, mock(WarningPort.class)));
    }

    @Test
    void shouldAnswerTheReservedChordsAndWhetherDesktopNotificationsAreOn() {
        when(configurations.configuration()).thenReturn(rebindingFocusLeft());

        assertThat(query.configuration())
                .isEqualTo(reboundConfigurationView());
    }

    @Test
    void shouldAnswerTheDefaults() {
        when(configurations.configuration()).thenReturn(defaultConfiguration());

        assertThat(query.configuration()).isEqualTo(defaultConfigurationView());
    }

    @Test
    void shouldAnswerThatDesktopNotificationsAreOff() {
        when(configurations.configuration()).thenReturn(withoutDesktopNotifications());

        assertThat(query.configuration().desktopNotifications()).isFalse();
    }

    @Test
    void shouldLetTheRefusalReachTheCaller() {
        when(configurations.configuration()).thenThrow(refusal());

        assertThatThrownBy(query::configuration).isInstanceOf(ConfigurationRefusedException.class);
    }
}
