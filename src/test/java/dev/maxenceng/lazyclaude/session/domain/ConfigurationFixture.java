package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altZ;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlO;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlY;

import java.util.List;
import java.util.Map;

import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

public final class ConfigurationFixture {

    private ConfigurationFixture() {}

    public static Configuration defaultConfiguration() {
        return Configuration.builder()
                .reservedChords(ReservedChords.defaults())
                .desktopNotifications(true)
                .mouse(true)
                .build();
    }

    public static Configuration rebindingFocusLeft() {
        return Configuration.builder()
                .reservedChords(ReservedChords.defaults().rebinding(Map.of(ReservedAction.FOCUS_LEFT, ctrlY())))
                .desktopNotifications(true)
                .mouse(true)
                .build();
    }

    public static Configuration rebindingZoomOntoClaudesDefault() {
        return Configuration.builder()
                .reservedChords(ReservedChords.defaults().rebinding(Map.of(ReservedAction.ZOOM, ctrlO())))
                .desktopNotifications(true)
                .mouse(true)
                .build();
    }

    public static Configuration withoutDesktopNotifications() {
        return Configuration.builder()
                .reservedChords(ReservedChords.defaults())
                .desktopNotifications(false)
                .mouse(true)
                .build();
    }

    public static Configuration withoutMouse() {
        return Configuration.builder()
                .reservedChords(ReservedChords.defaults())
                .desktopNotifications(true)
                .mouse(false)
                .build();
    }

    public static ConfigurationRefusedException refusal() {
        return new ConfigurationRefusedException("~/.config/lazyclaude/config.yml:3: unknown key 'focusLeftt'");
    }

    public static List<Chord> bindingsClashingTwice() {
        return List.of(altH(), altZ());
    }

    public static ClaudeKeybindingsUnreadableException unreadableKeybindings() {
        return new ClaudeKeybindingsUnreadableException("access denied to /home/max/.claude/keybindings.json", null);
    }
}
