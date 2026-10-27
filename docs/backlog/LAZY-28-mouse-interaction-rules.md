---
id: LAZY-28
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-25, LAZY-27]
---

# What does a mouse do in each part of the screen?

## Question

What does a mouse do in each part of the screen?

The second roadmap's table, settled against what LAZY-27 finds: panels (click focuses and selects), the board and its zoomed columns, the ticket detail's actions and links, the dock's tabs, region focus, wheel scrolling, the docs tree, the review screen's diff and comments, and clicks inside a hosted pane (forwarded, never acted on by lazyclaude). Whether double-click opens anything, or single click plus enter stays the only path; how focus stays visible; the ADR next to ADR 0008.

Also settled here, from LAZY-27: where the shell keeps the geometry it hit-tests against; whether the two gaps in hosted panes (no hover, a drag's release lost outside the pane) are fixed in the mouse work; and what lazyclaude does on a terminal that answers in X10 encoding, where a click arrives as typed keys.

## Kind

grilling

## Resolution

Settled on 2026-10-27 by grilling; recorded in ADR 0051.

- **The mouse focuses, selects, switches dock tabs, scrolls and selects a diff range; every other action stays on its
  key.** Right and middle clicks do nothing. The key bar, the status line and links are inert; Shift keeps the outer
  terminal's own selection and link-click.
- **A second click is `enter`**: a press on the row already selected, in the pane that already had focus. Board header
  folds, status columns pick, Sessions focuses a tab, resumes or opens the verb picker, a Docs folder folds, Files goes
  to its line, Comments goes to its line or opens the pending comment, a diff line edits its pending comment. A dock
  tab's second click does nothing (`enter` there is `claude`'s). No timed double-click.
- **The wheel acts on the pane under the pointer and focuses it**: a row a notch in a list (the selected row stays
  visible), three lines in the main view and the diff, forwarded over a hosted pane.
- **Hosted panes**: a click on an unfocused pane only focuses it, so it never answers a permission prompt; once focused,
  press, drag, release and wheel pass through, the wheel even on the focusing notch. Both LAZY-27 gaps are fixed in the
  mouse work: a drag keeps the pane it started in and its release is clamped to the edge, and motion reaches the pane
  only while it is focused and its session asked for 1003, dropped everywhere else before a render.
- **Popups are modal**: inside, a click selects and a second click is `enter`; outside, nothing happens.
- **A drag across diff lines selects a range** like `v`, clamped to the diff.
- **Focus shows as today**: no hover highlight in lazyclaude's own panels.
- **Geometry is a per-frame hit map** each element writes as it renders (`PaneProbe` generalised), read by the shell's
  global mouse handler on the render thread.
- **X10**: `mouse: false` in `config.yml` turns capture off, off by default under `TERM=linux`; no decoder of our own,
  the fix is TamboUI #517.

Graduated: [BOARD-7](BOARD-7-the-mouse-on-the-main-screen.md),
[REVIEW-4](REVIEW-4-the-mouse-on-the-review-screen.md),
[SESSION-14](SESSION-14-a-hosted-pane-takes-the-mouse-once-focused.md).
