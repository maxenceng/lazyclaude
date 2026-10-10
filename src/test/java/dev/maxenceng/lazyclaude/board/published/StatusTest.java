package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void shouldListTheFiveStatusesInLifecycleOrder() {
        assertThat(List.of(Status.values()))
                .extracting(Status::label)
                .containsExactly("draft", "todo", "in-progress", "in-review", "done");
    }

    @Test
    void shouldParseALabel() {
        assertThat(Status.parse("in-review")).isEqualTo(Status.IN_REVIEW);
    }

    @Test
    void shouldRefuseAStatusOutsideTheFive() {
        assertThatThrownBy(() -> Status.parse("blocked"))
                .isInstanceOf(UnknownStatusException.class)
                .hasMessage("status 'blocked' is not one of draft, todo, in-progress, in-review, done");
    }

    @Test
    void shouldRefuseAMissingStatus() {
        assertThatThrownBy(() -> Status.parse(null)).isInstanceOf(UnknownStatusException.class);
    }

    @Test
    void shouldFinishTheLifecycleAtDoneOnly() {
        assertThat(List.of(Status.values())).filteredOn(Status::finished).containsExactly(Status.DONE);
    }
}
