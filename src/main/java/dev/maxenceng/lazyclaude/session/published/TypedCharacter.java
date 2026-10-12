package dev.maxenceng.lazyclaude.session.published;

/** A key that types a character, by its Unicode code point. */
public record TypedCharacter(int codePoint) implements KeyInput {}
