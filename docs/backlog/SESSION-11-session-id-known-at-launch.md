---
id: SESSION-11
status: done
context: session
type: feature
tracker_id: 94
created: 2026-10-26
---

# A hosted session is named at launch and its state read from its own registry record

## Acceptance criteria

- [x] `N` and a `/ticket` launch run `claude --session-id <uuid>`; the session's row, dock tab and notification carry
      that id from the first frame, before Claude Code has written anything.
- [x] A resume keeps the id it resumes, and a second resume of the same session is still refused while the first runs.
- [x] A hosted session's state (busy, waiting for permission with its reason, waiting for input, your turn) comes from
      `~/.claude/sessions/<pid>.json` for its pid; nothing reads `/proc` or runs `claude agents --json`.
- [x] A hosted session whose record is unreadable or of an unknown shape shows as `Unknown`, with one warning naming
      why, and the others keep their state. One with no record yet, or a record with no status yet, is still starting:
      Claude Code writes none on its trust dialog, and its first record a moment before its first status.
- [x] A session in another terminal entering waiting fires no desktop notification; a hosted one still does, by ADR
      0033's rule.
- [x] `make contract` checks a live hosted `claude`'s registry record has the fields the domain reads.

## Model decision

Changes in `session`'s domain and secondary adapters, no new aggregate (ADR 0047).

- `HostedSessionId` goes; `HostedSession` is named by the published `SessionId`, minted by the domain for `NewClaude`
  and `TicketClaude`, taken from `ResumeClaude` for a resume. `LaunchCommand` carries it so the adapter adds
  `--session-id` before `--`.
- `LiveSessionPort` reads the record of a given pid; `LivenessCheck`, `ClaudeAgentsRepository`, `SourceFallback` and the
  `pidDomain` computation go. `UnavailableSessions` no longer means "neither source answered"; a failed read is per
  session.
- The registry watcher stays on `~/.claude/sessions/`, keeping only the records of hosted pids.
- `DesktopNotificationManager` receives hosted sessions only; its rule is unchanged.

## Design direction

n/a: no screen changes; rows and tabs keep their shape.

## Glossary impact

*Registry* drops "never trusted without the liveness check". *Hosted session* is named by its session id from launch.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map. Decided in [LAZY-25](LAZY-25-what-session-keeps.md),
recorded in ADR 0047. Blocked by [SESSION-10](SESSION-10-only-hosted-sessions.md).

Review found that `/clear` keeps the pid and starts a new session, so the launch id names the tab, not the session for
its whole life: the row follows the record's `sessionId`, and ADR 0061 amends the *Model decision* above. The same ADR
ignores a record older than the spawn (a crashed `claude`'s leftover `<pid>.json` on a reused pid) and refuses an
unknown status rather than showing a quiet `Unknown`, reversing ADR 0021. Two resumes of one session racing on the
shell's executor started two `claude --resume`; the id is now claimed under the lock before the spawn.
