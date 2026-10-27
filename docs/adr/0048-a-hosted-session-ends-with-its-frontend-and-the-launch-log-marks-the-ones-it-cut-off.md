# 48. A hosted session ends with its frontend, and the launch log marks the ones it cut off

Date: 2026-10-27

## Status

Accepted. Keeps ADR 0007's and ADR 0026's rule that a hosted session dies with lazyclaude. Amends ADR 0047: a launch-log
entry records how its session ended.

## Context

A session that had to outlive lazyclaude used to live in kitty or tmux, and ADR 0047 removed both. The desktop evolution
also puts `session` behind a sidecar with two frontends, the TUI and a Tauri window. That forces a choice: either the
sidecar is a child of its frontend, so quitting ends its sessions, or a long-lived process owns the ptys and frontends
attach to it.

A daemon would keep sessions through a quit, a crash or a frontend restart. In exchange it would have to hold every
terminal's screen so it could repaint a frontend that attaches, offer discovery over a socket, a version handshake,
several clients and a rule for who resizes, and manage its own lifecycle across upgrades. `claude --resume` already brings
back the conversation. What a quit loses is a turn in flight and any background shell `claude` was running.

## Decision

- **A hosted session ends with its frontend.** The sidecar is a child process: the TUI hosts it in-process and Tauri
  spawns it. It exits when its input closes, and it ends every `claude` it hosts. The protocol has no attach, no detach,
  no discovery and no second client. Rejected: a daemon owning the ptys, for the costs above. Also rejected: a hybrid where
  an explicit detach hands one session to a holder process, which pays the daemon's cost for a rarer case. Keeping
  sessions through a quit would be a new decision and a new ADR.
- **The launch log records an end only when `claude` exits by itself.** An entry with no live process and no end is an
  *interrupted session*: lazyclaude's quit, a crash or a kill cut it off. Since the record is missing rather than written,
  a crash needs no special handling.
- **Interrupted sessions are listed first among past sessions, with a marker, and resumed one at a time with `enter`.**
  Rejected: resuming them automatically at startup, or with one key, which would spawn several `claude` the developer may
  not want, each spending a resumed turn.
- **A session stays interrupted until it is resumed.** Resuming it clears its entry's end and makes it hosted again.
  The entry leaves the log when its transcript goes, as ADR 0047 has it. Rejected: an interruption that fades after one
  launch, and a key that dismisses it, as state nothing needs yet.
- **Closing the desktop window is `q`.** It asks first when a hosted session is busy or waiting, then ends the sessions.
  Rejected: hiding to a tray, which needs a tray on every platform and works around this ADR.

## Consequences

- The protocol's lifecycle is start, run, input closed, exit. Shutdown means ending the hosted `claude`, and there is no
  reattach to design.
- A webview reload in the desktop frontend does not end the Tauri process, so the sidecar keeps running and the frontend
  has to repaint its panes from it. That belongs to the desktop frontend's architecture.
- An in-flight turn and a `claude` background shell are lost on quit. If that starts to hurt, it reopens the daemon
  question.
- The launch log grows one optional field per entry, its end.
