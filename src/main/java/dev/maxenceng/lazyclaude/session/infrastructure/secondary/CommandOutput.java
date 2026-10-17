package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

/** How a command ended and what it printed, standard error included. */
record CommandOutput(int exit, String output) {}
