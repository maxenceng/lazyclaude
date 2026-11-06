package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.EmptyValueException;
import dev.maxenceng.lazyclaude.session.published.MergeState;

class VerbChoiceTest {

    private static final boolean DESIGN_RUN = true;
    private static final boolean NO_DESIGN_RUN = false;
    private static final MergeState MERGED = MergeState.MERGED;
    private static final MergeState NOT_MERGED = MergeState.NOT_MERGED;

    @Test
    void shouldPreselectTheOnlyVerbAllowed() {
        assertThat(VerbChoice.of(List.of(Verb.REFINE), NO_DESIGN_RUN, NOT_MERGED).preselected()).isEqualTo(Verb.REFINE);
    }

    @Test
    void shouldPreselectDesignWhenOfferedAndNoDesignSessionExists() {
        assertThat(VerbChoice.of(List.of(Verb.REVIEW, Verb.DESIGN), NO_DESIGN_RUN, NOT_MERGED).preselected())
                .isEqualTo(Verb.DESIGN);
    }

    @Test
    void shouldPreselectReviewWhenADesignSessionExists() {
        assertThat(VerbChoice.of(List.of(Verb.REVIEW, Verb.DESIGN), DESIGN_RUN, NOT_MERGED).preselected())
                .isEqualTo(Verb.REVIEW);
    }

    @Test
    void shouldPreselectReviewWhenDesignIsNotOffered() {
        assertThat(VerbChoice.of(List.of(Verb.REVIEW), NO_DESIGN_RUN, NOT_MERGED).preselected())
                .as("no design session is no reason to offer design when the branch has no frontend change")
                .isEqualTo(Verb.REVIEW);
    }

    @Test
    void shouldPreselectDoneWhenTheTicketBranchIsMerged() {
        assertThat(VerbChoice.of(List.of(Verb.RESPOND, Verb.DONE), NO_DESIGN_RUN, MERGED).preselected())
                .isEqualTo(Verb.DONE);
    }

    @Test
    void shouldPreselectRespondWhenTheTicketBranchIsNotMerged() {
        assertThat(VerbChoice.of(List.of(Verb.RESPOND, Verb.DONE), NO_DESIGN_RUN, NOT_MERGED).preselected())
                .isEqualTo(Verb.RESPOND);
    }

    @Test
    void shouldNotLetAMergedBranchPreselectAVerbThatIsNotOffered() {
        assertThat(VerbChoice.of(List.of(Verb.REVIEW), DESIGN_RUN, MERGED).preselected()).isEqualTo(Verb.REVIEW);
    }

    @Test
    void shouldKeepTheVerbsInTheOrderTheyWereAllowed() {
        assertThat(VerbChoice.of(List.of(Verb.RESPOND, Verb.DONE), NO_DESIGN_RUN, MERGED).verbs())
                .containsExactly(Verb.RESPOND, Verb.DONE);
    }

    @Test
    void shouldNotPreselectDoneWhileWhetherTheBranchIsMergedIsUnknown() {
        assertThat(VerbChoice.of(List.of(Verb.RESPOND, Verb.DONE), NO_DESIGN_RUN, MergeState.UNKNOWN).preselected())
                .as("done is preselected only on a branch known to be merged")
                .isEqualTo(Verb.RESPOND);
    }

    @Test
    void shouldPreselectTheFirstVerbListedWhenAStatusOrdersDoneAheadOfRespond() {
        assertThat(VerbChoice.of(List.of(Verb.DONE, Verb.RESPOND), NO_DESIGN_RUN, NOT_MERGED).preselected())
                .isEqualTo(Verb.DONE);
    }

    @Test
    void shouldLetDesignWinOverTheFirstVerbListedWhileNoDesignSessionExists() {
        assertThat(VerbChoice.of(List.of(Verb.REVIEW, Verb.DESIGN), NO_DESIGN_RUN, MERGED).preselected())
                .isEqualTo(Verb.DESIGN);
    }

    @Test
    void shouldLetAMergedBranchWinDoneOverAFirstVerbListed() {
        assertThat(VerbChoice.of(List.of(Verb.RESPOND, Verb.REVIEW, Verb.DONE), DESIGN_RUN, MERGED).preselected())
                .isEqualTo(Verb.DONE);
    }

    @Test
    void shouldRefuseAChoiceOfNoVerb() {
        assertThatThrownBy(() -> VerbChoice.of(List.of(), NO_DESIGN_RUN, NOT_MERGED))
                .isInstanceOf(EmptyValueException.class);
    }
}
