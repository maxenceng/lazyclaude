---
id: SESSION-14
status: in-review
context: session
type: feature
tracker_id: 103
created: 2026-10-27
---

# A hosted pane takes the mouse only once focused, keeps a drag's release, and gets hover

## Acceptance criteria

- [ ] A click on a hosted pane that does not have focus focuses it and is not forwarded: a click that focuses a pane
      showing a permission prompt does not answer it.
- [ ] Once the pane has focus, press, drag and release reach `claude` in order, as today.
- [ ] The wheel over a hosted pane is forwarded even on the notch that focuses it.
- [ ] A drag that starts in the pane and is released outside it sends its release at the pane's nearest edge cell, so
      `claude`'s selection ends.
- [ ] Pointer moves reach `claude` while its pane has focus and it has asked for mode 1003, so its rows highlight on
      hover; moves anywhere else are dropped and cause no render.
- [ ] With `mouse: false`, nothing is forwarded and `claude` sees no mouse.

## Model decision

The TUI's hosted-pane primary adapter and the shell (ADR 0051, amending ADR 0026). Built on SESSION-13, where
`MouseBytes` moves to the TUI adapter and encodes by the emulator's modes; the drag's target pane and the clamp are the
shell's, from BOARD-7's hit map.

## Design direction

n/a: no layout change.

## Glossary impact

none

## Notes

LAZY-27 found both gaps in the code already built: `mouseMotion` is never turned on, and the release of a drag that
leaves the pane is lost. Turning motion on makes the outer terminal report every move across the screen, hence
dropping it before a render.
