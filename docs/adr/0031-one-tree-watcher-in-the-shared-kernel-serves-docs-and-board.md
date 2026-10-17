# 31. One tree watcher in the shared kernel serves `docs` and `board`

Date: 2026-10-16

## Status

Accepted. The "new decision" ADR 0016's Consequences asked for; it moves the loop that ADR describes out of `docs`.

## Context

`docs` watches `docs/**` with a `WatchService` on every directory. `board` now has to watch three more kinds of tree: the root
`docs/backlog/`, the `docs/backlog/` of each worktree, and git's common directory for the refs. A second copy of the loop would be
the fourth in the codebase, with the same defects: the docs loop reports only once the events have been quiet, so a steady stream of
writes never reports, and a tree that cannot be registered sends the whole watcher to polling.

## Decision

- **`TreeWatcher` in `shared.infrastructure.secondary` is the one loop.** It takes `TreeScope`s: where a tree starts, which directories
  get a watch, and which changes count. It is one class, and each owner runs its own instance: one `WatchService` and one thread serve
  every scope of that owner, and `add` and `drop` change the set while it runs. A directory that appears gets a watch when it does, a
  moved or deleted one has its watches cancelled, and an `OVERFLOW` registers the scope again and reports.
- **A report is batched and bounded.** Events are collected until they have been quiet for 100 ms, or until 1 s has passed, whichever
  comes first, so a steady stream still reports. The watcher says that something changed, never what, as ADR 0016 has it.
- **Polling is per tree.** A scope that cannot be registered (`fs.inotify.max_user_watches` used up, no inotify) is checked every 5 s by
  modification time and size, and its owner is told once for that tree, with the cause naming the sysctl, however often the tree is
  dropped and added again. The other scopes keep their watches. lazyclaude starts either way.
- **`docs` and `board` own their scopes.** `docs` watches `docs/**` outside the backlog. `board` watches the root backlog, each
  worktree's backlog where only the ticket's own file counts, and in git's common directory `refs/heads/**`, `packed-refs`, `HEAD`
  and `worktrees/**`. A batch also reads `worktrees/` again, so a worktree added is watched and one removed is let go.
- **The session registry and transcript watchers stay as they are.** The registry's sweep and the transcripts' per-file settle are
  different machinery from a tree that is re-read whole.
- **A failed re-read of the board keeps the last good one and raises one warning** for the run, whatever happens after: a warning is
  never withdrawn (ADR 0022), so naming the mode again after a good read would only pile up copies of it. A board that cannot be read
  is never dropped silently.

## Consequences

- A worktree whose `docs/backlog/` does not exist yet is looked for again at the next batch, which the checkout itself produces under
  `worktrees/`. One whose `HEAD` is detached holds no branch and has no ticket file to watch.
- `TreeWatcher` picks the scope owning a key by the longest root, so two scopes nested in each other would share a key; none do.
- A directory that is swapped for another between a change and its re-registration is covered by the next `OVERFLOW` or batch, not by
  the first.
- `docs` and `board` each spend their own share of `fs.inotify.max_user_watches`, and each runs a thread; sharing one instance would
  need a callback per scope, which nothing yet asks for.
- The board's watch learns which branch a worktree holds from `worktrees/<name>/HEAD`, and the board's read learns it from
  `git worktree list`. When the two disagree, for a prunable or moved worktree, the watch follows the files and the next batch the
  read sees settles it.
- The docs loop's deferred limits (a `docs/` created after start is not watched, an exception ends the thread) carry over unchanged.
