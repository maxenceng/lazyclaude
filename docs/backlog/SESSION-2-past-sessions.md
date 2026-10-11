---
id: SESSION-2
status: done
context: session
type: feature
tracker_id: 79
created: 2026-10-10
---

# Past sessions read from transcripts through the cache, with the tail of a growing file parsed from where it left off

## Acceptance criteria

- [x] The Sessions panel lists this project's past sessions after its live ones: one row per
      top-level `~/.claude/projects/*/<sessionId>.jsonl`, never a `<session>/subagents/`
      transcript. Past rows are ordered by last activity (the timestamp of the transcript's
      last entry), newest first, and carry a dim `○` and the time since that last activity.
- [x] Only the 5 newest past sessions show, followed by a `… N older` row; `enter` on it
      expands every older past session in place. With 5 or fewer, there is no `… N older` row.
- [x] A session that is live is listed once, in the live block, never also as past. When its
      process dies, it moves to the past block and stays selected if it was.
- [x] A transcript belongs to this project when its last `cwd` locates to it through git,
      located once when the transcript is first scanned and kept in the cache. When that
      directory no longer exists, it belongs when the path lies under the project root or
      under `../<repo>.worktrees/`. A transcript in another project, or with no `cwd` at all,
      is not listed.
- [x] A row's text is the transcript's last `ai-title`. Without one, it is the first user
      prompt cut to one line; without a prompt, the short session id, dimmed.
