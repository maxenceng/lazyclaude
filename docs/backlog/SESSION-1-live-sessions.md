---
id: SESSION-1
status: in-review
context: session
type: feature
tracker_id: 78
created: 2026-10-10
---

# The Sessions panel shows live sessions from the registry with their state and marker: the liveness sweep, the watcher, and `claude agents --json` as fallback and contract test

## Acceptance criteria

- [x] The Sessions panel lists every live session whose `cwd` belongs to this project — the
      main worktree's root from `git rev-parse --git-common-dir`, so a session in a ticket
      worktree or a subdirectory is listed — as one flat list, whatever ticket is selected.
- [x] A live session in another project, or whose `cwd` is outside git or no longer exists,
      is not listed.
- [x] A registry record is live only when its `pidDomain` is this machine's, `/proc/<pid>`
      exists and `/proc/<pid>/stat` field 22 equals `procStart`. A record that fails any of
      the three is not listed, whatever its `status` says — including a dead PID reused by
      another process.
- [x] A record with `entrypoint: sdk-cli` is not listed.
- [x] Each row shows the state glyph, the registry `name` and how long ago the session
      started: `!` red for `waiting` + `permission prompt`, `?` yellow for `waiting` with any
      other reason, `•` cyan for `idle` (your turn), `∼` green for `busy` or `shell`.
- [x] Rows are ordered by start time, newest first, and never reorder when a state changes.
- [x] A session's state change shows within a frame of Claude Code rewriting its record,
      through a watch on `~/.claude/sessions/`; a session whose process died disappears
      within ~2 s, through the liveness sweep, though its record is never rewritten.
- [x] A `.key` file in `~/.claude/sessions/` is ignored.
- [x] Selecting a session puts its facts in the main view: name, state (with the
      `waitingFor` reason when waiting), `cwd`, pid, start time and Claude Code version.
      The selection follows the session id across refreshes; if the selected session dies,
      the cursor lands on the row that took its place.
- [x] With no live session in this project, the panel says so instead of standing empty.
- [x] When `~/.claude/sessions/` is missing or unreadable, or a record no longer parses,
      sessions come from `claude agents --json` instead, polled every ~2 s, with one warning
      naming why. While it is in use, `sdk-cli` sessions are listed (it has no `entrypoint`).
- [x] When neither source answers — no usable registry and `claude agents --json` fails or
      is not on the PATH — the panel shows *sessions unavailable* with the reason, and
      lazyclaude still starts.
- [x] A watch on `~/.claude/sessions/` that cannot be set up leaves the ~2 s sweep carrying
      state changes alone, with one startup warning; lazyclaude still starts.
- [x] `make contract` compares the registry adapter with the `claude agents --json` adapter
      over whatever sessions are live and fails on any disagreement in pid, session id,
      `cwd`, `name` or state. It is outside `make test` and CI, run by hand after a Claude
      Code upgrade.

## Model decision

**A new context, `session`, created by this ticket** — already on the context map as
*expected* (LAZY-12), so this is the planned first ticket into it, not a new context to raise.
It is built on `docs`' shape (query, application service, manager, a read port and a watch
port), since it too reads and watches files outside the project.

- **Domain.** `LiveSession` (session id, pid, name, `cwd`, start time, Claude Code version,
  `SessionState`) and `SessionState`, a sealed type: `Busy | YourTurn | WaitingForPermission
  | WaitingForInput(reason)`. The mapping from registry `status`/`waitingFor` to state, the
  `sdk-cli` exclusion, the filter to this project and the newest-first order are the
  `SessionManager`'s rules. A failure of both sources is a value, `UnavailableSessions`, as
  `docs` does with `UnavailableDocs`.
- **Ports.** `LiveSessionPort` (read every live session, liveness already checked) and a
  watch port with a callback on the watcher's own thread, as `DocsWatchPort`.
- **Adapters.** `SessionRegistryRepository` reads `~/.claude/sessions/*.json` and runs the
  liveness check against `/proc` and this machine's `pidDomain` (`/etc/machine-id` and
  `/proc/self/ns/pid`). `ClaudeAgentsRepository` runs `claude agents --json` behind the same
  port. Choosing between them when the registry is unusable is the adapter side's call,
  not the domain's. The watch adapter combines the `WatchService` and the ~2 s sweep.
- **Time.** Age needs "now": a `java.time.Clock` is injected from `LazyClaude.main`, never read
  statically.
- **JSON.** Jackson joins the dependencies, used only in secondary adapters; later tickets
  (transcripts, `gh --json`) parse JSON too. An ADR records it.
- **Locating a project moves to the shared kernel.** `ProjectLocatorPort` and its git
  adapter leave `board` for `shared.domain` and `shared.infrastructure.secondary` (ADR 0010,
  ADR 0017's pattern), so `board` and `session` share one rule for "which project is this
  path in". `session` asks it for each distinct `cwd`; a path it refuses belongs to no
  project here. An ADR records the move, and `docs/context-map.md`'s `shared` row with it.
- **Shell.** It asks `SessionQuery` for a `SessionsView` with this project's `Project` (ADR
  0015's hand-over), fills the Sessions panel with session rows, and lets `mainViewOwner`
  include `SESSIONS`.
- *Starting* (LAZY-15) belongs to hosted sessions, which don't exist until SESSION-4, so it
  is not in this ticket. Neither is linking (SESSION-3) nor the Elsewhere panel (SESSION-7).

## Design direction

The Sessions panel in the left column, as LAZY-10 placed it, focused here:

```
╭ Sessions ─────────────────────╮
│ ! lazyclaude-b6           2h  │
│ ∼ lazyclaude-3f          12m  │
│ • claude-tools-a1         1d  │
╰───────────────────────────────╯
```

- Row: glyph, then `name`, then age right-aligned (`12m`, `2h`, `1d`). The glyph alone
  carries the colour.
- `j`/`k` move the selection when the panel has focus; `alt+j`/`alt+k` and `h`/`l` reach it
  as they reach Board and Docs today. It grows when focused, like the others.
- Empty: `no live session` dimmed. Unavailable: `sessions unavailable: <reason>` dimmed.
- Main view for a selected session:

```
lazyclaude-b6  ! waiting for permission
cwd      /home/max/projects/lazyclaude.worktrees/BOARD-3-status-from-the-branch-copy
pid      17234
started  2026-10-10 14:05 (2h ago)
version  2.1.296
```

## Glossary impact

- Adds **Live session**: a session whose process runs, as the registry lists it after the
  liveness check, or `claude agents --json` when the registry is unusable.
- Adds **Registry**: Claude Code's `~/.claude/sessions/<pid>.json` records, one per running
  process; internal and undocumented, so never trusted without the liveness check.
- **Busy**, **Your turn** and **Waiting** drop their *(expected)* marker.
- **Project**'s glossary entry gains no new meaning, but its locating now lives in `shared`.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-2](LAZY-2-session-discovery.md), [LAZY-8](LAZY-8-detecting-a-waiting-session.md), [LAZY-13](LAZY-13-claude-agents-spike.md). `sdk-cli` sessions are excluded. A session still on the trust dialog shows as *starting* ([LAZY-15](LAZY-15-trusting-ticket-worktrees.md)).
