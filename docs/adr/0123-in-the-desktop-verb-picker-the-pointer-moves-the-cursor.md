# 123. In the desktop verb picker the pointer moves the cursor, and the cursor ground is its one highlight

Date: 2026-11-20

## Status

Accepted. Reverses, for the verb picker only, the hover ground every short session row took under the pointer; session rows
and the board keep it.

## Context

A short session row took the hover ground under the pointer, so every verb in the desktop's verb picker read as pickable. With
two or more verbs, the pointer on one verb lit it with the hover ground while the cursor verb kept the cursor ground: two rows
lit, in two colours, and enter started the one the pointer was not on.

## Decision

- **A pointer entering a verb moves the picker's cursor there**, through `VerbPicker.at`, as in a native menu. Enter, j and k
  carry on from that verb; a click still starts the clicked one.
- **A verb row draws no hover ground.** A `SessionRow` given `onPoint` carries the `followsPointer` class, which the hover-ground
  rule leaves out; the one predicate, a handler given, decides both. The cursor ground is the picker's only highlight.
- **Entering, not resting.** A pointer left on a verb while the keyboard moves the cursor away does not take it back, and the
  pointer leaving the picker leaves the cursor where it is.
- Session rows outside the picker, and the board, keep their hover ground: a click there selects, it does not follow a cursor.
- Rejected: keeping the hover ground and dimming the cursor ground under it, which still lights two rows.

## Consequences

- The TUI has no pointer; nothing changes there.
- deferred: the pointer is read on `pointerenter`, so a browser's synthetic enter after a scroll or a layout change, under a still
  pointer, would move the cursor too; not seen in jsdom — close it, by reading only a real pointer move, if the picker is ever seen to
  lose its preselected verb when it opens under the pointer.
