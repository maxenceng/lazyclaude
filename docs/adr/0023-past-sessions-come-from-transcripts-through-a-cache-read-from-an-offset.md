# 23. Past sessions come from transcripts through a cache, and a grown transcript is read from an offset

Date: 2026-10-11

## Status

Accepted. Extends ADR 0021, which covers the live sessions.

## Context

A past session has no process, so the registry (ADR 0021) says nothing of it. The only record is its
transcript, `~/.claude/projects/<folder>/<sessionId>.jsonl`: internal, undocumented, and large. A project's
folder can hold hundreds, some of megabytes, and the Sessions panel must draw its first frame without reading
any of them. Reading every transcript in full on every start would make the cost grow with the history,
not with what changed.

## Decision

- **Past sessions come from transcripts, one per top-level `<sessionId>.jsonl`.** A `subagents/`
  transcript sits a level deeper and is never a session. The row's facts are the last `cwd`, the last
  `ai-title`, the first user prompt, the first and the last entry's timestamp.
- **They are read through a cache in `$XDG_CACHE_HOME/lazyclaude/`** (`~/.cache/lazyclaude/` when unset or
  not absolute), `past-sessions.json`. The first frame is drawn from the registry and the cache; every
  transcript is then stat-ed on the transcript watcher's own thread, and only a transcript new or changed
  since the cache is read. The key is **path, size, modification time and file key**; the cache is written
  whole to a temporary file and renamed over the old one. A cache that is missing, unparseable, of
  another format version, or with an entry missing its path, file key or session id, is empty and rewritten, with no warning; one that cannot be written raises one
  warning, and the sessions still show, read in full on every start. Nothing is cached that the panel does
  not show or the project match does not need, and adding a field is a bump of the format version.
- **A grown transcript is read from the offset the cache holds.** This relies on transcripts being
  append-only, which is observed, not documented. A last line with no newline yet is not consumed: the
  offset stays before it, and the next read takes it whole. A transcript that shrank, or whose file key
  changed, is read again in full. If Claude Code ever rewrites a transcript in place without shrinking it,
  a stale title or `cwd` shows until the file is next read in full; that is the cost of this choice.
- **A transcript belongs to a project by git, located once.** Its last `cwd` is located through the shared
  `ProjectLocatorPort` (ADR 0019) when the transcript is first scanned, and the answer is kept in the cache.
  The adapter does not hold the port: the domain hands the watcher a `TranscriptLocator`, so the lookup
  stays a domain decision and runs once per new transcript, never per refresh.
- **A removed worktree is matched by the path rule.** When the `cwd` no longer exists, git cannot place it,
  and it belongs to the project when it lies under the project root or under `../<repo>.worktrees/`, the
  folder tickets' worktrees are made in. This also admits a directory that exists, is outside git, and sits
  under those paths; nothing of the kind is expected there.
- **A live id hides the past session of the same id.** A session is listed once, in the live block. When its
  process dies it moves to the past block with the same session id, so a selection follows it.
- **Past sessions never make the panel unavailable.** `UnavailableSessions` still means the live source
  failed. A transcript that cannot be read is not listed and is read again when it changes; a line that does
  not parse is skipped and the rest of the transcript counts.
- **The watch is two levels deep and the scan is the watcher's.** `TranscriptWatchRepository` watches
  `~/.claude/projects/` and each folder in it, and a folder created later; a changed file is read about one
  second after its last change, not on every line, and an `OVERFLOW` re-stats every transcript. It does not
  borrow the docs tree walker, which stays package-private (ADR 0016). When a watch cannot be set up, every
  transcript is re-stated every 5 s, with one warning naming the tree and `fs.inotify.max_user_watches`
  (ADR 0022). The scan never runs on the registry watcher's thread, so a slow scan cannot delay a live
  state change.
- **A missing `~/.claude/projects/` means no past sessions and no warning.**

## Consequences

- The first run with no cache reads every transcript once, off the UI thread, and rows appear as the
  reads land; every later start reads only what changed.
- Reading depends on three things Claude Code does not promise: the transcript path, the shape of its
  lines, and that they are appended to. A change in any of them shows as a missing or stale row, not a
  failure; the *deferred* notes and this ADR are where to look.
- A projects directory that does not exist when lazyclaude starts is not watched, so a first Claude Code
  run in the same session appears at the next start.
- A transcript that reads in full on every change of a shrinking or replaced file costs its size; the
  common case, a session appending lines, costs only the new lines.
- The prompt a row falls back to is the first user entry that is not a meta entry and carries text, cut
  to one line. A prompt that is a slash command shows as Claude Code wrote it.
