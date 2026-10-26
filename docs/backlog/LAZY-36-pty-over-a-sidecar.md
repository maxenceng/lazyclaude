---
id: LAZY-36
status: todo
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# Can a hosted `claude` run smoothly through a JSON Lines sidecar into xterm.js in a Tauri window?

## Question

Can a hosted `claude` run smoothly through a JSON Lines sidecar into xterm.js in a Tauri window?

A throwaway: Java spawns `claude` on pty4j, frames its output as JSON Lines on stdout, a Tauri app writes it into xterm.js and sends keys and resizes back. Measure input latency, throughput on a large output, the cost of the byte encoding (base64 or other), resize, Ctrl+C, paste, mouse reporting, and what happens when either side dies.

## Kind

prototype

## Resolution

_Not resolved yet._
