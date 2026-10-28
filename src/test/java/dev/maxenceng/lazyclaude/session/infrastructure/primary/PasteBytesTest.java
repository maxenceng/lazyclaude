package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SentBytesFixture.sentAsUtf8;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalInputFixture.pasted;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasteBytesTest {

    @Test
    void shouldWrapThePasteInBracketsWhileTheChildHasBracketedPasteOn() {
        assertThat(sentAsUtf8(PasteBytes.of(pasted("one\ntwo"), true))).isEqualTo("\u001b[200~one\ntwo\u001b[201~");
    }

    @Test
    void shouldKeepNewlinesAsTheyAreInsideBrackets() {
        assertThat(sentAsUtf8(PasteBytes.of(pasted("a\r\nb"), true)))
                .as("inside brackets a newline is text, so it must not become Enter")
                .contains("a\r\nb");
    }

    @Test
    void shouldDropAClosingBracketTheTextCarriesSoThePasteCannotEndEarly() {
        assertThat(sentAsUtf8(PasteBytes.of(pasted("a\u001b[201~rm -rf /"), true))).isEqualTo("\u001b[200~arm -rf /\u001b[201~");
    }

    @Test
    void shouldTypeThePasteWithCarriageReturnsWhenTheChildHasNoBracketedPaste() {
        assertThat(sentAsUtf8(PasteBytes.of(pasted("one\ntwo\r\nthree"), false))).isEqualTo("one\rtwo\rthree");
    }
}
