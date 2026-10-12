# 26. A hosted session is an aggregate behind a host port, with the encoding in the adapter

Date: 2026-10-12

## Status

Accepted. Builds on ADR 0007 and ADR 0008.

## Context

ADR 0007 settled that a session runs as the real `claude` on a pty, drawn through a terminal emulator, and ADR 0008
that the dock holds one tab per session. Making that real raises four questions the earlier decisions leave open: where
the hosted session lives in the model, where the bytes of a key, a paste and a mouse event are decided, whether the
outer terminal reports the mouse, and what is lost on the way from the keyboard to the pty.

## Decision

- **`HostedSession` is an aggregate of the `session` context**, not a new context. It is a `claude` process lazyclaude
  owns: *starting* while no registry record carries its pid, then the live session that record describes, dropped when the
  process exits. `HostedSessionManager` holds the open ones in start order and decides the working directory: the project
  root for a new unlinked session, the recorded `cwd` for a resume, the root with a warning when that directory is gone.
  It warns when a start fails or a `claude` exits non-zero. `SessionManager` learns which live sessions are hosted from
  their pids, so `LiveSessionView` says hosted or outside.
- **`HostPort` is the domain's whole view of the pty:** spawn, directory check, send, resize, screen, end, and the exit
  and screen-changed signals. `PtyHostRepository` implements it and is the only place that knows pty4j and jediterm-core;
  `ArchitectureTest` fails the build when those libraries (`com.pty4j..`, `com.jediterm..`) are used outside
  `session.infrastructure.secondary`. Its collaborators there (`HostedPty`, `KeyBytes`, `MouseBytes`, `PasteBytes`,
  `ScreenCopy`, `ScrubbedEnvironment`) hold no port.
- **The encoding boundary is the adapter.** The shell hands in published values (`TerminalKey`, `PastedText`,
  `TerminalMouse`) and reads a published `TerminalScreen`; no TamboUI type crosses into `session` and no jediterm type
  crosses out. A paste is bracketed when the child turned on mode 2004, and any closing bracket inside it is dropped; the
  mouse is sent as SGR only for the events the child asked for (clicks, drags, motion); arrows follow the child's cursor-key
  mode. The child's environment is lazyclaude's less every `CLAUDE*`, `KITTY*`, `TMUX*` and `TERMINFO` variable, with
  `TERM=xterm-256color` and `COLORTERM=truecolor`.
- **Mouse capture is on.** Without it a wheel and a click never reach `claude`'s fullscreen UI. The cost is that the outer
  terminal's own text selection needs its bypass modifier (shift in most terminals), and that the shell sees every mouse
  event, which it forwards only when it happens over the pane.
- **Bracketed paste is on and Tab and Shift+Tab are unbound from focus** in the `TuiConfig`, since TamboUI otherwise
  swallows them for focus movement and turns a paste into keystrokes whose newlines submit the prompt.
- **Chords are reserved, not forwarded:** `alt+h/j/k/l/m/z/w` inside a tab, and Ctrl+Z, which would stop `claude` with
  no shell to resume it. Ctrl+C reaches `claude`. Outside a tab, `q` and Ctrl+C quit at once unless a hosted session is
  busy or waiting, in which case the status line asks y/n first.
- **A hosted session dies with lazyclaude.** The composition root ends every hosted `claude` however the shell returns.
  Its tab stays open when the registry cannot be read, as *starting*, since the processes are lazyclaude's own.

## Consequences

- **Ctrl+J, Shift+Enter, Ctrl+Enter and Alt+Enter are lost inside the pane.** TamboUI collapses them to Enter or an
  unknown key before the shell sees them, so `claude`'s newline chords do not work there; `\` then Enter and paste still do.
  This is a `deferred:` note at the key mapping, closed when TamboUI supports the kitty keyboard protocol (upstream #446).
  A decorator on the JLine backend that forwards raw bytes was considered and left out as untried risk.
- **Matching a hosted session to its registry record is by pid.** If a `claude` launcher process forks and the registry
  records the child's pid, the tab stays *starting*; the contract run after a Claude Code upgrade is where that shows.
- **A hosted session still starting has no Sessions row,** since it has no session id yet; its tab says *starting*.
- **Reading the sessions again after a tab opens or closes runs on the thread that caused it,** so starting a session
  pays for one registry read and the `git` call per distinct `cwd`.
- **Sessions do not survive lazyclaude,** as ADR 0007 said; a holder process would be a separate, deliberate change.
