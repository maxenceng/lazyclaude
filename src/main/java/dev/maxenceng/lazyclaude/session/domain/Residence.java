package dev.maxenceng.lazyclaude.session.domain;

/** Where a live session lives, read from its own environment: in a kitty window, in a tmux pane, or in neither. */
public sealed interface Residence permits InKitty, InTmux, Nowhere {}
