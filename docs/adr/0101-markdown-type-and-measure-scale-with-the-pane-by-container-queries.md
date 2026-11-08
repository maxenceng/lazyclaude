# 101. Markdown type and measure scale with the pane, by container queries

Date: 2026-11-08

## Status

Accepted.

## Context

On a full-screen main view the desktop's Markdown stayed at 15px in an 80ch column pinned to the left, so a wide pane piled
its empty space on the right, the text read small from a normal distance, and a wide table scrolled sideways beside empty room.
The user's monitor is a 3440px ultrawide: a half tile is a ~1200px main pane, full screen a ~3000px one.

## Decision

- **The pane is the container.** `Detail` and `Document` wrap their content in an element with `container-type: inline-size`;
  the reading size is `clamp(15px, 15px + (100cqi - 720px) * rate, 18px)` in `tokens.css`, reaching 18px at a 3000px pane.
  Container queries rather than a `ResizeObserver`: no render pass, no state, and the browser keeps it smooth.
- **The column starts at the pane's edge and its measure grows with the pane**: `clamp(110ch, 70cqi, 200ch)`, so prose fills a
  half tile and reaches about 70% of a full-screen pane. `--reading-columns` is `measure | 1fr`; `Detail`'s facts and title
  use the same tracks as the Markdown body, so they share its left edge.
- **Code and tables break out to the right**: they start at the column's left edge, span to the pane's edge, and are
  `fit-content`, so a narrow one is not stretched and a wide one scrolls inside the pane. Code in a table cell does not wrap.
- **Headings, code and table heads are `em` ratios of the reading size** (`--reading-title-em` and its siblings), the old 28, 14 and 13px
  against 15.
- Rejected, after seeing them on screen: a centred 80ch column, which on a full-screen pane left a wide empty gutter on the left
  and pushed the breakouts to the right; a 22px ceiling, too large beside the 13–15px panels; a fixed 110ch measure, which left
  prose on a third of a full-screen pane. Also rejected: a viewport-width scale (`vw`), which ignores the sidebars and a half
  tile; one wrapper at `max-width`, which cannot let a table out.

## Consequences

- The 720px and 3000px anchors, the 70% share and the 200ch cap are tuned by eye on one ultrawide, and change in `tokens.css` alone.
- Full-screen lines run to about 200 characters: chosen to fill the pane, at the cost of a longer return sweep than book typography allows.
- jsdom lays nothing out: tests pin the tokens and the rules, and the look is confirmed on screen by the user.
