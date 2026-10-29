package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenViewFixture.run;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenViewFixture.screenSaying;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenViewFixture.screenSayingWithCursorAt;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenViewFixture.styled;
import static dev.maxenceng.lazyclaude.shell.EmulatorScreenElementFixture.elementOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorPastePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorScreenView;
import dev.maxenceng.lazyclaude.session.published.DefaultColour;
import dev.maxenceng.lazyclaude.session.published.IndexedColour;
import dev.maxenceng.lazyclaude.session.published.NoCursor;
import dev.maxenceng.lazyclaude.session.published.RgbColour;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.TextAttribute;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.PasteEvent;

class EmulatorScreenElementTest {

    @SuppressWarnings("unchecked")
    private final Consumer<EmulatorPastePayload> onPaste = mock(Consumer.class);

    @Test
    void shouldDrawEachRowAtItsLine() {
        Screen screen = Screen.of(elementOf(screenSaying("first", "second"), false, onPaste), 12, 3);

        assertThat(screen.lines()).containsExactly("first       ", "second      ", "            ");
    }

    @Test
    void shouldDrawTheRunsOfARowSideBySide() {
        EmulatorScreenView runs = EmulatorScreenView.builder()
                .rows(List.of(new ScreenRow(List.of(
                        run("ab", styled(new DefaultColour())), run("cd", styled(new IndexedColour(1)))))))
                .build();

        assertThat(Screen.of(elementOf(runs, false, onPaste), 6, 1).lines()).containsExactly("abcd  ");
    }

    @Test
    void shouldDrawAnIndexedColourAsThePaletteEntry() {
        EmulatorScreenView red = EmulatorScreenView.builder()
                .rows(List.of(new ScreenRow(List.of(run("red", styled(new IndexedColour(1)))))))
                .build();

        assertThat(Screen.of(elementOf(red, false, onPaste), 6, 1).foregroundText(Color.indexed(1))).containsExactly("red");
    }

    @Test
    void shouldDrawATruecolorAsItsRgb() {
        EmulatorScreenView orange = EmulatorScreenView.builder()
                .rows(List.of(new ScreenRow(List.of(run("orange", styled(new RgbColour(0xFF6400)))))))
                .build();

        assertThat(Screen.of(elementOf(orange, false, onPaste), 8, 1).foregroundText(Color.rgb(255, 100, 0)))
                .containsExactly("orange");
    }

    @Test
    void shouldDrawTheAttributesOfARun() {
        EmulatorScreenView dimmed = EmulatorScreenView.builder()
                .rows(List.of(new ScreenRow(List.of(run("faint", styled(new DefaultColour(), TextAttribute.DIM))))))
                .build();

        assertThat(Screen.of(elementOf(dimmed, false, onPaste), 8, 1).dimText()).containsExactly("faint");
    }

    @Test
    void shouldDrawReversedAndLeaveThePlainRunAlone() {
        EmulatorScreenView mixed = EmulatorScreenView.builder()
                .rows(List.of(new ScreenRow(List.of(
                        run("plain ", styled(new DefaultColour())),
                        run("picked", styled(new DefaultColour(), TextAttribute.REVERSED))))))
                .build();

        assertThat(Screen.of(elementOf(mixed, false, onPaste), 12, 1).reversedText()).containsExactly("picked");
    }

    @Test
    void shouldNotDrawBeyondTheAreaItWasGiven() {
        Screen screen = Screen.of(elementOf(screenSaying("abcdefgh", "ijklmnop", "qrstuvwx"), false, onPaste), 10, 3, new Rect(1, 0, 4, 2));

        assertThat(screen.lines()).containsExactly(" abcd     ", " ijkl     ", "          ");
    }

    @Test
    void shouldNotSplitAWideCharacterAtTheEdgeOfTheArea() {
        Screen screen = Screen.of(elementOf(screenSaying("a你好"), false, onPaste), 6, 1, new Rect(0, 0, 4, 1));

        assertThat(screen.lines().getFirst().stripTrailing()).isEqualTo("a你");
    }

    @Test
    void shouldPlaceTheCursorOnItsCellOfTheAreaWhenFocused() {
        Screen screen = Screen.of(elementOf(screenSayingWithCursorAt(3, 1, "abcdef", "ghijkl"), true, onPaste), 20, 5, new Rect(4, 2, 10, 3));

        assertThat(screen.cursor()).contains(new Position(7, 3));
    }

    @Test
    void shouldLeaveTheCursorAloneWhenNotFocused() {
        Screen screen = Screen.of(elementOf(screenSayingWithCursorAt(3, 1, "abcdef", "ghijkl"), false, onPaste), 20, 5);

        assertThat(screen.cursor()).isEmpty();
    }

    @Test
    void shouldLeaveTheCursorAloneWhenTheSessionHidIt() {
        EmulatorScreenView hidden = EmulatorScreenView.builder()
                .rows(screenSaying("abc").rows())
                .cursor(new NoCursor())
                .build();

        assertThat(Screen.of(elementOf(hidden, true, onPaste), 20, 5).cursor()).isEmpty();
    }

    @Test
    void shouldLeaveTheCursorAloneWhenItIsBeyondTheArea() {
        Screen screen = Screen.of(elementOf(screenSayingWithCursorAt(9, 0, "abc"), true, onPaste), 20, 5, new Rect(0, 0, 5, 3));

        assertThat(screen.cursor()).isEmpty();
    }

    @Test
    void shouldHandAPasteOnAsAPastePayload() {
        EventResult result = elementOf(screenSaying("x"), true, onPaste).handlePasteEvent(new PasteEvent("one\ntwo"));

        verify(onPaste).accept(new EmulatorPastePayload("one\ntwo"));
        assertThat(result.isHandled()).isTrue();
    }

    @Test
    void shouldBeFocusableSoThatThePasteReachesIt() {
        EmulatorScreenElement element = elementOf(screenSaying("x"), true, onPaste);

        assertThat(element.isFocusable()).isTrue();
        assertThat(element.id()).isNotBlank();
    }
}
