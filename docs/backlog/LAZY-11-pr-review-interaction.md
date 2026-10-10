---
id: LAZY-11
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-4, LAZY-6, LAZY-10]
---

# How does the PR review screen work?

## Question

How does the PR review screen work?

Moving through a unified diff, picking a line or range, writing a comment, seeing pending comments and existing threads, writing the summary, submitting — and what happens to pending comments if the screen is left before submitting. Settled by reacting to a rough prototype.

## Kind

prototype

## Resolution

**Variant A's panels, B's inline comments, and C's GitHub pending review.** Picked from a
prototype of three screens. A was the lazygit staging view: panels plus a diff in the main view.
B was one full-screen scroll with everything inline. C was one file's diff beside a review-draft
pane, saved to GitHub. Each variant also had its own policy for unsent comments. The prototype lives on
branch `prototype/lazy-11-pr-review` (`prototypes/pr-review/index.html?variant=A|B|C`). Its A
still opens the comment box as an overlay. The decision is inline, as in B.

**Layout** (ADR 0008's frame, no zoom needed). `p` on a ticket opens its branch's open PR.
- The left panels become the PR's:
  - **Files**: status letter, name, `◆n` open threads, `✎n` pending comments.
  - **Comments**: the pending comments, then the threads. Outdated threads are listed here
    but never placed on a line. `enter` jumps to the line.
  - **Review**: the summary's first line, the pending count, and a warning when the head
    moved (below).
- The main view holds the unified diff of every file, in one scroll, with file and hunk headers.
  The dock keeps its place under it.
- Threads and pending comments show **inline**, under the line they sit on. Resolved threads
  are hidden until `r`.

**Keys** (lazygit's staging view, AZERTY-safe):
- `j`/`k` move a line, `J`/`K` move a file, `ctrl+d`/`ctrl+u` move a page, `h`/`l` change panel.
- `v` starts or ends a range. A range must stay inside one hunk, or the keys refuse with a
  message. A 422 would reject the whole review (LAZY-6).
- `c` comments on the line or range. The comment box opens **inline under the line**, as in B.
  In it, `enter` is a newline, `ctrl+s` saves, and `esc` cancels.
- `enter` on a line with a `✎` edits that comment. `x` deletes it. Saving an empty comment
  deletes it too.
- Comments go on any `+`, `-` or context line. A `-` line is `side: LEFT`, the old line.
  File and hunk headers take no comment.
- `S` opens the summary in the same kind of box. `ctrl+s` saves it, then a confirm submits.
  `esc` leaves the review.

**Pending comments live in GitHub's pending review** (ADR 0009):
- Each saved comment, edit or deletion goes to the server at once.
- The summary is the review's body. Submitting sends `COMMENT` to that review.
- Leaving, quitting or a crash loses nothing, and quit asks nothing about the review.
- The ticket detail shows `✎n pending`.
- An existing pending review of yours on the PR is **adopted** when the screen opens. GitHub
  allows one per PR, so this includes one started on github.com.
- If the head moved past the commit the review was written against, the Review panel warns
  (`written on 9f3c2e1, head is now …`) and submitting still goes through. GitHub marks
  comments on moved lines outdated.

**After submitting**, a dialog offers to start `/ticket respond <ID>` in a new dock tab, in the
ticket's worktree. `esc` declines and returns to the ticket.
