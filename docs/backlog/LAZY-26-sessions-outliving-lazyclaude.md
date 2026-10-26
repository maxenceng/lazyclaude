---
id: LAZY-26
status: todo
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

_Not resolved yet._
