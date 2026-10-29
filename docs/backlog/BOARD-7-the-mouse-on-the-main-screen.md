---
id: BOARD-7
status: in-review
context: board
type: feature
tracker_id: 101
created: 2026-10-27
---

# The mouse focuses and selects on the main screen, and a second click is `enter`

## Acceptance criteria

- [x] A left click on a row of the Board, the status columns, Sessions or Docs focuses that panel and selects the row;
      the main view follows the selection as it does for `j`/`k`.
- [x] A second click (a press on the row already selected, in the panel that already had focus) does what `enter` does
      there: a Board header folds, the status columns pick, Sessions focuses a tab, resumes or opens the verb picker,
      a Docs folder folds.
- [x] A first click on the selected row of an unfocused panel only focuses it: clicking the Sessions panel whose cursor
      sits on a past session does not resume it.
- [x] A click in the main view focuses it; a click on a dock tab's label selects that tab and focuses the dock, and a
      click on a tab in the dock strip does the same and ends the zoom.
- [x] The wheel acts on the pane under the pointer and focuses it: one row a notch in a list, three lines in the main
      view.
- [x] In the verb picker or a prompt, a click selects a row and a second click is `enter`; a click outside it does
      nothing and reaches nothing under it.
- [x] Right and middle clicks, the key bar, the status line, the narrow status column and a link in the ticket detail
      do nothing.
- [x] Every key does what it did before.
- [x] `config.yml`'s `mouse: false` turns mouse capture off, and it is off by default when `TERM` is `linux`; a
      `mouse` value that is not a boolean is refused with its file and line like the other settings.

## Model decision

The shell, plus one setting in `session`'s `Configuration` (ADR 0051). No domain change in `board`: the prefix is the
screen's, as with the status columns.

- A per-frame hit map, generalising `PaneProbe`: every pane records its rectangle, first visible row and tab offsets as
  it renders; the shell's global mouse handler resolves an event against the last frame's map on the render thread.
- The second click is the shell's: the focus and selection before the press decide it. It calls the same path `enter`
  does, so there is no second copy of any action.
- `mouse` joins `Configuration` beside `desktopNotifications`, read through `ConfigurationQuery`; the `TERM` default is
  the configuration repository's, since it reads the environment.

## Design direction

No layout change: focus shows as today and nothing highlights on hover. Keys unchanged.

## Glossary impact

*Second click* and *Mouse setting* (added with ADR 0051).

## Notes

Mouse events reach the shell's global handler only; TamboUI routes a left press to the outermost element and never
selects a row (`docs/research/tamboui-mouse-support.md`). Built after SESSION-10, so there is no Elsewhere panel to
give the mouse.
