---
id: REVIEW-2
status: in-progress
context: review
type: feature
tracker_id: 88
created: 2026-10-10
---

# The PR review screen, read-only: Files panel, unified diff, and existing threads inline

## Acceptance criteria

- [ ] `p` on a ticket whose ticket branch is the head of an open pull request (REVIEW-1's
      exact join) opens the review: the left column's four panels are replaced by **Files**
      and **Comments**, the main view shows the PR's diff, and the status line and the dock
      stay. `esc` leaves it and restores the four panels with the same ticket selected.
- [ ] `p review` is in the key bar only on such a ticket. On a ticket with no open PR, one
      whose PR head only shares the id prefix, or while the open-PR fetch is `checking…` or
      `unknown`, `p` is not listed and does nothing.
- [ ] Diff and threads are read once when `p` opens the review, off the render thread, with
      a dim `loading…` in the main view meanwhile, and read again on `R`. No timer reads them.
- [ ] The main view shows the diff of every file in one scroll, each file under a file
      header and each hunk under its `@@` header, `+` lines green, `-` lines red. It draws
      only the visible slice, so a diff of 20k lines scrolls without a per-frame cost in its
      length.
- [ ] A line cursor moves over `+`, `-` and context lines and skips file and hunk headers:
      `j`/`k` one line, `J`/`K` to the first line of the next/previous file, `ctrl+d`/`ctrl+u`
      half a page. The Files selection follows the cursor's file; `enter` on a file in Files
      moves the cursor to its first line.
- [ ] Files lists each file with its status letter (`A` added, `M` modified, `D` deleted,
      `R` renamed, shown `old → new`), its name, and `◆n` for its open threads (unresolved,
      not outdated); no `◆` when it has none.
- [ ] A thread shows inline under the line it sits on: its `line` on the new side for
      `RIGHT`, on the old side (the `-` line) for `LEFT`. A range shows `L12–18` from
      `startLine` to `line`. Its comments follow as author and body in plain wrapped text,
      replies indented under the first.
- [ ] A resolved thread is hidden inline until `r`, which shows resolved threads dimmed;
      `r` again hides them.
- [ ] An outdated thread (no `line`) is never placed inline. Comments lists every thread in
      diff order (file, then line), outdated ones last and marked `outdated`, resolved ones
      dimmed; `enter` on a placed thread moves the cursor to its line, and does nothing on
      an outdated one.
- [ ] When the diff cannot be read — `gh` missing, not authenticated, offline, a non-zero
      exit, or a run killed after 30 s — the main view shows `PR unknown (<reason>)` and
      Files is empty. A diff GitHub refuses as too large shows
      `diff too large for GitHub (<reason>)`. Threads still list in Comments when they were
      read; when they cannot be, Comments shows `threads unknown (<reason>)`. Nothing reaches
      the status line, nothing blocks, and `esc` still leaves.
- [ ] A read that fails on `R` after one succeeded replaces what was shown with the reason;
      the last good diff or threads are never shown as current.
