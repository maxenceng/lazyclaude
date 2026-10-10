---
id: LAZY-21
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-8, LAZY-9, LAZY-16, LAZY-19]
---

# How do the board, the docs tree, the sessions and each branch copy stay current?

## Question

How do the board, the docs tree, past sessions and each ticket's branch copy stay current while lazyclaude runs?

LAZY-8 already settled live sessions: `WatchService` on the registry plus a ~2 s liveness sweep. Still open:
- the backlog and `docs/**` in the root checkout: `WatchService`, polling, or a manual refresh key;
- each ticket's branch copy (LAZY-9), which changes in its worktree's file or in a commit on its branch, not in the root checkout;
- new and growing transcripts, for past sessions and links (the cost depends on LAZY-16);
- the PR state behind the preselected verb, and the open-PR list behind the board's PR and CI glyphs (LAZY-19: one `gh pr list --state open … statusCheckRollup` call, ~0.5 s).

## Kind

grilling

## Resolution

Grilled 2026-10-10. Live sessions stay as LAZY-8 settled them. Everything else is
watched, never polled, and `gh` runs on a timer driven by focus.

- **Root checkout** (`docs/backlog/`, `docs/**`): a `WatchService` on every directory
  under `docs/`, because Java's watcher doesn't cover subdirectories on Linux. New
  directories get a watch as they appear. Events are batched over ~100 ms, since one
  editor save fires several. An `OVERFLOW` rescans that tree.
- **Branch copies:**
  - a watch on each ticket worktree's `docs/backlog/` (only the ticket's file counts);
  - a watch on the main repo's `.git/refs/heads/` and `.git/packed-refs`. When a ticket
    branch's ref moves, its `git show <branch>:<file>` is read again.
  - `.git/worktrees/` changing adds or drops worktree watches. Nothing is polled.
- **Transcripts:**
  - a watch on `~/.claude/projects/` and on each folder under it;
  - a grown file is parsed only from the byte offset the LAZY-16 cache holds, because
    the link fields (`ai-title`, `pr-link`, `/ticket` commands, `gitBranch`) arrive as
    new lines;
  - a file that shrank, or was replaced (new inode), is rescanned in full;
  - each file waits ~1 s after its last change before being read, so a busy session
    isn't parsed line by line.
- **Open-PR list** (LAZY-19's call): run at startup, then every 60 s while the terminal
  has focus (`?1004`, already used by LAZY-8). Run at once when focus returns if the
  last run is older than 60 s, and when any `refs/remotes/origin/<ticket branch>` moves
  (a push). Never while unfocused, and never two runs at once.
- **Merged state** (the gap LAZY-17 left): a second call,
  `gh pr list --state merged --limit 100 --json headRefName,headRefOid,number` (~0.5 s,
  no checks), on the same schedule, matched exactly on the ticket branch. Its
  `headRefOid` also serves LAZY-17's "commits beyond the merged head" refusal. Until it
  answers, or while GitHub is unreadable, a branch with no open PR still counts as the
  status source.
- **`R`** re-reads everything: both `gh` calls and every rescan. It's lazygit's refresh
  key and a plain panel key, not one of LAZY-20's reserved chords, so it isn't
  configurable. It's shown in the key bar.
- **A watch that can't be set up** (`max_user_watches` used up, or no inotify): that tree
  is polled every 5 s instead, with one startup warning naming the tree and the sysctl.
  lazyclaude still starts: this isn't a configuration error.
- **On screen:**
  - the selection follows the ticket id, doc path or session id, never the row number;
  - if the selected item vanishes, the cursor lands on the row that took its place;
  - an open doc or ticket detail is redrawn and keeps its scroll position;
  - a ticket that changes status column (zoom) keeps its selection;
  - nothing flashes or says "refreshed".
- **Ownership:**
  - `board` owns the root-checkout, worktree and ref watchers;
  - `session` owns the transcript watcher, next to the registry watcher;
  - `review` owns both `gh` calls and the merged state;
  - `shell` owns focus, `R` and the scheduling.

  `board` still knows nothing of pull requests: `shell` hands its status read the set
  of merged ticket branches as plain branch names, the same join as LAZY-19's PR glyph.
  No new edge.
- No ADR: every part of this is cheap to reverse.
