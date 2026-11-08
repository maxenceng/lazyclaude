# 101. Markdown type scales with the pane and its column is centred, by container queries

Date: 2026-11-08

## Status

Accepted.

## Context

On a full-screen main view the desktop's Markdown stayed at 15px in an 80ch column pinned to the left, so a wide pane piled
its empty space on the right, the text read small from a normal distance, and a wide table scrolled sideways beside empty room.

## Decision

- **The pane is the container.** `Detail` and `Document` wrap their content in an element with `container-type: inline-size`;
  the reading size is `clamp(15px, 15px + (100cqi - 720px) * rate, 18px)` in `tokens.css`, reaching 18px at 1800px.
  Container queries rather than a `ResizeObserver`: no render pass, no state, and the browser keeps it smooth.
- **The column is grid tracks**, `1fr | 80ch | 1fr`, in `--reading-columns`. A pane narrower than 80ch has no offset; a wider one
  splits the remainder evenly. The `ch` is the scaled type's, so the measure stays 80 characters. `Detail`'s facts and
  title use the same tracks as the Markdown body, so they share its left edge.
- **Code and tables break out to the right**: they start at the column's left edge, span to the pane's edge, and are
  `fit-content`, so a narrow one is not stretched and a wide one scrolls inside the pane.
- **Headings, code and table heads are `em` ratios of the reading size** (`--reading-title-em` and its siblings), the old 28, 14 and 13px
  against 15.
- Rejected: a viewport-width scale (`vw`), which ignores the sidebars and a half tile; one wrapper at `max-width: 80ch`, which
  cannot let a table out.

## Consequences

- The 720px and 1800px anchors are measured guesses at a half tile and a full screen, and change in `tokens.css` alone.
- jsdom lays nothing out: tests pin the tokens and the rules, and the look is confirmed on screen by the user.
