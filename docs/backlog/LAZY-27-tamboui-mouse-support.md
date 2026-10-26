---
id: LAZY-27
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# What mouse support do TamboUI and the hosted pty offer?

## Question

What mouse support do TamboUI and the hosted pty offer?

- TamboUI 0.5.0 (and any later release): mouse event APIs, how events reach elements and containers, clicks, double-clicks, wheel, coordinates, modifiers, drag; how to turn reporting on (off by default);
- hit-testing: can a click be mapped to the element drawn at that cell, or must lazyclaude keep its own layout rectangles;
- forwarding mouse input to a hosted `claude`: whether `claude` turns mouse reporting on, how jediterm-core exposes the mode, how SGR mouse sequences are re-encoded for the pty, and how a click is told apart from a selection;
- terminal-emulator limits (kitty, gnome-terminal, Windows Terminal, iTerm2) — selection with Shift, SGR 1006, focus reporting interplay (ADR 0034).

## Kind

research

## Resolution

TamboUI 0.5.0 (0.6.0 changes nothing here) reports SGR mouse events once `mouseCapture(true)`
is on, and lazyclaude already turns it on. It reports press, release, drag, move and wheel, with
modifiers and 0-based cells, but no double-click. Routing to elements is weak. Only a left
press, move and wheel up/down reach elements, outermost first, and hit-testing stops at an
element's rectangle, so a click focuses a panel but never selects a list row. lazyclaude must
keep its own geometry and hit-test in the shell's global handler. `claude` 2.1.310 turns on
modes 1000/1002/1003 with SGR and handles its own clicks and selection, so a hosted pane only
forwards press, drag and release in order. Selecting across the whole screen stays with the
outer terminal (Shift, or Option in iTerm2). Two gaps in the code already built: `mouseMotion`
is never on, so `claude` gets no hover; and a drag that leaves the pane loses its release
(clamp it to the pane's edge). Risks: terminals that answer with X10 encoding turn a click into
typed keys (a `q` can quit), and Windows mouse support is likely missing in 0.5.0. Details:
[`docs/research/tamboui-mouse-support.md`](../research/tamboui-mouse-support.md).
