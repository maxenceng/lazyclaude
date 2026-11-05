# 87. The desktop window follows a document's links itself and opens the rest through Tauri's opener

Date: 2026-11-05

## Status

Accepted. Builds on ADR 0051 (the mouse; a second click is `enter`), ADR 0057 (the dumb relay) and ADR 0086 (a Tauri plugin
for a platform capability the webview lacks).

## Context

The TUI shows a document's markdown and follows none of its links: it has no pointer to follow them with, and no way to open
a browser from a terminal pane. The desktop frontend has both, and a webview that follows an `<a href>` navigates away from
the application. A document's links are of four kinds: to another document of the tree, to a file outside it, to a web
address, and to a heading by its fragment.

`docs/watch` and `docs/changed` already carry every document's text, so a link to a document of the tree needs no new
method. They carry no reason for a path they do not list, and the sidecar has no read of one document.

## Decision

- **No link navigates the webview.** Every `<a>` the markdown primitive renders cancels the click and hands its `href` to the
  page; so does the middle button. Raw HTML in a document or a ticket is text, never markup.
- **A relative link is resolved against the document that holds it, then against `docs/`.** A path that lands under
  `docs/` names a document by its path under `docs/`, opens in the main view and selects its row in the tree, unfolding
  the folders above it. Anything else is outside the tree. A ticket's body has no document, so its relative links start
  at the project root; `docs/guide/intro.md` there is the same document.
- **The tree's selection is the only state.** There is no history and no back: following a link moves the selection, as
  `j` would. The selection is a row by kind and path, so it survives the tree being read again; it may name a path the
  tree does not list.
- **A fragment names a heading by its slug**, as GitHub writes it: lowercased, punctuation dropped, spaces to hyphens, a
  repeated slug numbered `-1`, `-2`. The TUI derives no slug, so this one is ours. A fragment with no matching heading
  opens the document at its top and puts a warning on the status line.
- **A link to a markdown path under `docs/` the tree does not list shows as an unreadable document**: its path, and the
  reason `No document at this path in the docs tree.` The reason is the window's own words, since the sidecar says nothing.
  `docs/backlog/` is not listed, so a link there reads the same.
- **Outside the tree, `tauri-plugin-opener` opens it.** A file or folder goes to `openPath`, which on Linux is `xdg-open`;
  an `http` or `https` address goes to `openUrl`, the system browser. No `mailto:`, `javascript:` or other scheme is opened:
  the window warns that it did not. A failure of the opener is a warning with its reason, never silence. The capability
  grants `opener:allow-open-url` for `http://**` and `https://**` and `opener:allow-open-path` for `/**`; a document is the
  developer's own and a path inside the project is the common case, but a narrower scope would need the project root at
  build time.
- **The markdown is a small renderer of our own** in `design-system/markdown.ts`: headings, paragraphs, fenced code, lists,
  quotes, rules, pipe tables, and inline code, links, strong and emphasis. Rejected: `react-markdown` or `marked`, each a
  tree of packages for the few blocks these documents use, and each needing a sanitiser or plugins to keep raw HTML out and
  to give headings stable ids. One primitive sets a document and a ticket's body.

## Consequences

- Nested lists, reference-style links, footnotes and raw HTML are shown as the text they are written as, until a document
  needs them.
- A link to a document the tree lists but the sidecar could not read shows that document's own reason.
- The opener is behind `LinkOpener`, as the clipboard is behind `Clipboard`; tests record what it was asked to open.
- Checked by the tests in `docs/` and the window's, not in the packaged window: the packaged `xdg-open` and browser hand-offs
  are untried.
