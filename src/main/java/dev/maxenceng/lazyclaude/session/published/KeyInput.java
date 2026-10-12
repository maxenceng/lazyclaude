package dev.maxenceng.lazyclaude.session.published;

/** What a key press is: a character typed, or a key that has no character. */
public sealed interface KeyInput permits TypedCharacter, PressedKey {}
