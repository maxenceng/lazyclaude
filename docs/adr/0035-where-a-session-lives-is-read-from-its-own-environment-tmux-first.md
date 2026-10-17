# 35. Where a session lives is read from its own environment, tmux first

Date: 2026-10-17

## Status

Accepted. Builds on ADR 0021 (the sweep that reads the live sessions) and ADR 0027 (the application service composes managers).

## Context

An outside session runs in a terminal lazyclaude did not start. To label its row and bring its window forward, lazyclaude has to know
which window that is. The registry says nothing of it, but the process carries the variables its terminal gave it.

## Decision

- **A live session carries a `Residence`**: `InKitty` (the `KITTY_PID` and `KITTY_WINDOW_ID` it carries), `InTmux` (its `TMUX_PANE`
  and the server named by the first field of `TMUX`, or an unknown one) or `Nowhere`. The sweep reads it with the registry, from
  `/proc/<pid>/environ`, through `ProcessEnvironmentPort`.
- **tmux wins over kitty.** A `claude` inside tmux carries the stale `KITTY_*` of wherever the server was first started. A value that
  is not a number (a window id) or not a pane id (`%` and a number) counts as absent.
- **An environment that cannot be read** places the session nowhere and raises one warning; a process that is gone has an empty one.
- **The jump** goes through `TerminalJumpPort`: `kitten @ --to unix:@kitty-<KITTY_PID> focus-window --match id:<KITTY_WINDOW_ID>`,
  or `tmux switch-client -t <pane>`. A session in tmux is jumped to only when lazyclaude runs in the same tmux server, read from
  lazyclaude's own environment (`ProcessEnvironmentPort.ownEnvironment`), handed over at wiring.
- **kitty's refusal for remote control** is told apart from any other failure by its words: `Remote control is ...` (kitty listens but
  does not allow it) and a failed connection to the socket (`allow_remote_control no` opens none). Any other non-zero exit is an
  ordinary failure.
- **A session lazyclaude hosts is never outside**, under Elsewhere either: the hosted pids are laid over the project's live sessions and
  over every place elsewhere. `o` does nothing on it.

## Consequences

- A stale `KITTY_PID` (kitty gone) also reads as a refusal, so a lazyclaude in kitty offers the `kitty.conf` lines (ADR 0036) for it; the
  user can answer no.
- A session in a tmux server lazyclaude is not attached to cannot be jumped to; it warns. Closing that needs the attached client's kitty
  window.
- Every sweep reads one `environ` per live session.
