package dev.maxenceng.lazyclaude.session.published;

/** The colour of a cell's text or background: the terminal's own default, one of its 256 palette entries, or a truecolor. */
public sealed interface CellColour permits DefaultColour, IndexedColour, RgbColour {}
