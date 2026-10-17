package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.elsewhereGroupViewNamed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedLiveSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.namedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewElsewhere;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.addRemoteControlView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.addRemoteControlViewOutsideHome;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.conflictingRemoteControlView;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.jumpedWith;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.outsideOffering;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnElsewhereOutside;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessionsOutside;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellWithSessionsFocused;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionCommand;
import dev.maxenceng.lazyclaude.session.published.Busy;

class ShellJumpTest {

    @Test
    void shouldJumpToTheSelectedOutsideSessionWithOInTheSessionsPanel() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);

        shell.press(key('o'));

        assertThat(jumpedWith(outside).session()).isEqualTo(sessionId());
    }

    @Test
    void shouldHandTheCommandTheProjectAndItsTicketIdsToReadTheSessionsBy() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);

        shell.press(key('o'));

        assertThat(jumpedWith(outside).sessions().root()).isEqualTo(ShellFixture.projectRootPath());
        assertThat(jumpedWith(outside).sessions().ticketIds()).extracting(id -> id.value()).containsExactly("PROJ-1");
    }

    @Test
    void shouldJumpToTheSelectedOutsideSessionWithOInTheElsewherePanel() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnElsewhereOutside(
                sessionsViewElsewhere(elsewhereGroupViewNamed("next-suggestions", namedSessionView(sessionIdOf(7), "refactor", new Busy()))),
                outside);

        shell.press(key('o'));

        assertThat(jumpedWith(outside).session()).isEqualTo(sessionIdOf(7));
    }

    @Test
    void shouldDoNothingWithOOnASessionLazyclaudeHosts() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(hostedLiveSessionView())), outside);

        shell.press(key('o'));

        verifyNoInteractions(outside);
    }

    @Test
    void shouldDoNothingWithOOnAHostedSessionElsewhere() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnElsewhereOutside(
                sessionsViewElsewhere(elsewhereGroupViewNamed("next-suggestions", hostedLiveSessionView())), outside);

        shell.press(key('o'));

        verifyNoInteractions(outside);
    }

    @Test
    void shouldDoNothingWithOOnAPastSession() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnSessionsOutside(pastSessionsViewOf(List.of(pastSessionView())), outside);

        shell.press(key('o'));

        verifyNoInteractions(outside);
    }

    @Test
    void shouldDoNothingWithOOutsideTheSessionsAndElsewherePanels() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);
        shell.press(ShellFixture.alt('k'));

        shell.press(key('o'));

        verifyNoInteractions(outside);
    }

    @Test
    void shouldJumpOffTheRenderThread() {
        HeldTasksFixture held = new HeldTasksFixture();
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        Shell shell = shellWithSessionsFocused(sessionsViewOf(List.of(sessionView())), blankScreens(), outside, held, 1);

        shell.press(key('o'));

        verifyNoInteractions(outside);
        held.runNewestFirst();
        verify(outside).jump(any());
    }

    @Test
    void shouldListTheJumpKeyOnAnOutsideSessionOnly() {
        assertThat(Screen.of(shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), mock(OutsideSessionCommand.class))).lines().getLast())
                .contains("o jump");
        assertThat(Screen.of(shellOnSessionsOutside(sessionsViewOf(List.of(hostedLiveSessionView())), mock(OutsideSessionCommand.class))).lines().getLast())
                .doesNotContain("o jump");
        assertThat(Screen.of(shellOnSessionsOutside(pastSessionsViewOf(List.of(pastSessionView())), mock(OutsideSessionCommand.class))).lines().getLast())
                .doesNotContain("o jump");
    }

    @Test
    void shouldListTheJumpKeyOnASessionElsewhere() {
        Shell shell = shellOnElsewhereOutside(
                sessionsViewElsewhere(elsewhereGroupViewNamed("next-suggestions", namedSessionView(sessionIdOf(7), "refactor", new Busy()))),
                mock(OutsideSessionCommand.class));

        assertThat(Screen.of(shell).lines().getLast()).contains("o jump");
    }

    @Test
    void shouldOpenNoPopupWhenTheJumpWorked() {
        OutsideSessionCommand outside = mock(OutsideSessionCommand.class);
        when(outside.jump(any())).thenReturn(Optional.empty());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);

        shell.press(key('o'));

        assertThat(Screen.of(shell).text()).doesNotContain("remote control");
    }

    @Test
    void shouldOfferTheLinesToAddOverTheMainViewWhenKittyRefusesRemoteControl() {
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outsideOffering(addRemoteControlView()));

        shell.press(key('o'));

        assertThat(Screen.of(shell).text())
                .contains("╭ kitty remote control is off ")
                .contains("│ append to ~/.config/kitty/kitty.conf:")
                .contains("│   # added by lazyclaude")
                .contains("│   allow_remote_control socket-only")
                .contains("│   listen_on unix:@kitty-{kitty_pid}")
                .contains("│ kitty needs a restart afterwards")
                .contains("╰ y add · n cancel ");
    }

    @Test
    void shouldNameAFileOutsideTheHomeInFull() {
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outsideOffering(addRemoteControlViewOutsideHome()));

        shell.press(key('o'));

        assertThat(Screen.of(shell).text()).contains("│ append to /etc/kitty-here/kitty.conf:");
    }

    @Test
    void shouldListTheAnswersToThePopupOnTheKeyBar() {
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outsideOffering(addRemoteControlView()));

        shell.press(key('o'));

        assertThat(Screen.of(shell).lines().getLast()).contains("y add").contains("n/esc cancel").doesNotContain("o jump");
    }

    @Test
    void shouldAddTheLinesWithYAndClosePopup() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);
        shell.press(key('o'));

        shell.press(key('y'));

        verify(outside).addRemoteControl();
        assertThat(Screen.of(shell).text()).doesNotContain("kitty remote control is off");
    }

    @Test
    void shouldAddNothingWithNAndClosePopup() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);
        shell.press(key('o'));

        shell.press(key('n'));

        verify(outside, never()).addRemoteControl();
        assertThat(Screen.of(shell).text()).doesNotContain("kitty remote control is off");
    }

    @Test
    void shouldAddNothingWithEscapeAndClosePopup() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);
        shell.press(key('o'));

        shell.press(escape());

        verify(outside, never()).addRemoteControl();
        assertThat(Screen.of(shell).text()).doesNotContain("kitty remote control is off");
    }

    @Test
    void shouldKeepThePopupOpenAndActOnNothingForAnyOtherKey() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView(), namedSessionView(sessionIdOf(2), "other", new Busy()))), outside);
        shell.press(key('o'));

        shell.press(key('j'));
        shell.press(key('q'));

        assertThat(Screen.of(shell).text()).contains("kitty remote control is off");
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("lazyclaude-b6");
        assertThat(shell.quitRequested()).isFalse();
    }

    @Test
    void shouldOfferAgainOnTheNextJumpAfterARefusedOffer() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);
        shell.press(key('o'));
        shell.press(key('n'));

        shell.press(key('o'));

        assertThat(Screen.of(shell).text()).contains("kitty remote control is off");
    }

    @Test
    void shouldShowTheConflictAndOnlyCloseOnEscape() {
        OutsideSessionCommand outside = outsideOffering(conflictingRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);

        shell.press(key('o'));

        assertThat(Screen.of(shell).text())
                .contains("╭ kitty remote control conflicts ")
                .contains("│ ~/.config/kitty/user.conf line 12 sets")
                .contains("│   allow_remote_control no")
                .contains("│ remote control needs")
                .contains("│   allow_remote_control socket-only")
                .contains("│ nothing was edited")
                .contains("╰ esc close ")
                .doesNotContain("y add");
        assertThat(Screen.of(shell).lines().getLast()).contains("esc close");
        shell.press(key('y'));
        shell.press(key('n'));
        verify(outside, never()).addRemoteControl();
        assertThat(Screen.of(shell).text()).contains("kitty remote control conflicts");
        shell.press(escape());
        assertThat(Screen.of(shell).text()).doesNotContain("kitty remote control conflicts");
    }

    @Test
    void shouldSendTheJumpAndTheAnswerInTheOrderTheyWereMade() {
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellOnSessionsOutside(sessionsViewOf(List.of(sessionView())), outside);

        shell.press(key('o'));
        shell.press(key('y'));

        InOrder sent = inOrder(outside);
        sent.verify(outside).jump(any());
        sent.verify(outside).addRemoteControl();
    }

    @Test
    void shouldAddTheLinesOffTheRenderThread() {
        HeldTasksFixture held = new HeldTasksFixture();
        OutsideSessionCommand outside = outsideOffering(addRemoteControlView());
        Shell shell = shellWithSessionsFocused(sessionsViewOf(List.of(sessionView())), blankScreens(), outside, held, 1);
        shell.press(key('o'));
        held.runNewestFirst();
        shell.press(key('y'));

        verify(outside, never()).addRemoteControl();
        held.runNewestFirst();
        verify(outside).addRemoteControl();
    }
}
