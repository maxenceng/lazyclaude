package dev.maxenceng.lazyclaude.session.domain;

import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

public final class KittyConfigFixture {

    private KittyConfigFixture() {}

    public static KittyConfigFile kittyConfigFile() {
        return new KittyConfigFile(Path.of("/home/max/.config/kitty/kitty.conf"));
    }

    public static KittyConfigFile includedConfigFile() {
        return new KittyConfigFile(Path.of("/home/max/.config/kitty/user.conf"));
    }

    public static KittyConfigLine allowRemoteControlLine() {
        return line("allow_remote_control", "socket-only");
    }

    public static KittyConfigLine listenOnLine() {
        return line("listen_on", "unix:@kitty-{kitty_pid}");
    }

    public static List<KittyConfigLine> bothLines() {
        return List.of(allowRemoteControlLine(), listenOnLine());
    }

    public static KittyConfigLine line(String key, String value) {
        return KittyConfigLine.builder().key(new KittyConfigKey(key)).value(new KittyConfigValue(value)).build();
    }

    public static KittyConfigSetting settingOf(KittyConfigLine line, KittyConfigFile file, int number) {
        return KittyConfigSetting.builder()
                .line(line)
                .place(KittyConfigPlace.builder().file(file).line(new KittyConfigLineNumber(number)).build())
                .build();
    }

    public static KittyConfigSetting allowRemoteControlSetting() {
        return settingOf(allowRemoteControlLine(), kittyConfigFile(), 4);
    }

    public static KittyConfigSetting listenOnSetting() {
        return settingOf(listenOnLine(), kittyConfigFile(), 5);
    }

    public static void settingsRead(KittyConfigPort config, KittyConfigSetting... settings) {
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of(settings)));
    }

    public static KittyConfigSetting conflictingAllowSetting() {
        return settingOf(line("allow_remote_control", "no"), includedConfigFile(), 12);
    }

    public static KittyConfigSetting conflictingListenSetting() {
        return settingOf(line("listen_on", "unix:/tmp/kitty"), kittyConfigFile(), 9);
    }

    public static RemoteControlConfig addingBoth() {
        return adding(bothLines());
    }

    public static AddRemoteControl adding(List<KittyConfigLine> lines) {
        return addingIn(kittyConfigFile(), lines);
    }

    public static AddRemoteControl addingIn(KittyConfigFile file, List<KittyConfigLine> lines) {
        return AddRemoteControl.builder().file(file).lines(lines).build();
    }

    public static RemoteControlConfig conflictingWith(KittyConfigSetting found, KittyConfigLine wanted) {
        return ConflictingRemoteControl.builder().found(found).wanted(wanted).build();
    }
}
