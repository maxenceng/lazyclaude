package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.session.published.Chord;

/** Where the chords Claude Code binds are read from. */
public interface ClaudeKeybindingsPort {

    /**
     * The first keystroke of every binding the user made, in any context; none when there is no file.
     *
     * @throws ClaudeKeybindingsUnreadableException when the file exists and cannot be read
     */
    List<Chord> bindings();

    /** The first keystroke of every binding Claude Code makes by default, in any context. */
    List<Chord> defaults();
}
