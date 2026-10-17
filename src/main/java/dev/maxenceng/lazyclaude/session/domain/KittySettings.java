package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/**
 * The settings kitty's configuration makes, in the order kitty applies them, an include where it is written: a later line
 * overrides an earlier one.
 */
public record KittySettings(List<KittyConfigSetting> found) {

    private static final KittyConfigLine ALLOW_REMOTE_CONTROL = KittyConfigLine.builder()
            .key(new KittyConfigKey("allow_remote_control"))
            .value(new KittyConfigValue("socket-only"))
            .build();
    private static final Set<String> SOCKET_ALLOWING = Set.of("socket-only", "socket");
    private static final Set<String> ALLOWING_ALL = Set.of("y", "yes", "true");
    private static final String KITTY_PID = "{kitty_pid}";
    private static final KittyConfigLine LISTEN_ON = KittyConfigLine.builder()
            .key(new KittyConfigKey("listen_on"))
            .value(new KittyConfigValue("unix:@kitty-" + KITTY_PID))
            .build();
    private static final List<KittyConfigLine> REMOTE_CONTROL = List.of(ALLOW_REMOTE_CONTROL, LISTEN_ON);

    public KittySettings {
        Assert.notNull("settings", found);
    }

    /** The options {@link #remoteControl} decides from, which the config is read for. */
    public static List<KittyConfigKey> remoteControlKeys() {
        return REMOTE_CONTROL.stream().map(KittyConfigLine::key).toList();
    }

    /** The socket the {@code listen_on} line this adds makes {@code kitty} listen on, which a jump to its windows goes through. */
    static KittySocket socketOf(ProcessId kitty) {
        return new KittySocket(LISTEN_ON.value().value().replace(KITTY_PID, Long.toString(kitty.value())));
    }

    /**
     * What the configuration needs for remote control over a socket: the first option set to a value that does not give it is
     * a conflict, else the lines not yet set are to be added to {@code file}, else there is nothing to do. Remote control
     * allowed over the socket, or everywhere, gives it as well as the {@code socket-only} this adds.
     */
    public RemoteControlConfig remoteControl(KittyConfigFile file) {
        Optional<RemoteControlConfig> conflict = REMOTE_CONTROL.stream()
                .flatMap(wanted -> effective(wanted.key())
                        .filter(setting -> !satisfies(setting.line(), wanted))
                        .map(setting -> conflict(setting, wanted))
                        .stream())
                .findFirst();
        List<KittyConfigLine> missing = REMOTE_CONTROL.stream()
                .filter(wanted -> effective(wanted.key()).isEmpty())
                .toList();
        return conflict.orElseGet(() -> missing.isEmpty() ? new ConfiguredRemoteControl() : added(file, missing));
    }

    private static boolean satisfies(KittyConfigLine line, KittyConfigLine wanted) {
        return line.equals(wanted) || wanted.equals(ALLOW_REMOTE_CONTROL) && allowsTheSocket(line.value());
    }

    private static boolean allowsTheSocket(KittyConfigValue value) {
        return SOCKET_ALLOWING.contains(value.value()) || ALLOWING_ALL.contains(value.value().toLowerCase(Locale.ROOT));
    }

    private Optional<KittyConfigSetting> effective(KittyConfigKey key) {
        return found.stream().filter(setting -> setting.line().key().equals(key)).reduce((earlier, later) -> later);
    }

    private static RemoteControlConfig conflict(KittyConfigSetting setting, KittyConfigLine wanted) {
        return ConflictingRemoteControl.builder().found(setting).wanted(wanted).build();
    }

    private static RemoteControlConfig added(KittyConfigFile file, List<KittyConfigLine> lines) {
        return AddRemoteControl.builder().file(file).lines(lines).build();
    }
}
