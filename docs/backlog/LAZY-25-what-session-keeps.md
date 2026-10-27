---
id: LAZY-25
status: done
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

Grilled 2026-10-26; recorded in [ADR 0047](../adr/0047-only-the-sessions-lazyclaude-started-count-and-a-launch-log-lists-them.md).

- **What goes**: outside live sessions and the `o` jump, the transcript view (ADR 0037, past hosted sessions included —
  resuming shows the conversation), the kitty jump and `kitty.conf` offer (ADR 0036), tmux residence (ADR 0035), the
  Elsewhere panel.
- **Id at launch**: every new and `/ticket` launch passes `--session-id <uuid>`; `HostedSessionId` merges into `SessionId`.
- **State**: the registry record of each hosted pid, no liveness check, no `claude agents --json` fallback; an
  unreadable record is `Unknown` with one warning. Hooks through `--settings` rejected.
- **Past sessions**: a *launch log* lazyclaude keeps under `$XDG_STATE_HOME/lazyclaude/`, one file per project — session
  id, directory, launch command, started at. Transcripts are read only for a row's detail, only for logged ids, through
  ADR 0023's cache scoped down. Sessions from before the log are not imported.
- **Pruning**: an entry whose transcript is gone is dropped silently; a live one waits for its transcript.
- **Links**: known at launch for a `/ticket` launch, then moved by `/ticket` args in the transcript; the branch rule and
  the weak link go.
- **Notifications**: ADR 0033's rule, hosted sessions only.
- **A selected past session** shows a session card in the main view — title, first prompt, linked tickets with their
  last verb, directory, started and last active — with `enter resume`.
- **Timing**: the ADR now; glossary, context map and CLAUDE.md (hard rule 4's `tmux`) with the code that goes.

Graduated into [SESSION-10](SESSION-10-only-hosted-sessions.md),
[SESSION-11](SESSION-11-session-id-known-at-launch.md) and [SESSION-12](SESSION-12-the-launch-log.md), in that order.
