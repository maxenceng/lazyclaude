package dev.maxenceng.lazyclaude.session.domain;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.ProcessId;

public final class ProcessEnvironmentFixture {

    private ProcessEnvironmentFixture() {}

    public static ProcessEnvironment environmentOf(Map<String, String> variables) {
        return new ProcessEnvironment(variables.entrySet().stream()
                .collect(Collectors.toMap(
                        variable -> new VariableName(variable.getKey()),
                        variable -> new VariableValue(variable.getValue()))));
    }

    public static ProcessEnvironment emptyEnvironment() {
        return environmentOf(Map.of());
    }

    public static ProcessEnvironment kittyEnvironment() {
        return environmentOf(kittyVariables());
    }

    public static ProcessEnvironment tmuxEnvironment() {
        return environmentOf(tmuxVariables());
    }

    public static ProcessEnvironment kittyAndTmuxEnvironment() {
        return environmentOf(Stream.concat(kittyVariables().entrySet().stream(), tmuxVariables().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public static ProcessEnvironment tmuxPaneOnlyEnvironment() {
        return environmentOf(Map.of("TMUX_PANE", "%7"));
    }

    public static ProcessEnvironment otherTmuxServerEnvironment() {
        return environmentOf(Map.of("TMUX_PANE", "%7", "TMUX", "/tmp/tmux-1000/other,4243,0"));
    }

    public static ProcessEnvironment kittyPidOnlyEnvironment() {
        return environmentOf(Map.of("KITTY_PID", "1621"));
    }

    public static ProcessEnvironment kittyWithGarbledWindowEnvironment() {
        return environmentOf(Map.of("KITTY_PID", "1621", "KITTY_WINDOW_ID", "three"));
    }

    public static ProcessEnvironment garbledTmuxPaneEnvironment() {
        return environmentOf(Map.of("TMUX_PANE", "seven", "TMUX", "/tmp/tmux-1000/default,4242,0"));
    }

    public static InKitty inKitty() {
        return InKitty.builder().kitty(new ProcessId(1621)).window(new KittyWindowId(3)).build();
    }

    public static InTmux inTmux() {
        return InTmux.builder().pane(new TmuxPane("%7")).server(new TmuxSocket("/tmp/tmux-1000/default")).build();
    }

    private static Map<String, String> kittyVariables() {
        return Map.of("KITTY_PID", "1621", "KITTY_WINDOW_ID", "3");
    }

    private static Map<String, String> tmuxVariables() {
        return Map.of("TMUX_PANE", "%7", "TMUX", "/tmp/tmux-1000/default,4242,0");
    }
}
