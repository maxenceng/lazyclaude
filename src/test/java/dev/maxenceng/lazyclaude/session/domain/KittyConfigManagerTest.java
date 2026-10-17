package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.adding;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.bothLines;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingAllowSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.settingsRead;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class KittyConfigManagerTest {

    @Mock
    private KittyConfigPort config;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private KittyConfigManager manager;

    @Test
    void shouldAppendBothLinesAndSayKittyNeedsARestart() {
        settingsRead(config);

        manager.add();

        verify(config).append(adding(bothLines()));
        assertThat(raisedTexts(warnings)).containsExactly("kitty.conf updated; restart kitty to turn remote control on.");
    }

    @Test
    void shouldAppendOnlyTheLineNotYetSet() {
        settingsRead(config, allowRemoteControlSetting());

        manager.add();

        verify(config).append(adding(List.of(listenOnLine())));
    }

    @Test
    void shouldEditNothingWhenAKeyHasBeenSetAnotherWaySinceTheOffer() {
        settingsRead(config, conflictingAllowSetting());

        manager.add();

        verify(config, never()).append(any());
        assertThat(raisedTexts(warnings))
                .containsExactly("kitty.conf not changed: line 12 of /home/max/.config/kitty/user.conf sets allow_remote_control to something else.");
    }

    @Test
    void shouldEditNothingAndSayKittyNeedsARestartWhenBothKeysAreSet() {
        settingsRead(config, allowRemoteControlSetting(), listenOnSetting());

        manager.add();

        verify(config, never()).append(any());
        assertThat(raisedTexts(warnings)).containsExactly("kitty.conf already allows remote control, so kitty needs a restart.");
    }

    @Test
    void shouldWarnWhyWhenTheWriteFails() {
        settingsRead(config);
        doThrow(new KittyConfigUnwritableException("cannot write /home/max/.config/kitty/kitty.conf: access denied", new IOException()))
                .when(config)
                .append(any());

        manager.add();

        assertThat(raisedTexts(warnings))
                .containsExactly("cannot update kitty.conf: cannot write /home/max/.config/kitty/kitty.conf: access denied");
    }

    @Test
    void shouldWarnWhyWhenTheConfigCannotBeRead() {
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys()))
                .thenThrow(new KittyConfigUnreadableException("cannot read /home/max/.config/kitty/kitty.conf: access denied", new IOException()));

        manager.add();

        verify(config, never()).append(any());
        assertThat(raisedTexts(warnings))
                .containsExactly("cannot update kitty.conf: cannot read /home/max/.config/kitty/kitty.conf: access denied");
    }
}
