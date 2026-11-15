# 116. The desktop frontend draws a mermaid block with mermaid.js, loaded on demand

Date: 2026-11-15

## Status

Accepted. Amends ADR 0087: the renderer it describes (the design system's `Markdown`) now draws a `mermaid` fenced block
as a diagram; every other part of ADR 0087 stands. Builds on ADR 0059 (the self-contained bundle).

## Context

`docs/context-map.md` draws how the contexts connect as a mermaid block, and GitHub draws such a block in any document or
issue. The window's `Markdown` set it as a code block, so the picture was source. lazyclaude opens other projects' docs too, so
any diagram kind GitHub draws may turn up (#179). The window's CSP is off, and a document's markdown is not trusted input.

## Decision

- **The parser keeps a fence's language.** `Markdown` draws a block whose language is `mermaid` through mermaid.js and any
  other block, with another language or none, as the code block it always was. One rule for every place `Markdown` renders:
  a document in the main view and a ticket's body alike, as GitHub does. A review comment is set by `InlineText`, not
  `Markdown`, so a mermaid block in a comment still shows its source, under `InlineText`'s deferred note on fenced blocks.
- **The renderer stays our own** (ADR 0087 kept `react-markdown` and `marked` out as a tree of packages): mermaid.js draws only
  the inside of a `mermaid` block, which no small renderer of ours could.
- **mermaid.js, not a lighter flowchart-only renderer,** so every diagram kind GitHub draws is drawn. **Not pre-rendered in
  the sidecar:** mermaid-cli needs Node and Chromium at runtime, against the self-contained bundle of ADR 0059.
- **It is imported on demand,** from one loader only `Diagram` calls, so Vite splits it into its own chunk. A
  document without a mermaid block never loads it, and the window's main chunk does not hold it.
- **It runs at `securityLevel: 'strict'`:** a `click` directive, or an HTML label carrying a script or a handler, runs
  nothing.
- **Until the drawing is ready the block shows its source as the code block, which the drawing then replaces.** Drawing waits
  on the reading faces (regular, semibold, italic) being loaded, so a label's width is measured in the face it is drawn in.
- **A block that is not drawn keeps its source as a code block under one line,** whether its source does not parse or
  mermaid.js or a reading face could not be loaded: "Diagram not drawn", through i18n, then
  Mermaid's message as Mermaid gives it, its first two lines (PRODUCT.md: absence and failure are shown with their reason).
  The rest of the document is unaffected.
- **The look is the code block's sunk panel,** mermaid's `base` theme fed from the design tokens read at runtime. A diagram
  wider than the reading area shrinks to fit, never below 85% of its natural size, then scrolls sideways; a narrower one is
  not enlarged. The natural size is the width the root `<svg>` names (its view box, else its `width` or `max-width`), and the
  panel is sized from it, since mermaid gives most diagram kinds no intrinsic width.
- **Mermaid's look is pinned:** `look: 'classic'` and `layout: 'elk'`, with no minimum node width. ELK draws a graph's
  edges at right angles, as the project's Zensical docs site draws them; the approved prototype, on mermaid 11, had dagre's curves,
  and the window looked at side by side with the docs site was judged better straight. `classic` keeps mermaid 12's `neo`
  look (drop shadows) out of the sunk panel, and no minimum keeps a short label's node from growing to 120 pixels.
- **The TUI is unchanged:** its main view still shows a mermaid block's source.

## Consequences

- The production bundle carries a large lazy chunk set (mermaid and one chunk per diagram kind). The window fetches
  them from its own assets, so nothing waits on a network, but the deb and AppImage grow.
- A diagram's colours are read once, when it is drawn; a runtime theme change would not reach a drawn diagram. The window has
  no theme switch today.
- The italic reading face is now bundled, so emphasis everywhere in the window is set in it rather than slanted.
- A document's `%%{init}%%` directive may still change a diagram's look or layout; it cannot loosen `securityLevel`, which
  mermaid keeps out of a directive's reach.
- jsdom cannot lay out SVG: tests mock the loader, and the drawing itself is checked in the window.
