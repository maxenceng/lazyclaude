package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VerbTest {

    @Test
    void shouldFindTheVerbALabelNames() {
        assertThat(Verb.find("respond")).contains(Verb.RESPOND);
    }

    @Test
    void shouldFindNoVerbOutsideTheSixTicketAccepts() {
        assertThat(Verb.find("new")).isEmpty();
    }

    @Test
    void shouldOfferRefineOnlyToADraft() {
        assertThat(List.of(Status.values())).filteredOn(Verb.REFINE::mayBeOfferedIn).containsExactly(Status.DRAFT);
    }

    @Test
    void shouldOfferStartOnlyToATodo() {
        assertThat(List.of(Status.values())).filteredOn(Verb.START::mayBeOfferedIn).containsExactly(Status.TODO);
    }

    @Test
    void shouldOfferDesignOnlyToAnInProgressTicket() {
        assertThat(List.of(Status.values())).filteredOn(Verb.DESIGN::mayBeOfferedIn).containsExactly(Status.IN_PROGRESS);
    }

    @ParameterizedTest
    @EnumSource(
            value = Verb.class,
            names = {"REVIEW", "RESPOND"})
    void shouldOfferReviewAndRespondUnderAnyOpenStatus(Verb verb) {
        assertThat(List.of(Status.values()))
                .filteredOn(verb::mayBeOfferedIn)
                .containsExactly(Status.DRAFT, Status.TODO, Status.IN_PROGRESS, Status.IN_REVIEW);
    }

    @Test
    void shouldOfferDoneUnderEveryStatusFinishedIncluded() {
        assertThat(List.of(Status.values())).filteredOn(Verb.DONE::mayBeOfferedIn).containsExactly(Status.values());
    }

    @Test
    void shouldOfferOnlyDoneOnceTheTicketIsFinished() {
        assertThat(List.of(Verb.values()))
                .as("a ticket a merge has closed can still be taken through /ticket done, and through nothing else")
                .filteredOn(verb -> verb.mayBeOfferedIn(Status.DONE))
                .containsExactly(Verb.DONE);
    }
}
