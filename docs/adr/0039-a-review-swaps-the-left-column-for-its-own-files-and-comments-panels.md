# 39. A review swaps the left column for its own Files and Comments panels

Date: 2026-10-18

## Status

Accepted. Amends ADR 0008, which fixes the left column at four panels. Amended by ADR 0040, which adds a third panel, Review.

## Context

ADR 0008 puts four panels on the left (Board, Sessions, Docs, Elsewhere) and a main view that follows the selection. A pull request's
review needs two panels of its own, the files of the diff and the threads on it, beside a main view that is the diff. The four panels
have nothing to say about a diff, and squeezing six into the column leaves each too short to read.

## Decision

- **While a review is open the left column holds Files over Comments, and nothing else.** The Board, Sessions, Docs and Elsewhere panels
  are not drawn; the status line, the main view's place and the dock stay as they are. The main view is the diff.
- **`p` opens it, `esc` leaves it.** `p` is offered only on a ticket whose ticket branch is the head of an open pull request, the join
  the board's glyphs use. `esc` restores the four panels with the board's selection as it was; it was never moved.
- **Focus gains `FILES` and `COMMENTS`, which stand beside each other and not beside the four.** `alt+j` and `alt+k` run from Files to
  Comments and no further; `h` and `l` move between them and the main view as they do today.
- **The diff and the threads are read once on `p`, off the render thread, and again on `R`.** No timer reads them. A read lands only in
  the review it was begun for and only if no later read of its kind was begun.
- **Where a thread meets a diff line is `review`'s rule, not the shell's.** The domain joins the diff with the threads, giving each
  thread its diff line and listing them in diff order. Whichever read has not answered yet places nothing: until the diff is read,
  every thread has no line. The shell asks for the join through `ReviewQuery` whenever either read lands, and draws what it gets.
  A `LEFT` thread's row in Comments writes its lines as `-100`: in diff order an old line 100 can sit above a new line 50.
- **The diff source is `gh pr diff`, not `/pulls/{n}/files`**; that is LAZY-6's decision and needs no record here.

## Consequences

- Every key that assumed the left column holds the four panels (`n`, `d`, `o`, `enter`) answers nothing in a review, since their focus
  is not one of the four.
- The review is read-only. Pending comments, the Review panel and submitting build on this cursor and these panels, and need no new
  column.
- A pull request's diff of up to GitHub's limits (300 files, 20,000 lines) is held whole in memory and indexed once per read; a frame
  draws only the rows that fit the main view.