- [x] Selecting a past session puts its facts in the main view: title, `cwd`, last activity,
      start time (the first entry's timestamp) and session id. The selection follows the
      session id across refreshes, as for live sessions.
- [x] The first frame is drawn from the registry and the cache, without reading a transcript.
      Every transcript is then stat-ed off the UI thread, and only those new or changed since
      the cache (path, size, mtime, file key) are read; their rows appear or update as each
      lands. A first run with no cache shows live sessions at once and fills past ones in.
- [x] A transcript that grew is read only from the offset the cache holds. A last line with no
      newline yet is not consumed: the offset stays before it and the next read takes it whole.
      A transcript that shrank, or whose file key changed, is read again in full.
- [x] A line that does not parse as JSON is skipped, and the rest of the transcript still
      counts. A transcript that cannot be read at all is not listed, and is retried on its
      next change.
- [x] A transcript deleted (Claude Code's retention) disappears from the panel and from the
      cache.
- [x] New and growing transcripts reach the panel through a watch on `~/.claude/projects/` and
      on each folder in it, a folder created later included. A changed file is read ~1 s after
      its last change, not on every line. An `OVERFLOW` re-stats every transcript.
- [x] A watch that cannot be set up falls back to re-stating every 5 s, with one warning
      naming the tree and the `max_user_watches` sysctl; lazyclaude still starts.
- [x] The cache lives in `$XDG_CACHE_HOME/lazyclaude/` (`~/.cache/lazyclaude/` when unset),
      written whole to a temporary file and renamed over the old one. A cache that is
      missing, unparseable or of another format version is treated as empty and rewritten,
      with no warning. A cache that cannot be written raises one warning; past sessions still
      show, read in full on every start.
- [x] With `~/.claude/projects/` missing, there are no past sessions and no warning.
- [x] Nothing in the cache is a field this ticket does not show; adding one is a format
      version bump.

## Model decision

**New behaviour in the existing `session` context.** No new context, no new edge.

- **Domain.**
  - `PastSession`: session id, `SessionTitle`, `WorkingDirectory`, `LastActivity`,
    `StartedAt`. `SessionTitle` is sealed: `AiTitle | FirstPrompt | NoTitle`, absence as a
    type (ADR 0012).
  - `LiveSessions` grows into a `Sessions` shape holding the live list and the past list.
    `UnavailableSessions` keeps meaning the live source failed; past sessions never make
    the panel unavailable.
  - `SessionManager` owns the rules: a live id hides the past one, the project match on
    the located root or the path rule, and the last-activity order. The 5-row cap and the
    expansion are the shell's, as a fold is.
- **Ports.** `PastSessionPort` returns every cached past session with its located project
  root, never touching a transcript. `TranscriptWatchPort` takes a callback, called on the
  watcher's own thread once a batch of rescans has landed, plus a `WatchFallback` for the
  5 s polling warning. Locating still goes through the shared `ProjectLocatorPort`, once per
  new transcript, never per refresh.
- **Adapters (`session.infrastructure.secondary`).**
  - `TranscriptRepository` reads a transcript from an offset with a line prefilter: only
    lines that could carry `cwd`, `timestamp`, `ai-title` or the first user prompt are
    parsed with Jackson. The JSON mirror is private, as `SessionRecord` is (ADR 0020).
  - `TranscriptCacheRepository` loads and writes the cache file.
  - `TranscriptWatchRepository` sets up the two-level watch and the 5 s polling fallback,
    and keeps the ~1 s per-file wait. Transcripts sit at a fixed depth, so it registers
    the root and each folder itself. It does not borrow `docs`' tree walker, which stays
    package-private (ADR 0016).
  - The rescan runs on that watcher's thread, never on the registry watcher's, so a slow
    scan never delays a live state change.
- **Application and primary.** `SessionApplicationService.subscribe` merges both sources.
  `SessionsView` gains past rows (`PastSessionView`). `SessionQuery` stays the shell's only
  entry.
- **Shell.** `SessionRows` takes rows of either kind, plus the `… N older` row, keyed by
  session id. `SessionText` gains the past facts and the `○` glyph.
- **ADR, in the implementing change:** past sessions come from transcripts through a cache
  in `$XDG_CACHE_HOME`, keyed on path, size, mtime and file key. A grown file is read from
  its offset, which relies on transcripts being append-only (observed, not documented). A
  removed worktree's `cwd` is matched by the path rule. All three are reversible only by
  someone who knows why.
- Not in this ticket: linking and the per-ticket split (SESSION-3), the read-only transcript
  view (SESSION-6), resuming (SESSION-5), `R` (no ticket yet owns it).

## Design direction

The Sessions panel as SESSION-1 left it, with the past block under the live one:

```
╭ Sessions ─────────────────────╮
│ ! lazyclaude-b6           2h  │
│ ∼ lazyclaude-3f          12m  │
│ ○ Refine SESSION-2       10m  │
│ ○ Fix status line         3h  │
│ ○ /ticket start DOCS-1    1d  │
│ ○ Docs tree watcher       2d  │
│ ○ 3f2a91c0                4d  │
│ … 42 older                    │
╰───────────────────────────────╯
```

- `○` is dim and is not a marker. The age is the time since last activity, right-aligned
  like a live session's.
- An untitled session with no prompt shows its short id, dimmed.
- `… N older` is dim and selectable, and `enter` replaces it with the older rows.
- With neither live nor past sessions, the panel still says `no live session`.
- Main view for a selected past session:

```
Refine SESSION-2  ○ past
cwd            /home/max/projects/lazyclaude
last activity  2026-10-11 09:12 (10m ago)
started        2026-10-11 08:40
session        3f2a91c0-…
```

## Glossary impact

- **Past session** drops its *(expected)* marker. Until linking lands, every past session is
  unlinked.
- Adds **Transcript**: Claude Code's `~/.claude/projects/<folder>/<sessionId>.jsonl`, one per
  session; internal format, append-only by observation. A `subagents/` file is not one.
- Adds **Last activity**: the timestamp of a transcript's last entry. It orders past sessions.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-16](LAZY-16-startup-cost-of-transcripts.md), [LAZY-21](LAZY-21-keeping-the-screen-current.md). Draw the first frame from the registry and the cache; rescan the misses in the background.

Closed after [#14](https://github.com/maxenceng/lazyclaude/pull/14). Review changed the Model decision in several places. ADR 0023 records the decisions, and the section above is left as decided:

- Live sessions are placed by the same rule as past ones: through git on every registry read, and by the path rule when git cannot place the `cwd`. Without it, a session still running in a removed worktree is listed nowhere: not live, because git cannot place it, and not past, because its live id hides its past row. A `deferred:` note on `SessionManager` covers git running once per distinct `cwd` on every read.
- A change to the transcripts reuses the last read of the live sessions (`LastLiveRead`) rather than reading the registry again. Both watchers deliver under one lock in `SessionManager`.
- The watcher does not hold `ProjectLocatorPort`. The domain hands it a `TranscriptLocator`, so locating stays a domain decision and runs once per new transcript.
- `SessionTitle`, `AiTitle`, `FirstPrompt`, `NoTitle` and `SessionId` sit in `session.published`, not the domain, because the shell's `SessionText` reads them.
- `LiveSessions` became `AvailableSessions` (live list, past list, clock). One registry read is a `RunningSessions`: the id of every session running on the machine, which hides its past row even when it belongs to another project, plus this project's live sessions. `AvailableSessions` holds whatever it is given; only the manager keeps a live id out of the past list.
- `TranscriptCacheRepository` became `TranscriptCacheFile`. A `*Repository` that implements no `*Port` now fails the build (ADR 0011).
- `WatchFallback` split into `PollingFallback`, `CacheFallback` and `RootFallback`, one per warning the watch can raise. A transcripts root that is missing raises none.
