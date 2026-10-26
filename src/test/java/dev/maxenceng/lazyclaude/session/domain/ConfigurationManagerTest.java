package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.bindingsClashingTwice;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.defaultConfiguration;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.refusal;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.unreadableKeybindings;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altW;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class ConfigurationManagerTest {

    @Mock
    private ConfigurationPort configurations;

    @Mock
    private ClaudeKeybindingsPort keybindings;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private ConfigurationManager manager;

    @Test
    void shouldHandOverTheConfigurationTheFileGives() {
        when(configurations.configuration()).thenReturn(rebindingFocusLeft());
        when(keybindings.bindings()).thenReturn(List.of());

        assertThat(manager.configuration()).isEqualTo(rebindingFocusLeft());
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldRefuseWithoutReadingClaudesBindingsWhenTheConfigurationIsRefused() {
        when(configurations.configuration()).thenThrow(refusal());

        assertThatThrownBy(manager::configuration).isInstanceOf(ConfigurationRefusedException.class);
        verifyNoInteractions(keybindings, warnings);
    }

    @Test
    void shouldRaiseOneWarningListingEveryClash() {
        when(configurations.configuration()).thenReturn(defaultConfiguration());
        when(keybindings.bindings()).thenReturn(bindingsClashingTwice());

        manager.configuration();

        assertThat(raisedTexts(warnings))
                .hasSize(1)
                .allSatisfy(text -> assertThat(text).contains("alt+h", "focusLeft", "alt+z", "zoom"));
    }

    @Test
    void shouldNotWarnOfABindingOnAChordThatWasRebound() {
        when(configurations.configuration()).thenReturn(rebindingFocusLeft());
        when(keybindings.bindings()).thenReturn(List.of(altH(), altW()));

        manager.configuration();

        assertThat(raisedTexts(warnings)).hasSize(1).allSatisfy(text -> assertThat(text).doesNotContain("focusLeft").contains("alt+w"));
    }

    @Test
    void shouldWarnThatTheCheckWasSkippedAndCarryOnWhenTheBindingsCannotBeRead() {
        when(configurations.configuration()).thenReturn(defaultConfiguration());
        when(keybindings.bindings()).thenThrow(unreadableKeybindings());

        assertThat(manager.configuration()).isEqualTo(defaultConfiguration());
        assertThat(raisedTexts(warnings))
                .hasSize(1)
                .allSatisfy(text -> assertThat(text).contains("skipped", "access denied"));
    }
}
