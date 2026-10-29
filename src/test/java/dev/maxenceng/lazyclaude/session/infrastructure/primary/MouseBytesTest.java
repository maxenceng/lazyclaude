package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.leftPressAt;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.mouse;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.wheelUpAt;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SentBytesFixture.sent;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jediterm.terminal.emulator.mouse.MouseMode;

import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.MouseAction;
import dev.maxenceng.lazyclaude.session.published.PointerButton;

class MouseBytesTest {

    @Test
    void shouldSendNothingWhileTheChildReportsNoMouse() {
        assertThat(MouseBytes.of(leftPressAt(4, 2), MouseMode.MOUSE_REPORTING_NONE)).isEmpty();
    }

    @Test
    void shouldSendAPressAtTheOneBasedCell() {
        assertThat(sent(MouseBytes.of(leftPressAt(4, 2), MouseMode.MOUSE_REPORTING_NORMAL)))
                .as("zero-based cell (4,2) is column 5, row 3 on the wire")
                .isEqualTo("\u001b[<0;5;3M");
    }

    @Test
    void shouldSendAReleaseWithALowercaseM() {
        assertThat(sent(MouseBytes.of(mouse(MouseAction.RELEASE, PointerButton.LEFT, 0, 0), MouseMode.MOUSE_REPORTING_NORMAL)))
                .isEqualTo("\u001b[<0;1;1m");
    }

    @Test
    void shouldSendTheMiddleAndRightButtons() {
        assertThat(sent(MouseBytes.of(mouse(MouseAction.PRESS, PointerButton.MIDDLE, 0, 0), MouseMode.MOUSE_REPORTING_NORMAL)))
                .isEqualTo("\u001b[<1;1;1M");
        assertThat(sent(MouseBytes.of(mouse(MouseAction.PRESS, PointerButton.RIGHT, 0, 0), MouseMode.MOUSE_REPORTING_NORMAL)))
                .isEqualTo("\u001b[<2;1;1M");
    }

    @Test
    void shouldSendTheWheelAsButtonsSixtyFourAndUp() {
        assertThat(sent(MouseBytes.of(wheelUpAt(10, 5), MouseMode.MOUSE_REPORTING_NORMAL))).isEqualTo("\u001b[<64;11;6M");
        assertThat(sent(MouseBytes.of(mouse(MouseAction.SCROLL_DOWN, PointerButton.NONE, 10, 5), MouseMode.MOUSE_REPORTING_NORMAL)))
                .isEqualTo("\u001b[<65;11;6M");
    }

    @Test
    void shouldAddTheModifierBits() {
        EmulatorMousePayload withCtrl = EmulatorMousePayload.builder()
                .action(MouseAction.PRESS)
                .button(PointerButton.LEFT)
                .column(new CellColumn(0))
                .row(new CellRow(0))
                .modifiers(Modifiers.of(Modifier.CTRL, Modifier.SHIFT))
                .build();

        assertThat(sent(MouseBytes.of(withCtrl, MouseMode.MOUSE_REPORTING_NORMAL))).isEqualTo("\u001b[<20;1;1M");
    }

    @Test
    void shouldSendADragOnlyToAChildThatAskedForButtonMotion() {
        EmulatorMousePayload drag = mouse(MouseAction.DRAG, PointerButton.LEFT, 1, 1);

        assertThat(MouseBytes.of(drag, MouseMode.MOUSE_REPORTING_NORMAL))
                .as("a child that asked only for clicks would read motion as garbage")
                .isEmpty();
        assertThat(sent(MouseBytes.of(drag, MouseMode.MOUSE_REPORTING_BUTTON_MOTION))).isEqualTo("\u001b[<32;2;2M");
    }

    @Test
    void shouldSendBareMotionOnlyToAChildThatAskedForAllMotion() {
        EmulatorMousePayload move = mouse(MouseAction.MOVE, PointerButton.NONE, 1, 1);

        assertThat(MouseBytes.of(move, MouseMode.MOUSE_REPORTING_BUTTON_MOTION)).isEmpty();
        assertThat(sent(MouseBytes.of(move, MouseMode.MOUSE_REPORTING_ALL_MOTION))).isEqualTo("\u001b[<35;2;2M");
    }
}
