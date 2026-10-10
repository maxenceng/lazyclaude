# 7. Sessions run embedded, as the real `claude` on a pty

Date: 2026-10-10

## Status

Accepted

## Context

lazyclaude has to put the user into a Claude Code session: a new one on a ticket's next
`/ticket` verb, a past one to resume, or a live one. The user wants that session inside
lazyclaude, interactive exactly as Claude Code is, not in another tab or behind a suspended
TUI.

There were two ways to show a session in a pane. lazyclaude could draw its own chat view over
`claude -p --input-format stream-json --output-format stream-json`. That would mean
reimplementing Claude Code's interface, its permission prompts, dialogs and slash commands,
and chasing it on every release. Or lazyclaude could run the real `claude` on a pseudo-terminal
and draw what it prints. Handing the whole terminal over (kitty tabs, tmux, or closing the
TamboUI runner) was rejected because lazyclaude would no longer be on screen beside the session.

LAZY-14 ran `claude` 2.1.294 on pty4j 0.14.0 with jediterm-core 3.76. Its fullscreen UI drew
correctly, and resize, paste and Ctrl+C reached it. Drawing a 200×50 grid as a TamboUI
`Element` costs under 1 ms a frame. Details: `docs/research/embedding-claude-on-a-pty.md`.

## Decision

A hosted session is the real `claude` process. lazyclaude spawns it on a pty (pty4j), runs its
output through a headless terminal emulator (jediterm-core), and draws the emulator's cell grid
in a pane. While the pane has focus, keys go to the pty.

lazyclaude owns those ptys, so a hosted session ends when lazyclaude quits. Quitting asks for
confirmation while any session is busy or waiting. A session that ended can be resumed from its
transcript.

A live session started outside lazyclaude is not embedded. lazyclaude shows a read-only view of
its transcript and can jump to its kitty window.

## Consequences

Whatever Claude Code's interface does, a hosted session does too, with no work on lazyclaude's
side. The cost is a dependency on terminal emulation fidelity instead of on a message format.

- **jediterm-core isn't on Maven Central.** The pom has to name JetBrains' `intellij-dependencies`
  repository.
- **pty4j loads a native library.** On JDK 25 the JVM needs `--enable-native-access`.
- **The child's environment must be cleaned.** Strip `CLAUDE*`, `KITTY*` and `TMUX`, or `claude`
  believes it runs inside another session, kitty or tmux.
- **Input is the weak spot.**
  - TamboUI takes Tab and Shift+Tab for focus, so those bindings must be freed while a session
    pane has focus.
  - Bracketed paste and mouse capture must be turned on.
  - Shift/Ctrl/Alt+Enter are lost until TamboUI supports the kitty keyboard protocol (upstream
    #446).
  - Ctrl+Z needs intercepting.
- **The way out of the pane is a chord.** The session swallows every key, so leaving it takes a
  chord lazyclaude reserves.
- **Sessions don't survive lazyclaude.** Keeping them alive across a restart would take a
  separate holder process, like tmux's server. That is a later, deliberate change, not a
  default.
