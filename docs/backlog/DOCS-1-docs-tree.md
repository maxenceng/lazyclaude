---
id: DOCS-1
status: done
context: docs
type: feature
tracker_id: 77
created: 2026-10-10
---

# A Docs panel with a tree of `docs/**/*.md`, each rendered read-only in the main view and kept current

## Acceptance criteria

- [x] The Docs panel lists every `*.md` under the project's `docs/`, at any depth, except `docs/backlog/`: the files directly under `docs/` first, then one header per folder (`▾ adr (14)`, counting the documents beneath it), each level sorted by name and nested folders indented. Files other than `.md` are not listed.
- [x] Every folder starts unfolded. `enter` on a folder header folds it (`▸`) or unfolds it, and `j`/`k` move the selection while the Docs panel has focus.
- [x] On a document row, the main view shows that document rendered as markdown. It scrolls with the ticket detail's keys (`j`/`k`, `ctrl+d`/`ctrl+u`, `g`/`G`), stops at both ends, and goes back to the top when another document is selected. On a folder header, the main view shows nothing.
- [x] The main view follows the selection of whichever left panel last had focus. Moving from Docs back to Board shows the selected ticket again, and moving back to Docs shows the selected document again.
- [x] Whichever of Board and Docs has focus, or last had it, takes the left column's remaining height. The other shrinks to a few rows, and its selected row stays in view.
- [x] A document whose file is empty or whitespace shows a dim `empty document`, not a blank.
- [x] A document whose file cannot be read stays listed, and the main view shows its path and the reason, not a blank or a crash.
- [x] A project whose `docs/` holds nothing but `docs/backlog/` shows a dim `no docs` in the panel, and lazyclaude still starts.
- [x] Saving, adding or deleting a document, or adding a folder under `docs/`, shows up without a key press. The selection follows the document's path, never the row number, and the folds are kept. An open document is redrawn at its scroll position. Nothing flashes or says "refreshed".
- [x] If the selected document is deleted, the cursor lands on the row that took its place.
- [x] Changes under `docs/backlog/` never change the Docs panel.
- [x] If a watch cannot be set up (`max_user_watches` used up, or no inotify), the docs tree is polled every 5 s instead. One warning names the tree and the sysctl, and lazyclaude still starts.

## Model decision

A new `docs` context, the first ticket refined into it. `docs/context-map.md` already lists it (*expected*, from LAZY-12), where `docs` stands alone. So this creates the context the map already agreed on rather than choosing a new one. Its one aggregate is the docs tree.

- **`docs` domain**: `DocsTree`, built by a `DocsManager` over a `DocsPort`. It holds folders and documents in display order. A `Document` has a `DocumentPath`, relative to `docs/`, which is its identity across reads. It also has `Content | EmptyDocument`, or is an `UnreadableDocument` with its reason. Leaving out `docs/backlog/`, and sorting files before folders and by name, are the tree's rules. They are tested without a file system.
- **`docs` infrastructure.secondary**: `DocsRepository` walks `docs/**` with `Files` and reads every document's text. The tree rides one read, the same choice ADR 0014 made for the ticket detail. Twenty-odd documents cost only memory, and a document never disagrees with its row. `DocsWatchRepository` puts a `WatchService` on every directory under `docs/` except `docs/backlog/`, adds a watch for each new directory, batches events over ~100 ms, rescans on `OVERFLOW`, and falls back to 5 s polling with the warning. Each batch re-reads the tree. No document has frontmatter today (none of the 23 outside the backlog do), so the text is rendered as written: there is no frontmatter split and no frontmatter criterion.
- **`docs` infrastructure.primary**: `DocsQuery` answers a `DocsView` for a `DocsPayload` and lets the shell subscribe to new `DocsView`s. The payload carries the project root.
- **`shell`**: it takes the project root from the board read (`BoardView` gains it, through `board.published`) and hands it to `DocsQuery`. This is the same kind of shell join LAZY-21 uses for merged branches. It adds no edge between contexts, and it is how `session` and `review` will get the project too. That choice sets a pattern, so it gets an ADR in the implementing change. The shell also gains a `DocsRows` beside `BoardRows`, keyed by path, with folds. It remembers which left panel last had focus so the main view knows what to show, and gives the focused panel the height. The main view's markdown and scroll (`MarkdownView`, `MainViewScroll`) are reused for a document, without the ticket's head rows. Applying a new `DocsView` happens on the UI thread.
- **Who owns the watcher**: `docs` owns the `docs/**` watcher. This amends LAZY-21's "`board` owns the root-checkout watchers", which predates `docs` standing alone. `board` keeps `docs/backlog/` for BOARD-4. Nothing watches anything yet, and ADR 0014's claim that a refresh keeps the detail current is not true yet: BOARD-4 makes it true.
- **Architecture rule**: `side_effects_live_in_secondary_adapters` lists `ProcessBuilder`, `Process`, `Files` and `java.net.http`, but not `java.nio.file.WatchService`. It gains `WatchService`, and is watched to fail on a watcher placed outside `infrastructure.secondary`.

