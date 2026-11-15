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
  a document in the Docs panel and a ticket's body alike, as GitHub does.
- **mermaid.js, not a lighter flowchart-only renderer,** so every diagram kind GitHub draws is drawn. **Not pre-rendered in
  the sidecar:** mermaid-cli needs Node and Chromium at runtime, against the self-contained bundle of ADR 0059.
- **It is imported on demand,** from the one component that draws a diagram, so Vite splits it into its own chunk. A
  document without a mermaid block never loads it, and the window's main chunk does not hold it.
- **It runs at `securityLevel: 'strict'`:** a `click` directive, or an HTML label carrying a script or a handler, runs
  nothing.
- **Until the drawing is ready the block shows its source as the code block, which the drawing then replaces.** Drawing waits
  on the reading faces (regular, semibold, italic) being loaded, so a label's width is measured in the face it is drawn in.
- **A block that does not parse keeps its source as a code block under one line:** "Diagram not drawn", through i18n, then
  Mermaid's message as Mermaid gives it, its first two lines (PRODUCT.md: absence and failure are shown with their reason).
  The rest of the document is unaffected.
- **The look is the code block's sunk panel,** mermaid's `base` theme fed from the design tokens read at runtime. A diagram
  wider than the reading area shrinks to fit, never below 85% of its natural size, then scrolls sideways; a narrower one is
  not enlarged.
- **The TUI is unchanged:** its Docs view still shows a mermaid block's source.

## Consequences

- The production bundle carries a large lazy chunk set (mermaid and one chunk per diagram kind). The window fetches
  them from its own assets, so nothing waits on a network, but the deb and AppImage grow.
- The diagram's colours are read when it is drawn, so a diagram already drawn keeps its theme until its document is read again.
- jsdom cannot lay out SVG: tests mock the loader, and the drawing itself is checked in the window.
