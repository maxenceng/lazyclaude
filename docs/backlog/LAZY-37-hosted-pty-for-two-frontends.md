---
id: LAZY-37
status: todo
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-26, LAZY-36]
---

# How does `session` expose a hosted pty to two frontends?

## Question

How does `session` expose a hosted pty to two frontends?

Today the one host adapter holds pty4j and jediterm (ADR 0026). Where terminal emulation lives once the TUI emulates in jediterm and the desktop in xterm.js; what the `HostPort` and `HostedSession` publish (a raw byte stream, a grid, both); and who owns resize and the session's lifetime.

## Kind

grilling

## Resolution

_Not resolved yet._
