package dev.maxenceng.lazyclaude.session.published;

/** A truecolor, packed as {@code 0xRRGGBB}. */
public record RgbColour(int rgb) implements CellColour {}
