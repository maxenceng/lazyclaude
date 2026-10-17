package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.ResidenceName;

/** The session carries neither a kitty window nor a tmux pane, so there is no window to bring forward. */
public record Nowhere() implements Residence {

    @Override
    public Optional<ResidenceName> name() {
        return Optional.empty();
    }
}
