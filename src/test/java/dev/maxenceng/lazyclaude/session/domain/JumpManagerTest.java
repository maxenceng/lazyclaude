package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.adding;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.addingBoth;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingAllowSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingWith;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnSetting;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.hostedSessionLivingIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionLivingIn;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.emptyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inTmux;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyAndTmuxEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.otherTmuxServerEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.tmuxEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.tmuxPaneOnlyEnvironment;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class JumpManagerTest {

    @Mock
    private TerminalJumpPort jumps;

    @Mock
    private ProcessEnvironmentPort environment;

    @Mock
    private KittyConfigPort config;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private JumpManager manager;

    @Test
    void shouldFocusTheKittyWindowOfASessionInKitty() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());

        assertThat(manager.jump(sessionLivingIn(inKitty()))).isEmpty();

        verify(jumps).jump(inKitty());
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldFocusTheKittyWindowOfASessionWhateverTheTerminalLazyclaudeRunsIn() {
        when(environment.ownEnvironment()).thenReturn(emptyEnvironment());

        manager.jump(sessionLivingIn(inKitty()));

        verify(jumps).jump(inKitty());
    }

    @Test
    void shouldSwitchToThePaneOfASessionInTheTmuxServerLazyclaudeRunsIn() {
        when(environment.ownEnvironment()).thenReturn(tmuxEnvironment());

        assertThat(manager.jump(sessionLivingIn(inTmux()))).isEmpty();

        verify(jumps).jump(inTmux());
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldSwitchToTheTmuxPaneWhenLazyclaudeRunsInTmuxInsideKitty() {
        when(environment.ownEnvironment()).thenReturn(kittyAndTmuxEnvironment());

        manager.jump(sessionLivingIn(inTmux()));

        verify(jumps).jump(inTmux());
    }

    @Test
    void shouldWarnAndDoNothingElseForASessionInTmuxWhenLazyclaudeIsNotInTmux() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());

        assertThat(manager.jump(sessionLivingIn(inTmux()))).isEmpty();

        verifyNoInteractions(jumps, config);
        assertThat(raisedTexts(warnings))
                .containsExactly("cannot jump: the session is in a tmux server lazyclaude is not attached to.");
    }

    @Test
    void shouldWarnForASessionInAnotherTmuxServerThanLazyclaudes() {
        when(environment.ownEnvironment()).thenReturn(otherTmuxServerEnvironment());

        manager.jump(sessionLivingIn(inTmux()));

        verifyNoInteractions(jumps, config);
        assertThat(raisedTexts(warnings))
                .containsExactly("cannot jump: the session is in a tmux server lazyclaude is not attached to.");
    }

    @Test
    void shouldWarnForASessionInTmuxWhoseServerIsUnknown() {
        when(environment.ownEnvironment()).thenReturn(tmuxEnvironment());

        manager.jump(sessionLivingIn(tmuxPaneOnlyEnvironment().residence()));

        verifyNoInteractions(jumps);
        assertThat(raisedTexts(warnings)).hasSize(1);
    }

    @Test
    void shouldWarnForASessionInNeitherKittyNorTmux() {
        when(environment.ownEnvironment()).thenReturn(kittyAndTmuxEnvironment());

        assertThat(manager.jump(sessionLivingIn(new Nowhere()))).isEmpty();

        verifyNoInteractions(jumps, config);
        assertThat(raisedTexts(warnings)).containsExactly("cannot jump: the session lives in neither kitty nor tmux.");
    }

    @Test
    void shouldDoNothingAtAllForASessionLazyclaudeHosts() {
        assertThat(manager.jump(hostedSessionLivingIn(inKitty()))).isEmpty();

        verifyNoInteractions(jumps, config, environment);
        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldWarnWhyWhenTheJumpCommandFails() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new TerminalJumpFailedException("kitten exited with 1: no matching windows")).when(jumps).jump(inKitty());

        assertThat(manager.jump(sessionLivingIn(inKitty()))).isEmpty();

        assertThat(raisedTexts(warnings)).containsExactly("cannot jump: kitten exited with 1: no matching windows");
        verifyNoInteractions(config);
    }

    @Test
    void shouldWarnWhyWhenTheTmuxJumpFails() {
        when(environment.ownEnvironment()).thenReturn(tmuxEnvironment());
        doThrow(new TerminalJumpFailedException("tmux exited with 1: can't find pane")).when(jumps).jump(inTmux());

        manager.jump(sessionLivingIn(inTmux()));

        assertThat(raisedTexts(warnings)).containsExactly("cannot jump: tmux exited with 1: can't find pane");
    }

    @Test
    void shouldOfferBothLinesWhenKittyRefusesAndLazyclaudeRunsInKitty() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).contains(addingBoth());

        assertThat(raisedTexts(warnings)).isEmpty();
    }

    @Test
    void shouldOfferOnlyTheLineNotYetSet() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys()))
                .thenReturn(new KittySettings(List.of(allowRemoteControlSetting())));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).contains(adding(List.of(listenOnLine())));
    }

    @Test
    void shouldOfferAlsoWhenLazyclaudeRunsInTmuxInsideKitty() {
        when(environment.ownEnvironment()).thenReturn(kittyAndTmuxEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).contains(addingBoth());
    }

    @Test
    void shouldShowTheConflictAndOfferNothingToEditWhenAKeyIsSetAnotherWay() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys()))
                .thenReturn(new KittySettings(List.of(conflictingAllowSetting(), listenOnSetting())));

        assertThat(manager.jump(sessionLivingIn(inKitty())))
                .contains(conflictingWith(conflictingAllowSetting(), allowRemoteControlLine()));

        verify(config, never()).append(any(), any());
    }

    @Test
    void shouldWarnThatKittyNeedsARestartWhenBothKeysAreAlreadySet() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys()))
                .thenReturn(new KittySettings(List.of(allowRemoteControlSetting(), listenOnSetting())));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).isEmpty();

        assertThat(raisedTexts(warnings))
                .containsExactly("cannot jump: kitty.conf already allows remote control, so kitty needs a restart.");
    }

    @Test
    void shouldNeverOfferWhenLazyclaudeRunsOutsideKitty() {
        when(environment.ownEnvironment()).thenReturn(emptyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());

        assertThat(manager.jump(sessionLivingIn(inKitty()))).isEmpty();

        verifyNoInteractions(config);
        assertThat(raisedTexts(warnings))
                .containsExactly("cannot jump: kitty remote control is off, and lazyclaude does not run in kitty to offer turning it on.");
    }

    @Test
    void shouldOfferAgainTheNextTimeKittyRefuses() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));
        manager.jump(sessionLivingIn(inKitty()));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).as("a refused offer is not remembered").contains(addingBoth());
    }

    @Test
    void shouldWarnWhyWhenKittyConfigCannotBeRead() {
        when(environment.ownEnvironment()).thenReturn(kittyEnvironment());
        doThrow(new RemoteControlOffException("Failed to connect")).when(jumps).jump(inKitty());
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys()))
                .thenThrow(new KittyConfigUnreadableException("cannot read /home/max/.config/kitty/kitty.conf: access denied", new IOException()));

        assertThat(manager.jump(sessionLivingIn(inKitty()))).isEmpty();

        assertThat(raisedTexts(warnings))
                .containsExactly("cannot jump: cannot read /home/max/.config/kitty/kitty.conf: access denied");
    }
}
