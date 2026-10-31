package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

/** What a frontend that is not told of a withdrawal sees of the warnings: the status line's one warning, or that none is held. */
public sealed interface WarningsView permits WarningView, NoWarningView {}
