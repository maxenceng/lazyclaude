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

    public static ConfigLine allowRemoteControlLine() {
        return line("allow_remote_control", "socket-only");
    }

    public static ConfigLine listenOnLine() {
        return line("listen_on", "unix:@kitty-{kitty_pid}");
    }

    public static List<ConfigLine> bothLines() {
        return List.of(allowRemoteControlLine(), listenOnLine());
    }

    public static ConfigLine line(String key, String value) {
        return ConfigLine.builder().key(new ConfigKey(key)).value(new ConfigValue(value)).build();
    }

    public static ConfigSetting settingOf(ConfigLine line, KittyConfigFile file, int number) {
        return ConfigSetting.builder()
                .line(line)
                .place(ConfigPlace.builder().file(file).line(new ConfigLineNumber(number)).build())
                .build();
    }

    public static ConfigSetting allowRemoteControlSetting() {
        return settingOf(allowRemoteControlLine(), kittyConfigFile(), 4);
    }

    public static ConfigSetting listenOnSetting() {
        return settingOf(listenOnLine(), kittyConfigFile(), 5);
    }

    public static void settingsRead(KittyConfigPort config, ConfigSetting... settings) {
        when(config.locate()).thenReturn(kittyConfigFile());
        when(config.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of(settings)));
    }

    public static ConfigSetting conflictingAllowSetting() {
        return settingOf(line("allow_remote_control", "yes"), includedConfigFile(), 12);
    }

    public static ConfigSetting conflictingListenSetting() {
        return settingOf(line("listen_on", "unix:/tmp/kitty"), kittyConfigFile(), 9);
    }

    public static RemoteControlConfig addingBoth() {
        return adding(bothLines());
    }

    public static RemoteControlConfig adding(List<ConfigLine> lines) {
        return AddRemoteControl.builder().file(kittyConfigFile()).lines(lines).build();
    }

    public static RemoteControlConfig conflictingWith(ConfigSetting found, ConfigLine wanted) {
        return ConflictingRemoteControl.builder().found(found).wanted(wanted).build();
    }
}
