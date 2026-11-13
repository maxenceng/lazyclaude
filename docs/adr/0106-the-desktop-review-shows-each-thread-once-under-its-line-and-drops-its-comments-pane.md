# 106. The desktop review shows each thread once under its line and drops its Comments pane

Date: 2026-11-13

## Status

Accepted. Builds on ADR 0103's visual system. The TUI is unchanged.

## Context

The desktop review listed every thread twice: in a Comments pane beside the diff, and again as a small annotation under its
line. The Pull requests tab was one line per pull request with only its CI glyph, and the diff showed two number gutters,
cut long lines off, drew the cursor line as the focused title row's inverse, and drew code at 13px. A throwaway prototype
(variant R4, on the Pull requests list of the previous variants) was drawn and chosen.

## Decision

- **The Pull requests tab is a full-width table**, a row per open pull request: CI as a glyph and a word (`failing`,
  `running`, `passing`, `no checks`), the ticket it closes by number or an explicit dash, title with number, branch, comments
  in words, a `pending` badge for your unsubmitted review, and its age as elapsed time. The row facts come from `review`:
  `openedAt`, `comments`, `pendingReview` and `closing`. The window works out the age and never imports the board.
- **A thread shows once**, as a card across the whole diff under its line, hung from it, the line marked by a glyph and an
  edge. Open threads are edged blue; your pending comment is a neutral card edged and badged amber, with Edit and Delete
  buttons beside their keys. The desktop review has no Comments pane, so `Focus.Comments`, its keys and its selection are gone;
  `enter` on a line replies to the thread under it (a pending comment on the line is edited first), and a press on a line
  number opens the comment box as `c` does.
- **A thread with no line still shows**, once, as a card at the top of its file's diff under the header and before the first
  hunk, marked `outdated · was line 40` (`was lines 40–41` for a range), or only `outdated` when `review` keeps no original
  place. A pending comment says the same after `pending ·`. What `review` lists with no line (`listed` with a `noDiffLine`) is what is shown.
- **The files rail carries the pending review.** Each file shows its added and removed counts and word badges for its
  threads and your pending comments; the rail's foot shows the pending review and a Submit button that calls what `S` calls.
  The button and its key chip are tested at 4.5:1 against their fill.
- **The diff reads in one column of numbers**: the new number, the old one on a removed line. A long line wraps under a hanging
  indent. The cursor line is lifted and edged, never the focused title row's inverse. Code is 14px or more, and every ink on
  the list and the review is a token pair tested at 4.5:1. Counts are words; there is no emoji.
- **One-side edges are kept**: the CI stripe on a row, the thread, pending and diff-line edges, though impeccable's detector
  calls them side tabs; each state also has its word or glyph, so colour is never alone.

## Consequences

- The TUI keeps its Comments panel, so the two frontends no longer match on this screen: a reader of the TUI will expect a
  Comments panel the window does not have. The window needs none, as every thread is under its line or at the top of its file,
  and its read keys (`enter`, `x`, `c`, `r`) act where the thread is.
- Replying by key to a thread whose line also holds a pending comment reaches the pending comment first; the thread's Reply
  button still works.
- The protocol's `listed` list is read by the window only for threads with no line.
