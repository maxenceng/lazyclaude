# 118. A session pane keeps a ctrl+click it acts on from the program, and leaves a link it would refuse to the program

Date: 2026-11-20

## Status

Accepted. Amends ADR 0115: a ctrl+click on a web link is the pane's alone, and a link of another scheme is the program's
while it reports the mouse. The rest of ADR 0115 stands.

## Context

While Claude Code draws on the alternate screen it reports the mouse, so xterm.js sends it every press and release in the
pane, a ctrl+click on a link included. Claude Code opens a link clicked with ctrl or alt held by itself, through `xdg-open`,
and xterm.js also runs the pane's own link, which opens it through lazyclaude's opener (ADR 0115). Two programs opened the
same address (#241). Deduplicating cannot work, because the second open happens in `claude`'s process.

## Decision

- **lazyclaude stays the one opener of a web link.** A ctrl+press on an http(s) link, a plain address or an OSC 8 one,
  stops at xterm's screen element. xterm's link detector listens there, so it still sees the press and opens the link on
  release. The terminal element above it reports presses to the program, and it never sees this one. Without the press, xterm
  never starts reporting the release either, wherever the button comes up. The pane keeps the focus a click gives it.
- **A link the pane would refuse is the program's while it reports the mouse.** A ctrl+click on an OSC 8 link of another
  scheme is reported to the program as before, and the pane raises no "refused" notice and clears none. While the program
  does not report the mouse, the pane refuses it with a notice, as ADR 0115 says.
- **One predicate decides.** The press is kept exactly when ctrl is held and the pointer is on a link the key bar names.
  These are the links ctrl+click opens, so the bar never offers a link whose click then reaches the program.
- Rejected: yielding a web link to `claude` while it reports the mouse. That gives two openers depending on the mode, and
  loses ADR 0115's notices for a failed open.

## Consequences

- The pane reads xterm's `.xterm-screen` element by its class, as its underline layer already does (ADR 0115). An xterm.js
  upgrade that moves its link detector or its mouse reporting to another element would bring the double open back. The
  emulator's tests click a real xterm.js and check both the open and the reports, so such an upgrade fails them.
- A press xterm resolves before the pointer's link is known reaches the program, which opens the link once, by itself.
- An alt+click on a link is still the program's: lazyclaude acts on ctrl alone.
