---
id: LAZY-25
status: todo
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# What does `session` keep when only the sessions lazyclaude started count?

## Question

What does `session` keep when only the sessions lazyclaude started count?

- which v1 behaviour goes: outside live sessions, their transcript view, the kitty jump and its `kitty.conf` offer, tmux (ADRs 0035, 0036), the Elsewhere panel;
- whether the registry check and `claude agents --json` (ADR 0021) are still needed to know a hosted session's state, or the pty and hooks of its own launch suffice;
- how a past hosted session is found to resume once lazyclaude has quit — a record lazyclaude keeps of what it started, or the transcripts as today (ADR 0023);
- whether the args-then-branch link (ADR 0024) gives way to a link known at launch;
- notifications: only hosted sessions entering waiting (ADR 0033);
- the superseding ADRs and the code, glossary and context-map removals this implies.

## Kind

grilling

## Resolution

_Not resolved yet._
