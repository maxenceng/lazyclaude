# 106. The desktop review shows each thread once under its line and drops its Comments pane

Date: 2026-11-13

## Status

Accepted. Builds on ADR 0103's visual system. Amends ADR 0089: the desktop review has no Comments pane, and the Review panel is
the files rail's foot. Keeps ADR 0090: `review` says where every comment shows, a line, a file's rule or after the diff, and the
window draws it there. The TUI keeps its layout; only an outdated thread's row changes, as below.

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
- **The open pull requests are read with one `gh api graphql` query**, no longer `gh pr list`: `gh pr list --json` gives neither
  the review threads' comment counts nor whether you hold a pending review. Moving back to `gh pr list` loses those two columns.
- **The Ticket column is the issue the pull request closes (`closingIssuesReferences`), not the branch's ticket.** The board's
  pull request glyph and `p` still match a ticket's branch to the pull request's head (ADR 0090). The two can disagree: a pull
  request whose body closes nothing shows a dash while its branch's ticket shows its glyph. The column says what merging closes.
- **The window's CI words are `failing`, `running`, `passing`**, not the glossary's *passed*, *failed*, *pending*, so a row's CI
  never reads `pending` beside its `pending` review badge.
- **A thread shows once**, as a card across the whole diff under its line, hung from it, the line marked by a glyph and an
  edge. Open threads are edged blue; your pending comment is a neutral card edged and badged amber, with Edit and Delete
  buttons beside their keys. The desktop review has no Comments pane, so `Focus.Comments`, its keys and its selection are gone;
  `enter` on a line replies to the thread under it (a pending comment on the line is edited first), and a press on a line
  number opens the comment box as `c` does.
- **A thread with no line still shows, where `review` places it.** `commentsInDiff` answers each thread and pending comment with
  a place: `onDiffLine`, `atFileHead` (a file the diff holds, on a line it does not show), or `outsideDiff` (a file the diff does
  not hold, such as one reverted out of the pull request, or a diff not read). An `atFileHead` card stands under its file's rule,
  before the first hunk; an `outsideDiff` card stands under a `Not in this diff` rule after the last file. The window joins
  nothing by path, and `r` hides a resolved thread through `review`'s `shown`, wherever it stands.
- **A lineless card's word comes from its position.** `outdated · was line 40` (`was lines 40–41`, or only `outdated` when GitHub
  keeps no original place) is said only of a thread GitHub calls outdated. A current thread on a line the diff does not show
  says `not in this diff · line 40`. A pending comment is never outdated: one on a line the diff no longer shows says
  `pending · head moved · line 40`, after the glossary's *Moved head*, and a reply to an outdated thread says
  `pending · on an outdated thread`.
- **A file's rule with cards under it is a stop of the cursor**, as is the `Not in this diff` rule. `j` and `k` move over it
  between lines, `J` and `K` land on it first, a press on it moves the cursor there, and `enter` and `x` act on its cards as on
  a line's: `review`'s `lastAtPlaces` picks the pending comment, else `enter` replies to the last thread there. `c` and `v`
  do nothing on a rule, which has no line.
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
- The TUI's Comments row for an outdated thread reads `was L12` where GitHub keeps the lines it was written on, and `outdated`
  where it keeps none; the window says `outdated · was line 12`. The panel is narrow, so the TUI keeps the one word.
- Replying by key to a thread whose line also holds a pending comment reaches the pending comment first; the thread's Reply
  button still works.
- The window no longer reads the protocol's `listed`; it is the TUI's Comments panel. `underLines` is renamed `shown` and
  `lastOnLines` `lastAtPlaces`, as both now hold every place, not only lines. The TUI still draws only `onDiffLine` entries
  under lines and lists the rest in Comments, so `NoDiffLine` splitting in two changes nothing it shows.