## Design direction

```
 ! 0  ? 0  • 0
┌ Board ───────────────┐┌ main view ─────────────────────────────┐
│ ▾ in-progress (1)    ││ Context map                            │
│   DOCS-1 A Docs pa…  ││ ══════════════════════════════════════ │
└──────────────────────┘│ Each bounded context is a direct …     │
┌ Sessions ────────────┐│                                        │
└──────────────────────┘│ Contexts                               │
┌ Docs ────────────────┐│ ────────────────────────────────────── │
│ context-map.md       ││ …                                      │
│ glossary.md          ││                                        │
│ ▾ adr (14)           ││                                        │
│   0001-record-arch…  │└────────────────────────────────────────┘
│   0002-enforce-arc…  │┌ dock ──────────────────────────────────┐
│ ▸ research (7)       │└────────────────────────────────────────┘
└──────────────────────┘
┌ Elsewhere ───────────┐
└──────────────────────┘
 j/k move  enter fold  h/l panel  alt+hjkl focus  q quit
```

- Rows: the file name only. Its folder is the header above it, and a long name is cut with `…` like a ticket title.
- Folder header: `▾`/`▸`, the folder name and the count of documents beneath it, styled like a Board status group.
- Main view on a document: its markdown and nothing else. The document's own H1 carries its title, as with a ticket.
- Keys in the Docs panel: `j`/`k` move, `enter` folds a folder. The key bar lists them while Docs has focus. The main view's keys are unchanged.
- Height: Board and Docs, whichever has focus or last had it, takes the remaining height, and the other keeps a few rows. Sessions and Elsewhere stay stubs.

## Glossary impact

- *Document* becomes true in code. Its *(expected)* marker comes off, and its meaning stays: one markdown file under `docs/`, outside `docs/backlog/`.
- Adds *Docs tree*: the project's documents grouped by folder, as the Docs panel shows them. `docs` owns it.
- *Main view* changes from "whatever the selection points at" to "the selection of whichever left panel last had focus", and adds "for a document, its markdown rendered".
- No word is displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-4](LAZY-4-tamboui-capabilities.md), [LAZY-10](LAZY-10-screen-layout-and-keys.md), [LAZY-21](LAZY-21-keeping-the-screen-current.md). Search across the docs is out of scope.

*Model decision* above says sorting files before folders and by name is the tree's rule, tested without a file system. Review reversed that: `DocsPort.paths` hands paths over in tree order and the domain keeps it, as `BacklogRepository` does for the board — see [ADR 0018](../adr/0018-the-docs-adapter-orders-the-tree.md). Leaving out `docs/backlog/` stayed in the domain, on `DocumentPath`.

Two shapes came out of review rather than refinement: a `docs/` that cannot be listed shows a dim "docs unavailable" with its reason instead of failing start-up ([ADR 0016](../adr/0016-docs-owns-the-docs-watcher.md)), and folders carry a published `FolderPath` so the shell keys its folds by path. The strict UTF-8 reading `board` and `docs` share moved to `shared.infrastructure.secondary` ([ADR 0017](../adr/0017-the-shared-kernel-has-an-adapter-package.md)).
