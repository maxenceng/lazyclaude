package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.otherWarning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.twoWarnings;
import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.warnings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.EmptyValueException;

class WarningsTest {

    @Test
    void shouldCountOneWarning() {
        assertThat(warnings().count()).isEqualTo(1);
    }

    @Test
    void shouldCountEveryWarningRaised() {
        assertThat(twoWarnings().count()).isEqualTo(2);
    }

    @Test
    void shouldTakeTheLastRaisedAsTheNewest() {
        assertThat(twoWarnings().newest()).isEqualTo(otherWarning());
    }

    @Test
    void shouldTakeTheOnlyWarningAsTheNewest() {
        assertThat(warnings().newest()).isEqualTo(warning());
    }

    @Test
    void shouldRefuseNoWarnings() {
        assertThatThrownBy(() -> new Warnings(List.of())).isInstanceOf(EmptyValueException.class);
    }
}
