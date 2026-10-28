# 61. A hosted session follows its record's session, dated from its spawn, and refuses a status it does not know

Date: 2026-10-28

## Status

Accepted. Amends ADR 0047 (the launch id names the tab, not the session for its whole life) and ADR 0021 (a status
this code does not know is refused, not shown as `Unknown` quietly).

## Context

ADR 0047 names a hosted session at launch and reads its state from `~/.claude/sessions/<pid>.json`. On Claude Code
2.1.311, `/clear` keeps the process and its pid but starts a new session: the record's `sessionId` changes, and a new
transcript is written under the new id. A row that kept the launch id linked the old transcript, showed the new one as a
past row, and would resume it as a second `claude` on a live transcript.

A record is found by pid alone. A `claude` that crashed leaves its `<pid>.json` behind, and when the OS gives that pid to
a new hosted `claude`, the old record describes the new tab until the new process overwrites it.

ADR 0021 showed a status this code does not know as `Unknown` with no warning, so one new value would not take every
session down with it. Since ADR 0047 each record is read for its own session, so that reason is gone. A quiet `Unknown`
also counts as not busy, so `q` quit without asking over a session doing something lazyclaude could not name.

## Decision

- **A hosted session runs as the session its record names.** The row, its links, its notification and the resume
  guard use the record's `sessionId`. The launch id names only the tab and its pty, so a session's id can change while
  its tab stays the same.
- **A hosted session keeps the instant it was spawned**, taken just before the spawn and truncated to the millisecond,
  as the registry records it. A record whose `startedAt` is earlier than that belongs to an earlier process of the same
  pid and is ignored: the session is still starting. An unread session is dated from its spawn, not from the read.
- **A status this code does not know makes the record unreadable**: `Unknown`, one warning naming the status, and `q`
  asks before quitting. A record with no `sessionId` is refused the same way.
- `make contract` pins the record's `sessionId` changing on `/clear` under the same pid.

## Consequences

- The transcript a hosted session left behind with `/clear` has no row until its tab closes: resuming it would need a
  pty keyed by its launch id, which the tab still holds. This is marked `deferred:` in `RunningSessions`.
- A Claude Code that adds a status shows a warning on each session that reports it, until this code learns the status.
