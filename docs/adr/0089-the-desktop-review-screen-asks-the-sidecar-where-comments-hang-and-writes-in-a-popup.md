# 89. The desktop Review screen asks the sidecar where comments hang and writes them in a popup

Date: 2026-11-05

## Status

Accepted. Builds on ADR 0009 and ADR 0041 (the pending review), ADR 0051 (the mouse), ADR 0057 (the protocol client) and
ADR 0085 (a controller per context).

## Context

The TUI's review places threads and pending comments on the diff, and decides whether a range can take a comment, by calling
`ReviewQuery` in-process on the render thread. Over the protocol each of those is a call, and the answer arrives later than
the key that asked for it. The TUI also draws a comment box inline under its line and moves the cursor with the wheel;
a webview has a native popup and a native scroll.

## Decision

- **`threadsInDiff`, `pendingInDiff` and `anchorOf` are asked of the sidecar, never worked out again in TypeScript.** After
  each read, and each write's answer, the session asks where the threads and the pending comments sit and builds its rows
  from the answer; only the latest ask lands, and one that fails is the diff's fault, with its retry. `c`, `v` and a drag wait for `anchorOf` before they open the box or mark the
  range, and a refusal is the notice `review` raises on the status line, in the sidecar's words.
- **While a review is open its panels replace the Board, the Docs and the main view**: Files over Comments, with the Review
  panel under them, and the diff beside. Their focus is the layout's, as the TUI's `FILES`, `COMMENTS` and `MAIN` are, so
  zoom, the hidden main view and the dock work as they do there.
- **Every comment, reply and the summary is written in one popup.** The TUI's inline box is not drawn; a reply shows its
  thread above the text. `ctrl+s` saves and `esc` throws it away, both read by the box itself: a key typed in it is not a
  binding of the window (`data-typing`). A save in flight makes it read-only, `esc` included; GitHub's refusal keeps the
  text and says why. A call that failed may still have written, so it closes the box, says so on the status line and reads
  the pending review again, rather than offer a save that could write the comment twice.
- **The confirm after the summary is a popup answered by `y`; any other bound key, and `esc`, keeps the review pending.**
  The TUI's "any other key" cannot reach a window that binds only the TUI's keys.
- **The TUI's keys are reused as their actions**: `c` is the one that captures on the Board and comments in a review; `v`,
  `x`, `S`, `J`, `K`, `r` and `y` are new to the table and mean nothing outside a review.
- **The mouse is the browser's**: a press puts the cursor on a line, a second press on it is `enter`, and a drag from one
  line to another marks the range between them, read from wherever the left button is let go, nearest line when it is not
  over one. The wheel scrolls the view and does not carry the cursor along; the view follows the cursor on a key.

## Consequences

- A diff read, then its placement, is two round trips before threads show under their lines; the diff shows first.
- A range dragged upward asks for `first` the line released on and `last` the line pressed on.
- deferred: after a submit the TUI offers to respond to the review when the ticket allows it; the window leaves the review
  and does not — close it when the window starts a session from a prompt.
- The diff lines are numbered in the window, files then hunks then lines, to match the index `threadsInDiff`,
  `pendingInDiff` and `anchorOf` speak; the `DiffLine` view does not carry it.
- Some of the TUI's screen rules are ported from the shell rather than asked of `review`: what a submit sends (the summary
  typed, else the adopted review's body), resolved threads kept out of the diff, pending comments listed before threads,
  the last-written pending comment on a line as the one `enter` and `x` act on, and the join of a ticket's branch to its
  pull request.
- deferred: the window counts diff lines itself and nothing ties the count to the domain's — close it when the domain's
  numbering changes, by putting the index on the `DiffLine` view.
- deferred: the ported screen rules have a copy in the shell and one in `frontend/src/review/` — close it when either copy
  changes, by moving the rule into `review` and asking it.
- deferred: a half page is a count of the lines the view shows, measured from the first line's height; a diff whose lines
  wrap or differ in height pages by a different amount — close it when a page is seen to land wrong.
