package dev.maxenceng.lazyclaude.review.published;

import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightRange;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AfterBoundException;

class PlacedThreadTest {

    @Test
    void shouldRefuseARangeThatStartsAfterItsLastLine() {
        assertThatThrownBy(() -> rightRange(18, 12))
                .as("a thread shows under its last line, so a range read backwards would show above where it starts")
                .isInstanceOf(AfterBoundException.class);
    }
}