- [ ] The open-PR fetch passes `--limit 100`: with more than 30 open PRs (`gh`'s default),
      every ticket's PR up to 100 still gets its glyph.
- [ ] `make contract` runs `gh pr diff --color never` and the threads GraphQL query against
      a real pull request of this repository and asserts both parse.

## Model decision

**New behaviour in `review`**, beside the open- and merged-PR reads; nothing about how those
are read changes except the limit.

- **Domain.** `PullRequestDiff`: `DiffFile`s, each with a `FileStatus` (added, modified,
  deleted, renamed), its path (and old path when renamed) and `Hunk`s of `DiffLine`s — kind
  (`+`, `-`, context) and old/new line numbers. `Threads`: each `Thread` has its path,
  `line`/`startLine`, side, resolved and outdated, and its comments (author, body). Each read's
  outcome is sealed, as `PullRequests` is: loading, read, or unavailable(`Reason`); too large
  is one of the unavailable reasons, not its own shape. Ports: `PullRequestDiffPort`,
  `ThreadPort`, both keyed by the `PullRequestNumber`.
- **Adapters.** A `gh` repository runs `gh pr diff <n> --color never` through `GhCommand`
  and parses the unified diff (status letters from `new file`, `deleted file`,
  `rename from`/`rename to`). Another runs `gh api graphql --paginate` over
  `reviewThreads(first:100)` and parses it with Jackson (ADR 0020); comments are
  `first:50` per thread with a `// deferred:` comment. Same 30 s patience and failure-to-reason
  mapping as `GhPullRequestsRepository`. The open-PR argv gains `--limit 100` and a
  `// deferred:` comment, as the merged one has.
- **Shell.** `p` uses REVIEW-1's join to find the PR number and asks `review`'s query for
  diff and threads, the way `readWorktree` reads on a keypress. Review mode replaces the left
  column's panels; a new viewport `Element` beside `DetailElement` draws the visible slice of
  diff rows (lines plus their inline threads) and owns the cursor. `R` re-reads both while
  the review is open.
- **ADR.** One records that the review swaps the left column for its own panels, since ADR
  0008 fixes it at four; the diff source (`gh pr diff`, not `/pulls/{n}/files`) is LAZY-6's
  and needs none.
- **Not here.** Pending comments, `✎n`, the Review panel, `c`/`v`/`S` and submit are
  REVIEW-3, which builds on this cursor. No fallback to per-file patches for a too-large diff:
  no ticket PR has come near GitHub's limits.

## Design direction

LAZY-11's variant A panels with B's inline threads:

```
╭ Files ─────────────────╮╭ PR #24 The PR review screen ──────────────────────╮
│ M Shell.java        ◆1 ││ ── src/main/java/…/Shell.java ─────────────────── │
│ A DiffElement.java     ││ @@ -120,6 +120,8 @@ final class Shell             │
│ R Old.java → New.java  ││   120   120  private final ReviewQuery reviews;   │
╰────────────────────────╯│ ▌     121  + private final DiffViewport diff;     │
╭ Comments ──────────────╮│       ◆ L121 alice                                │
│ ◆ Shell.java:121 alice ││         Why not reuse DetailElement?              │
│   Old.java  outdated   ││           bob  It assumes markdown.               │
╰────────────────────────╯│   121   122  private final Focus focus;           │
                          ╰───────────────────────────────────────────────────╯
```

- Files and Comments share the left column's height, Files on top; `h`/`l` move between
  them and the main view, as today.
- Diff rows: old and new line numbers in two dim gutters, then the line; the cursor row is
  reversed. File headers are a rule with the path; hunk headers are dim.
- Inline thread rows are indented past the gutters, `◆` and range in the thread colour,
  author bold; resolved ones dimmed when `r` shows them. The cursor never lands on them.
- Key bar in review: `j/k line  J/K file  ^d/^u page  r resolved  R refresh  esc back`.
- `PR unknown (…)`, `diff too large for GitHub (…)`, `threads unknown (…)` and `loading…`
  are dimmed.

## Glossary impact

- *Thread* drops its *(expected)* marker, in `review`.
- New in `review`: *Diff* (the pull request's unified diff, every file in one scroll),
  *Hunk* (one `@@` section of a file's diff), *Diff line* (a `+`, `-` or context line in a
  hunk; the only rows a cursor or a comment lands on).
- Nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-6](LAZY-6-github-pr-review-api.md), [LAZY-11](LAZY-11-pr-review-interaction.md). A huge diff needs a custom viewport `Element` ([LAZY-4](LAZY-4-tamboui-capabilities.md)).

Refined 2026-10-18. The open-PR fetch's missing `--limit` (it fell back to `gh`'s 30) was
found while refining and folded in here rather than given its own ticket. No `gh` call had a
contract test before this one.
