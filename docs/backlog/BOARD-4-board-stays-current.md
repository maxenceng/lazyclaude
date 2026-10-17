---
id: BOARD-4
status: in-review
context: board
type: feature
tracker_id: 75
created: 2026-10-10
---

# The board stays current: watchers on `docs/backlog/`, the worktrees' backlog folders and the refs, plus `R` to refresh

## Acceptance criteria

- [x] Saving, adding or removing a file in the root `docs/backlog/` updates the board without a
      restart; the several events of one editor save make one re-read.
- [x] Editing a ticket's file in a worktree's `docs/backlog/` updates the board; only that
      ticket's own file counts.
- [x] A commit on a ticket branch, a checkout, or a branch created or deleted updates the board,
      a packed ref included.
- [x] A worktree added after start is watched; one removed is no longer watched.
- [x] A steady stream of changes still re-reads within the batch's maximum wait — the docs tree
      too, which today waits until the stream stops.
- [x] The selection follows the ticket id. A ticket that vanishes, or moves into a folded group,
      leaves the cursor on the row that took its place. An open ticket detail is redrawn and
      keeps its scroll position.
- [x] Nothing flashes or says "refreshed".
- [x] `R` re-reads the board, the docs tree, both `gh` fetches, and the sessions and transcripts.
      An `R` while a `gh` run is under way never starts a second one.
- [x] A tree that cannot be watched is polled every 5 s instead, with one warning per tree,
      `board: cannot watch <tree> (…); checking every 5 s`, naming `fs.inotify.max_user_watches`.
      lazyclaude still starts.
- [x] A failed re-read keeps the last good board and raises one warning; it is never dropped
      silently.
- [x] A ticket created while lazyclaude runs links sessions without a restart.

## Model decision

New behaviour on `board`'s existing read — no new aggregate, no new context. `context: board`
stays: `R` sits in the shell and wakes review, docs and session through their primary adapters,
with no new edge.

- **Shared kernel.** A recursive tree watcher in `shared.infrastructure.secondary`: one
  `WatchService` registration per directory, new directories registered as they appear,
  batching over ~100 ms with a maximum wait, `OVERFLOW` as a rescan, and 5 s polling when the
  watch cannot be set up. `docs`' watcher moves onto it in this change; the session registry
  and transcript watchers stay as they are, since a sweep and a per-file settle are different
  machinery. An ADR records the kernel watcher — the "new decision" ADR 0016's Consequences
  asked for.
- **Board.** A board watch port, implemented on the kernel watcher, over the root
  `docs/backlog/`, each worktree's `docs/backlog/`, and in git's common directory
  `refs/heads/**`, `packed-refs`, `HEAD` and `worktrees/**` (a worktree's `HEAD` moves which
  worktree holds a branch). A change in `worktrees/` adds or drops worktree watches. Every
  batch re-reads the whole board, as ADR 0016 does for docs: the watcher reports a change,
  never what changed. The re-read runs on the watcher thread and is swapped in on the render
  thread, as the merged-set re-read already is.
- **Review.** The PR watch gains a "run now" for `R`; two runs still never overlap.
- **Shell.** `R` re-reads the board, the docs tree, both `gh` fetches, and sessions and
  transcripts. The session-link ticket ids follow the board's re-reads rather than start-up's.

Rejected: a re-read of only the ticket an event touches (mapping events back to tickets costs
more than the whole re-read saves); board's own copy of the watch loop (a fourth copy, against
ADR 0016's Consequences); unfolding a group so the selection can follow a ticket into it
(changes a fold nobody asked to change).

## Design direction

Nothing new on screen but `R refresh` in the key bar. A refresh shows only as the changed rows.

## Glossary impact

- *Polling* gains `board`: the backlog, worktree and ref watches fall back to it the same way.
- adds **Refresh** (`shell`): `R`, re-reading the board, the docs tree, the pull requests and
  the sessions at once.
- Nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-21](LAZY-21-keeping-the-screen-current.md). Selection follows ids; a watch that can't be set up falls back to 5 s polling with a warning.
