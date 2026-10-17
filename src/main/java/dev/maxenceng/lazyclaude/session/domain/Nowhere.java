package dev.maxenceng.lazyclaude.session.domain;


/** The session carries neither a kitty window nor a tmux pane, so there is no window to bring forward. */
public record Nowhere() implements Residence {
}
