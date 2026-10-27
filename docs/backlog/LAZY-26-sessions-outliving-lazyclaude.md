---
id: LAZY-26
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# Does a hosted session outlive the process that hosts it?

## Question

Does a hosted session outlive the process that hosts it?

Until now a session that had to survive lazyclaude lived in kitty or tmux; both are going.

- (a) quitting ends its sessions, and the next launch offers `claude --resume <id>` for each one it started;
- (b) a long-lived process owns the ptys and the TUI or the desktop frontend attaches to it.

The answer sets whether the sidecar is a child of its frontend or a daemon, and so the protocol's startup, shutdown and reattach. Leaning (a) when charted.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0048](../adr/0048-a-hosted-session-ends-with-its-frontend-and-the-launch-log-marks-the-ones-it-cut-off.md).

- **(a)**: quitting ends the hosted sessions. The sidecar is a child of its frontend (in-process for the TUI, spawned
  by Tauri), exits when its input closes and ends every `claude` it hosts: no attach, detach, discovery or second client.
  The daemon (b) and the detach-to-a-holder hybrid are rejected; reversing this is a fresh ticket.
- **Interrupted session**: the launch log records an end only when `claude` exits by itself, so an entry with no live
  process and no end was cut off by a quit, a crash or a kill. Listed first among past sessions with a marker, resumed
  one at a time with `enter`; no automatic or one-key resume.
- **It stays interrupted until resumed**, which clears the entry's end, or until its transcript goes and the entry
  leaves the log.
- **Closing the desktop window is `q`**: asks first when a session is busy or waiting; no tray.

Folded into [SESSION-12](SESSION-12-the-launch-log.md)'s acceptance criteria rather than a new ticket, since it is the
same log.
