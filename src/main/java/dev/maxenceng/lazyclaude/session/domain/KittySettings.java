package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * The settings kitty's configuration makes, in the order kitty applies them, an include where it is written: a later line
 * overrides an earlier one.
 */
public record KittySettings(List<ConfigSetting> found) {

    private static final ConfigLine ALLOW_REMOTE_CONTROL = ConfigLine.builder()
            .key(new ConfigKey("allow_remote_control"))
            .value(new ConfigValue("socket-only"))
            .build();
    private static final ConfigLine LISTEN_ON = ConfigLine.builder()
            .key(new ConfigKey("listen_on"))
            .value(new ConfigValue("unix:@kitty-{kitty_pid}"))
            .build();
    private static final List<ConfigLine> REMOTE_CONTROL = List.of(ALLOW_REMOTE_CONTROL, LISTEN_ON);

    public KittySettings {
        Assert.notNull("settings", found);
    }

    /** The options {@link #remoteControl} decides from, which the config is read for. */
    public static List<ConfigKey> remoteControlKeys() {
        return REMOTE_CONTROL.stream().map(ConfigLine::key).toList();
    }

    /**
     * What the configuration needs for remote control over a socket: the first option set to another value is a conflict,
     * else the lines not yet set are to be added to {@code file}, else there is nothing to do.
     */
    public RemoteControlConfig remoteControl(KittyConfigFile file) {
        Optional<RemoteControlConfig> conflict = REMOTE_CONTROL.stream()
                .flatMap(wanted -> effective(wanted.key())
                        .filter(setting -> !setting.line().equals(wanted))
                        .map(setting -> conflict(setting, wanted))
                        .stream())
                .findFirst();
        List<ConfigLine> missing = REMOTE_CONTROL.stream()
                .filter(wanted -> effective(wanted.key()).isEmpty())
                .toList();
        return conflict.orElseGet(() -> missing.isEmpty() ? new ConfiguredRemoteControl() : added(file, missing));
    }

    private Optional<ConfigSetting> effective(ConfigKey key) {
        return found.stream().filter(setting -> setting.line().key().equals(key)).reduce((earlier, later) -> later);
    }

    private static RemoteControlConfig conflict(ConfigSetting setting, ConfigLine wanted) {
        return ConflictingRemoteControl.builder().found(setting).wanted(wanted).build();
    }

    private static RemoteControlConfig added(KittyConfigFile file, List<ConfigLine> lines) {
        return AddRemoteControl.builder().file(file).lines(lines).build();
    }
}
