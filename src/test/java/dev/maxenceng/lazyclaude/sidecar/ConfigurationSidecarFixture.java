package dev.maxenceng.lazyclaude.sidecar;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ConfigurationSidecarFixture {

    private static final Map<String, String> DEFAULT_KEYS = Map.of(
            "FOCUS_LEFT", "h", "FOCUS_DOWN", "j", "FOCUS_UP", "k", "FOCUS_RIGHT", "l", "HIDE_MAIN_VIEW", "m", "ZOOM", "z", "NEXT_WAITING", "w");
    private static final String ACTIONS = "FOCUS_LEFT,FOCUS_DOWN,FOCUS_UP,FOCUS_RIGHT,HIDE_MAIN_VIEW,ZOOM,NEXT_WAITING";

    private ConfigurationSidecarFixture() {}

    public static String defaultConfiguration() {
        return configuration(chords("ALT", "FOCUS_LEFT", 'h'), true, true);
    }

    public static String reboundConfiguration() {
        return configuration(chords("CTRL", "FOCUS_LEFT", 'y'), true, true);
    }

    public static String mouselessConfiguration() {
        return configuration(chords("ALT", "FOCUS_LEFT", 'h'), true, false);
    }

    private static String configuration(String chords, boolean desktopNotifications, boolean mouse) {
        return "{\"chords\":[%s],\"desktopNotifications\":%b,\"mouse\":%b}".formatted(chords, desktopNotifications, mouse);
    }

    private static String chords(String modifier, String rebound, char reboundKey) {
        return Stream.of(ACTIONS.split(","))
                .map(action -> action.equals(rebound)
                        ? chord(action, modifier, reboundKey)
                        : chord(action, "ALT", DEFAULT_KEYS.get(action).charAt(0)))
                .collect(Collectors.joining(","));
    }

    private static String chord(String action, String modifier, char key) {
        return "{\"action\":\"%s\",\"chord\":{\"modifiers\":[\"%s\"],\"key\":{\"kind\":\"typedCharacter\",\"codePoint\":%d}}}"
                .formatted(action, modifier, (int) key);
    }
}
