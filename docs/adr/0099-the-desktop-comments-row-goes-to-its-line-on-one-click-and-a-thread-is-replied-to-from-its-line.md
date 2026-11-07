# 99. The desktop Comments row goes to its line on one click, and a thread is replied to from under its line

Date: 2026-11-07

## Status

Accepted. Amends ADR 0094 for the desktop window's Review screen; the TUI keeps ADR 0051 as it is.

## Context

ADR 0094 made one click on a file in Files go to it, because selecting a file without going to it is of no use. Comments
is the same: its only job is to say which thread to read next and take you to it, yet it still needed a second click, and
a thread could only be answered with `c` from Comments. The row also said little: where a thread hangs and who opened it,
not how many replies it holds, who spoke last or what it is about.

## Decision

- **One click on a row in Comments selects it and goes to its line**: the diff cursor moves to the first line of the
  thread or pending comment and is scrolled into view, whether or not Comments had focus. A second click on the selected
  row, in the focused Comments, is still `enter`: on a pending comment it opens the box to edit it.
- **An outdated thread has no line, so one click selects it and leaves the diff where it was**, with no error. This is
  a deliberate limit, marked `deferred:` in the code; it closes when the sidecar places an outdated thread on the nearest
  line.
- **A desktop Comments row is two lines that never wrap**: the mark and `file:lines` with `n replies · last author` on the
  right (only the author when there is no reply, never `0 replies`), then, dimmer, the first line of the first comment,
  clipped with an ellipsis. A pending comment's row has the same shape with nothing on the right. A resolved thread is
  dimmed on both lines.
- **Every thread annotation under its diff line ends with a Reply button**, in dim ink and lit on hover; a pending
  comment's has none. A click opens the pending-reply popup `c` opens, with the thread above the text, on a resolved thread
  too, and moves neither the diff cursor nor the focus. It calls the operation `c` calls, as ADR 0051 requires, and the
  reply stays a comment of the pending review (ADR 0009, ADR 0041).

## Consequences

- The desktop window and the TUI now differ on a click in Comments: the TUI still selects on one press and goes on a
  second, and its row stays one line.
- A click on a pending comment's row can no longer select it without moving the diff cursor.
- Nothing changes in the protocol, the sidecar or the GitHub writes.
