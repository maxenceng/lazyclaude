package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.StatusVerbsFixture.inReviewDoneThenRespond;
import static dev.maxenceng.lazyclaude.board.published.StatusVerbsFixture.todoWithoutVerbs;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;

class OfferedVerbsTest {

    @Test
    void shouldOfferEachStatusItsDefaultVerbsWhenNothingIsConfigured() {
        OfferedVerbs offered = new OfferedVerbs(List.of());

        assertThat(List.of(Status.values())).allSatisfy(status -> assertThat(offered.of(status)).isEqualTo(status.verbs()));
    }

    @Test
    void shouldOfferTheConfiguredVerbsInTheirOrder() {
        assertThat(new OfferedVerbs(List.of(inReviewDoneThenRespond())).of(Status.IN_REVIEW))
                .containsExactly(Verb.DONE, Verb.RESPOND);
    }

    @Test
    void shouldKeepTheDefaultVerbsOfAStatusTheConfigurationLeavesOut() {
        assertThat(new OfferedVerbs(List.of(inReviewDoneThenRespond())).of(Status.DRAFT)).containsExactly(Verb.REFINE);
    }

    @Test
    void shouldOfferNoVerbToAStatusConfiguredEmpty() {
        assertThat(new OfferedVerbs(List.of(todoWithoutVerbs())).of(Status.TODO)).isEmpty();
    }
}
