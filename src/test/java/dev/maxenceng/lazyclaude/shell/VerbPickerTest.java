package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceViewFixture.donePreselected;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceViewFixture.respondPreselected;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;

class VerbPickerTest {

    @Test
    void shouldStartTheCursorOnThePreselectedVerb() {
        assertThat(new VerbPicker(ticketViewIn(Status.IN_REVIEW), donePreselected(), false).selected()).isEqualTo(Verb.DONE);
    }

    @Test
    void shouldMoveTheCursorDownAndUp() {
        VerbPicker picker = new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), false);

        picker.down();
        assertThat(picker.selected()).isEqualTo(Verb.DONE);
        picker.up();
        assertThat(picker.selected()).isEqualTo(Verb.RESPOND);
    }

    @Test
    void shouldStopTheCursorAtEitherEnd() {
        VerbPicker picker = new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), false);

        picker.up();
        assertThat(picker.selected()).isEqualTo(Verb.RESPOND);
        picker.down();
        picker.down();
        assertThat(picker.selected()).isEqualTo(Verb.DONE);
    }

    @Test
    void shouldMoveTheCursorToAnotherPreselectedVerbWhileItHasNotBeenMoved() {
        VerbPicker picker = new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), true);

        picker.preselecting(donePreselected());

        assertThat(picker.selected()).as("the PR check landed before anyone touched the cursor").isEqualTo(Verb.DONE);
    }

    @Test
    void shouldKeepTheCursorWhereItWasMovedWhenAnotherVerbIsPreselected() {
        VerbPicker picker = new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), true);
        picker.down();
        picker.up();

        picker.preselecting(donePreselected());

        assertThat(picker.selected())
                .as("a cursor that was moved, even back to where it began, is the person's choice")
                .isEqualTo(Verb.RESPOND);
    }

    @Test
    void shouldReadCheckingWhileAnInReviewTicketsPullRequestIsPending() {
        assertThat(new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), true).checking()).isTrue();
    }

    @Test
    void shouldNotReadCheckingOnceTheMergedReadLanded() {
        VerbPicker picker = new VerbPicker(ticketViewIn(Status.IN_REVIEW), respondPreselected(), true);

        picker.preselecting(respondPreselected());
        picker.checked();

        assertThat(picker.checking()).isFalse();
    }

    @Test
    void shouldNotReadCheckingForATicketWhoseVerbsDoNotDependOnItsPullRequest() {
        assertThat(new VerbPicker(ticketViewIn(Status.IN_PROGRESS), respondPreselected(), true).checking()).isFalse();
    }
}
