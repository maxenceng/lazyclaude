package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class FileNameTest {

    @Test
    void shouldOrderAsTheNamesDo() {
        assertThat(new FileName("a.md")).isLessThan(new FileName("b.md"));
    }

    @Test
    void shouldRefuseABlankName() {
        assertThatThrownBy(() -> new FileName(" ")).isInstanceOf(BlankValueException.class);
    }
}
