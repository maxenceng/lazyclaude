# 113. A session pane follows its own links with ctrl held, and names the hovered one in the key bar

Date: 2026-11-14

## Status

Accepted. Amends ADR 0087: `docs` owns every link the window follows except a session pane's, which the session folder owns.
ADR 0087's opener, and its http(s)-only rule for a web address, stand.

## Context

A hosted session's pane ran xterm.js with the fit addon alone, so a web address Claude printed was plain text and an OSC 8
hyperlink had no handler: neither opened anything (#213). Every other surface follows its links through `docs` (ADR 0087).
A pane's links differ from a document's: they are cells of a terminal, not `<a>` elements, a plain click there selects and
focuses the pane, and the only kind worth following is a web address. Routing them through `docs` would make the session
folder import another context's, which the frontend forbids (ADR 0057).

## Decision

- **The session folder follows a pane's links itself.** A link provider finds the plain `http` and `https` addresses in the
  pane's output, across soft-wrapped rows and in the cells wide characters fill; xterm's `linkHandler` takes OSC 8 links,
  of any scheme, so a refused one can say so. Both hand the address to the same opener `docs` uses, passed in at the
  composition root. The http(s) check is the session folder's own copy of ADR 0087's rule.
- **A link opens only with ctrl held.** A plain click on it does what a click on the pane always did. A followed link clears
  the notice, as ADR 0087's does; a refused scheme and a failed opener each raise a notice with the address.
- **The hovered link is named first in the key bar, `ctrl+click open <url>`, and only while the pointer is on it.** An OSC 8
  link's visible text hides its target, so the target is what is named, in full. A link ctrl+click would refuse is not named.
  Rejected: a tooltip above the pointer, which covers the line above the link, and a strip at the pane's foot, which covers
  Claude's prompt.
- **The key bar takes one `lead` entry, the only one that gives way.** It goes first and is clipped with an ellipsis when the
  bar runs out of room; every other entry stays whole. A second entry that must lead needs this decision revisited.

## Consequences

- The http(s) rule lives in two folders, `docs` and `session`. A scheme opened by one must be added to both.
- A link hovered in a tab that is then hidden by a key may stay named in the key bar until the pointer next moves over the
  pane.
- The TUI follows no link: in a terminal frontend, the host terminal's own link handling applies.
