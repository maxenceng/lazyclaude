package dev.maxenceng.lazyclaude.session.domain;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** The variables a process runs with, those set to something. */
public record ProcessEnvironment(Map<EnvironmentVariableName, EnvironmentVariableValue> variables) {

    private static final EnvironmentVariableName TMUX_PANE = new EnvironmentVariableName("TMUX_PANE");
    private static final EnvironmentVariableName TMUX = new EnvironmentVariableName("TMUX");
    private static final EnvironmentVariableName KITTY_PID = new EnvironmentVariableName("KITTY_PID");
    private static final EnvironmentVariableName KITTY_WINDOW_ID = new EnvironmentVariableName("KITTY_WINDOW_ID");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}");
    private static final String TMUX_FIELD_SEPARATOR = ",";
    private static final int TMUX_FIELDS_KEPT = 2;

    public ProcessEnvironment {
        Assert.notNull("variables", variables);
    }

    /**
     * Where a process with this environment lives. tmux wins over kitty: a {@code claude} inside tmux carries the
     * stale {@code KITTY_*} of wherever the server was started.
     */
    public Residence residence() {
        return inTmux().<Residence>map(tmux -> tmux).or(this::kitty).orElseGet(Nowhere::new);
    }

    private Optional<InTmux> inTmux() {
        return valueOf(TMUX_PANE)
                .filter(pane -> TmuxPane.SHAPE.matcher(pane.value()).matches())
                .map(pane -> InTmux.builder()
                        .pane(new TmuxPane(pane.value()))
                        .server(serverOfTmux())
                        .build());
    }

    private TmuxServer serverOfTmux() {
        return valueOf(TMUX)
                .map(value -> value.value().split(TMUX_FIELD_SEPARATOR, TMUX_FIELDS_KEPT)[0])
                .filter(socket -> !socket.isBlank())
                .<TmuxServer>map(TmuxSocket::new)
                .orElseGet(UnknownTmuxServer::new);
    }

    /** The kitty window the process runs in, whichever else it runs in too: its variables are there while tmux is. */
    public Optional<InKitty> kitty() {
        return numberOf(KITTY_PID)
                .flatMap(kitty -> numberOf(KITTY_WINDOW_ID)
                        .map(window -> InKitty.builder()
                                .kitty(new ProcessId(kitty))
                                .window(new KittyWindowId(window))
                                .build()));
    }

    private Optional<Long> numberOf(EnvironmentVariableName name) {
        return valueOf(name)
                .map(EnvironmentVariableValue::value)
                .filter(text -> NUMBER.matcher(text).matches())
                .map(Long::parseLong);
    }

    private Optional<EnvironmentVariableValue> valueOf(EnvironmentVariableName name) {
        return Optional.ofNullable(variables.get(name));
    }
}
