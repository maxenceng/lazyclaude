package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.ResidenceName;

/** Where a live session lives, read from its own environment: in a kitty window, in a tmux pane, or in neither. */
public sealed interface Residence permits InKitty, InTmux, Nowhere {

    /** What the Sessions panel calls the place, none for a session that lives nowhere known. */
    Optional<ResidenceName> name();
}
