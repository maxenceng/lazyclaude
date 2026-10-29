package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

class ConfigurationTest {

    @Test
    void shouldRefuseToBuildWithoutSayingWhetherDesktopNotificationsAreOn() {
        assertThatThrownBy(() -> Configuration.builder().reservedChords(ReservedChords.defaults()).build())
                .as("a forgotten flag would turn notifications off, the opposite of the default")
                .isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseToBuildWithoutSayingWhetherTheMouseIsOn() {
        assertThatThrownBy(() -> Configuration.builder()
                        .reservedChords(ReservedChords.defaults())
                        .desktopNotifications(true)
                        .build())
                .as("a forgotten flag would leave the mouse in whatever state the Java default is, not the terminal's")
                .isInstanceOf(MissingMandatoryValueException.class);
    }
}
