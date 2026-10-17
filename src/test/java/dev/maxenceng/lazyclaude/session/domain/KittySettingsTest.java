package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.adding;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.addingBoth;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingAllowSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingListenSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingWith;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.line;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.settingOf;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KittySettingsTest {

    @Test
    void shouldJumpOverTheSocketTheListenOnLineItAddsMakesKittyListenOn() {
        assertThat(inKitty().socket())
                .as("a jump goes where the offered listen_on line makes kitty listen, so the two cannot drift apart")
                .isEqualTo(new KittySocket("unix:@kitty-1621"));
    }

    @Test
    void shouldAskForBothLinesWhenNeitherKeyIsSet() {
        assertThat(new KittySettings(List.of()).remoteControl(kittyConfigFile())).isEqualTo(addingBoth());
    }

    @Test
    void shouldAskForTheListenLineAloneWhenOnlyRemoteControlIsAllowed() {
        assertThat(new KittySettings(List.of(allowRemoteControlSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(adding(List.of(listenOnLine())));
    }

    @Test
    void shouldAskForTheRemoteControlLineAloneWhenOnlyTheSocketIsListened() {
        assertThat(new KittySettings(List.of(listenOnSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(adding(List.of(allowRemoteControlLine())));
    }

    @Test
    void shouldFindRemoteControlConfiguredWhenBothKeysAreSetToTheWantedValues() {
        assertThat(new KittySettings(List.of(allowRemoteControlSetting(), listenOnSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(new ConfiguredRemoteControl());
    }

    @Test
    void shouldFindAConflictWhenRemoteControlIsAllowedAnotherWay() {
        assertThat(new KittySettings(List.of(conflictingAllowSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(conflictingWith(conflictingAllowSetting(), allowRemoteControlLine()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"socket", "yes", "y", "True"})
    void shouldFindRemoteControlConfiguredWhenItIsAllowedOverTheSocketAnotherWay(String allowing) {
        KittySettings settings = new KittySettings(List.of(settingOf(line("allow_remote_control", allowing), kittyConfigFile(), 1), listenOnSetting()));

        assertThat(settings.remoteControl(kittyConfigFile()))
                .as("kitten @ --to works over the socket with these as with socket-only, so they are no conflict")
                .isEqualTo(new ConfiguredRemoteControl());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no", "password"})
    void shouldFindAConflictWhenRemoteControlIsNotAllowedOverTheSocketWithoutAPassword(String refusing) {
        KittyConfigSetting setting = settingOf(line("allow_remote_control", refusing), kittyConfigFile(), 1);

        assertThat(new KittySettings(List.of(setting, listenOnSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(conflictingWith(setting, allowRemoteControlLine()));
    }

    @Test
    void shouldFindAConflictWhenTheSocketIsListenedAnotherWay() {
        assertThat(new KittySettings(List.of(allowRemoteControlSetting(), conflictingListenSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(conflictingWith(conflictingListenSetting(), listenOnLine()));
    }

    @Test
    void shouldReportTheRemoteControlConflictFirstWhenBothKeysConflict() {
        assertThat(new KittySettings(List.of(conflictingListenSetting(), conflictingAllowSetting())).remoteControl(kittyConfigFile()))
                .isEqualTo(conflictingWith(conflictingAllowSetting(), allowRemoteControlLine()));
    }

    @Test
    void shouldLetALaterLineWinOverAnEarlierOneAsKittyDoes() {
        KittySettings overridden = new KittySettings(List.of(
                settingOf(line("allow_remote_control", "no"), kittyConfigFile(), 1), allowRemoteControlSetting(), listenOnSetting()));

        assertThat(overridden.remoteControl(kittyConfigFile())).isEqualTo(new ConfiguredRemoteControl());
    }

    @Test
    void shouldFindAConflictWhenALaterLineOverridesTheWantedValue() {
        KittyConfigSetting later = settingOf(line("allow_remote_control", "no"), kittyConfigFile(), 40);

        assertThat(new KittySettings(List.of(allowRemoteControlSetting(), listenOnSetting(), later)).remoteControl(kittyConfigFile()))
                .isEqualTo(conflictingWith(later, allowRemoteControlLine()));
    }

    @Test
    void shouldIgnoreASettingOfAnyOtherKey() {
        assertThat(new KittySettings(List.of(settingOf(line("font_size", "12"), kittyConfigFile(), 2))).remoteControl(kittyConfigFile()))
                .isEqualTo(addingBoth());
    }

    @Test
    void shouldAskTheConfigForTheTwoKeysItDecidesFrom() {
        assertThat(KittySettings.remoteControlKeys()).containsExactly(allowRemoteControlLine().key(), listenOnLine().key());
    }
}
