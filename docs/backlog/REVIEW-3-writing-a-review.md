---
id: REVIEW-3
status: in-progress
context: review
type: feature
tracker_id: 89
created: 2026-10-10
---

# Writing a review: `c`/`v`/`ctrl+s`/`S` into GitHub's pending review, adopting one that already exists, submitting as *Comment*, then offering `/ticket respond`

## Acceptance criteria

- [ ] `c` on a `+`, `-` or context line of the diff opens the comment box inline under that
      line; `v` starts a range and `v` again ends it, and a range that leaves its hunk is
      refused with a message, the range unchanged. File and hunk headers take no comment and
      `c` on them does nothing.
- [ ] In the box `enter` is a newline, `ctrl+s` saves and `esc` cancels, throwing the text
      away. While a save is out the box shows a dim `saving…`; the `✎` appears only once GitHub
      has confirmed it. A save that fails — offline, `gh` not authenticated, a refusal, 30 s
      without an answer — keeps the box open with the text and `not saved (<reason>)` under
      it; `ctrl+s` retries.
- [ ] The first saved comment creates the pending review; later ones add to it. A comment on a
      `-` line is `LEFT` on its old line number, on a `+` or context line `RIGHT` on the new;
      a range carries its first line as start.
- [ ] In the diff, `enter` on a line with a `✎` opens its last pending comment in the box and
      `x` deletes that one; in Comments, `enter` and `x` act on the selected pending comment.
      Saving an empty comment deletes it. On a line without a `✎`, `enter` and `x` do nothing.
- [ ] `c` in Comments on a selected thread opens the box for a reply: inline under the thread
      when it is placed, in a popup over the main view with the thread drawn above when it is
      outdated. A saved reply goes into the pending review and shows as pending under the thread.
- [ ] When the review screen opens on a PR that already has a pending review of yours — from
      github.com or an earlier run — it is adopted: its comments and summary show and later
      saves add to it. A pending comment shows only as `✎`, never also as a `◆` thread, and
      never counts toward `◆n`.
- [ ] Files shows `✎n` beside a file's `◆n` for its pending comments; no `✎` when it has none.
      Comments lists pending comments first, in diff order, then the threads.
- [ ] A Review panel under Files and Comments shows the summary's first line and the pending
      count. When the PR's head is no longer the commit the pending review was written on, it
      warns `written on <sha>, head is now <sha>`, and submitting still goes through.
- [ ] `S` opens the summary in a popup box over the main view, holding the current summary.
      `ctrl+s` saves it, then a confirm submits the pending review as `COMMENT`; `n` or `esc`
      on the confirm leaves the summary saved and the review pending. Nothing is checked
      locally: a submit GitHub refuses shows `not submitted (<reason>)` and the review stays
      pending.
- [ ] After a submit GitHub accepts, a dialog offers `/ticket respond <ID>` in a new dock tab
      in the ticket's worktree, only when `board` allows `respond` for the ticket's status;
      otherwise, and on `esc`, the review closes and the ticket is selected.
- [ ] The ticket detail of a ticket with an open PR shows `✎n pending`, read when the selection
      lands on it, on `R`, and when the review screen is left — off the render thread, a late
      read dropped. No pending review shows nothing; a read that fails shows a dim
      `pending unknown (<reason>)`.
- [ ] Leaving the review, quitting or a crash loses nothing saved, and quitting asks nothing
      about the review. A box open with unsaved text when the review is left is discarded.
- [ ] `make contract` creates a pending review with a line comment and a reply on a pull
      request of this repository, reads it back through the pending-review read, asserts the
      thread read does not return its comments as threads, then deletes it.
- [ ] Approve, request changes and resolving a thread are not offered.

## Model decision

**New behaviour in `review`: a new aggregate, `PendingReview`**, beside the diff and thread reads.

- **Domain.** `PendingReview`: its id, the commit it was written on, its `Summary`, and its
  `PendingComment`s. A pending comment has its id, body, and an anchor — on the diff
  (path, side, first and last line, the shape `PlacedThread` already has) or a reply to a
  thread by that thread's id. Its reading is sealed like the others: loading, read (with the
  PR's node id and head commit, and the pending review or none), or unavailable(`Reason`).
  `PendingReviewManager` places pending comments on diff lines with the same rule as threads
  (`PullRequestDiff.place`), tells a moved head, and refuses a range across hunks as a
  `DomainException`. One port, `PendingReviewPort`: read, create, add thread, reply, edit,
  delete, set summary, submit.
