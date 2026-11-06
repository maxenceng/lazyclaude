# 94. The desktop review goes to a file on one click, and writes a comment on a second

Date: 2026-11-06

## Status

Accepted. Amends ADR 0051 for the desktop window's Review screen; the TUI keeps ADR 0051 as it is.

## Context

ADR 0051 gives the mouse focus and selection only, with a second click on the selected row acting as `enter`. In the
desktop window's review that left two things out of reach of the mouse. Going to a file took two clicks, though selecting
a file without going to it is of no use: Files has nothing else to do with a selection. And a comment could only be
started with `c`, because `enter` on a diff line edits the pending comment there and does nothing on a line without one.

## Decision

- **One click on a file in Files goes to it**: the file is selected and the diff cursor moves to its first line, whether
  or not Files had focus. Only Files changes. A second click is still `enter` in Comments and on the window's other lists.
- **A second click on a diff line writes there.** A click on the line the cursor is already on, in the focused diff,
  edits that line's pending comment, as `enter` does. On a line with no pending comment it opens the new-comment box, as
  `c` does, on the range if one is marked. It calls the operations those keys call, as ADR 0051 requires.
- **A second click acts on its release, not its press.** A press on the cursor's line may also start a drag. A release on
  another line marks the range and writes nothing. A release on the line pressed does the second click's action. A press
  off the diff, or the window losing focus, forgets the press.

## Consequences

- The desktop window and the TUI now differ on a click in a review's Files and on a second click on a diff line. The TUI
  still needs two clicks for a file and `c` for a new comment.
- A comment box opened by a click is still modal (ADR 0089), so a stray second click cannot write past it.
