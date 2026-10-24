package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.tamboui.tui.event.KeyCode;

class CommentBoxTest {

    @Test
    void shouldTypeCharactersAndTakeEnterAsANewline() {
        CommentBox box = new CommentBox(new SummaryTarget(), Optional.empty(), "");

        box.edit(key('a'));
        box.edit(enter());
        box.edit(key('b'));

        assertThat(box.content()).isEqualTo("a\nb");
    }

    @Test
    void shouldDeleteBackwardAndMoveTheCursorBeforeTyping() {
        CommentBox box = new CommentBox(new SummaryTarget(), Optional.empty(), "ab");

        box.edit(namedKey(KeyCode.BACKSPACE));
        box.edit(namedKey(KeyCode.LEFT));
        box.edit(key('x'));

        assertThat(box.content()).isEqualTo("xa");
    }

    @Test
    void shouldStartWithItsCursorAtTheEndOfTheTextItHolds() {
        CommentBox box = new CommentBox(new SummaryTarget(), Optional.empty(), "ab");

        box.edit(key('c'));

        assertThat(box.content()).isEqualTo("abc");
    }

    @Test
    void shouldLeaveSaveAndCancelAndEveryOtherChordToItsCaller() {
        CommentBox box = new CommentBox(new SummaryTarget(), Optional.empty(), "");

        assertThat(box.edit(ctrl('s'))).isFalse();
        assertThat(box.edit(namedKey(KeyCode.ESCAPE))).isFalse();
        assertThat(box.content()).isEmpty();
    }

    @Test
    void shouldHoldStillWhileASaveIsOutAndKeepItsTextWithTheReasonWhenItIsRefused() {
        CommentBox box = new CommentBox(new SummaryTarget(), Optional.empty(), "text");

        box.saveBegun();
        assertThat(box.edit(key('x'))).isFalse();
        box.saveRefused(reason());

        assertThat(box.saving()).isFalse();
        assertThat(box.notSaved()).contains(reason());
        assertThat(box.content()).isEqualTo("text");
        assertThat(box.edit(key('!'))).isTrue();
    }
}