- **Adapters.** `GhPendingReviewRepository` over `GhCommand`, same 30 s patience and reasons.
  One GraphQL query reads the PR's `id`, `headRefOid` and `reviews(states: PENDING, first: 1)`
  with its id, `commit.oid`, body and comments (`id`, path, line, startLine, side, `replyTo`).
  Mutations: `addPullRequestReview` (no event), `addPullRequestReviewThread`,
  `addPullRequestReviewThreadReply`, `updatePullRequestReviewComment`,
  `deletePullRequestReviewComment`, `updatePullRequestReview`, `submitPullRequestReview`
  with `COMMENT`. The thread query gains thread ids and leaves out comments of a pending
  review.
- **Primary.** `review`'s first command, `PendingReviewCommand` (writes, a `*Payload` in, a
  `PendingReviewView` out); the read goes through `ReviewQuery`.
- **Shell.** A comment box on TamboUI's `textArea`, inline in `DiffElement` or as a popup; the
  Review panel; the submit confirm and the respond dialog as prompts like the existing ones.
  The respond start is `hosting.startTicket` with `Verb.RESPOND`, offered only when the
  ticket's `Status` lists it.
- **ADRs.** One amends ADR 0039: the left column in a review holds a third panel, Review. One
  records that the pending review is one query with the PR's head and node id, and that
  pending comments are kept out of threads. CLAUDE.md's "every view is read-only" gains its
  carve-out: the pending review is the one write, by ADR 0009.
- **Not here.** Approve, request changes, resolving threads.

## Design direction

LAZY-11's layout with a Review panel under the other two:

```
╭ Files ─────────────────╮╭ PR #24 The PR review screen ──────────────────────╮
│ M Shell.java     ◆1 ✎2 ││   120   120  private final ReviewQuery reviews;   │
│ A DiffElement.java  ✎1 ││ ▌     121  + private final DiffViewport diff;     │
╰────────────────────────╯│       ✎ L121 you                                  │
╭ Comments ──────────────╮│         Does this need its own Element?           │
│ ✎ Shell.java:121       ││       ╭─────────────────────────────────────╮     │
│ ✎ DiffElement.java:8   ││       │ and the cursor too|                 │     │
│ ◆ Shell.java:90 alice  ││       ╰ ^s save  esc cancel ────────────────╯     │
╰────────────────────────╯│   121   122  private final Focus focus;           │
╭ Review ────────────────╮╰───────────────────────────────────────────────────╯
│ Looks close. Two nits. │
│ 3 pending              │
│ written on 9f3c2e1,    │
│ head is now 4b1d0a7    │
╰────────────────────────╯
```

- `✎` and pending rows in their own colour, distinct from the thread colour; author `you`.
- The box is indented past the gutters, as wide as the main view allows, growing with its
  lines; `saving…`, `not saved (…)` dim under it. The summary and an outdated reply use the
  same box as a popup over the main view, titled `Summary` / the thread's first line.
- Review panel: summary first line, `n pending`, the moved-head warning in the warning
  colour; `no pending review` dim when there is none.
- Key bar in the diff: `c comment  v range  enter edit  x delete  S submit` before the read
  keys; in Comments `c reply` replaces `c comment`; in the box `^s save  esc cancel`.
- Submit confirm on the status line: `submit review as Comment? y/n`. Respond dialog as a
  popup: `start /ticket respond <ID>?` with `y start  esc back`.
- Ticket detail: `✎3 pending` under the PR heading; `pending unknown (…)` dim.

## Glossary impact

- *Pending review*, *Pending comment*, *Summary*, *Submit*, *Moved head* drop *(expected)*.
- New in `review`: *Reply* — a pending comment answering an existing thread.
- New in `shell`: *Review panel* — the review screen's third panel: summary, pending count,
  moved-head warning.
- Nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-11](LAZY-11-pr-review-interaction.md), ADR 0009. Approve and request changes are out of scope.

Refined 2026-10-18. Replying to a thread was added while refining; the draft did not name it.
The ticket detail's `✎n pending` reads per selected ticket rather than riding the 60 s open-PR
read, which cannot see pending reviews.
