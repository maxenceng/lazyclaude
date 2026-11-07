package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldFindTheStatusALabelNames() {
        assertThat(Status.find("in-review")).contains(Status.IN_REVIEW);
    }

    @Test
    void shouldFindNoStatusOutsideTheFive() {
        assertThat(Status.find("blocked")).isEmpty();
    }

    @Test
    void shouldFindNoStatusForAMissingLabel() {
        assertThat(Status.find(null)).isEmpty();
    }

    @Test
    void shouldFinishTheLifecycleAtDoneOnly() {
        assertThat(List.of(Status.values())).filteredOn(Status::finished).containsExactly(Status.DONE);
    }

    @Test
    void shouldAllowEachStatusTheVerbsTicketAcceptsForIt() {
        assertThat(List.of(Status.values()))
                .extracting(status -> status.verbs().stream().map(Verb::label).toList())
                .containsExactly(
                        List.of("refine"), List.of("start"), List.of("review", "design"), List.of("respond", "done"), List.of("done"));
    }

    @Test
    void shouldOfferByDefaultOnlyVerbsTheGateAllowsUnderEachStatus() {
        assertThat(List.of(Status.values()))
                .as("the default lists and Verb.mayBeOfferedIn are two sources; a default the gate refuses would never pass config.yml")
                .allSatisfy(status -> assertThat(status.verbs()).allMatch(verb -> verb.mayBeOfferedIn(status)));
    }
}
