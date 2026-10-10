---
id: BOARD-2
status: in-review
context: board
type: feature
tracker_id: 73
created: 2026-10-10
---

# The ticket detail in the main view: frontmatter, plus the body rendered as markdown

## Acceptance criteria

- [x] On a ticket row, the main view shows a facts line (`BOARD-2 · draft · board · feature`, missing fields omitted as today), then every other frontmatter key in file order as `key  value`, a rule, then the body rendered as markdown.
- [x] A scalar value shows as written (`created     2026-10-10`); a list shows as `[LAZY-1, LAZY-4]`.
- [x] The YAML block never renders as markdown, and a `---` inside the body is a thematic break, not a second frontmatter.
- [x] A ticket whose body is empty or whitespace shows a dim `no body` under the rule, not a blank.
- [x] With focus on the main view, `j`/`k` scroll one row, `ctrl+d`/`ctrl+u` half a page, `g`/`G` to the top and bottom.
- [x] Scrolling stops at both ends: `k` at the top and `j` at the bottom stay put, and a ticket shorter than the view does not scroll at all.
- [x] Selecting another ticket resets the main view to the top.
- [x] While the main view has focus, the key bar lists its scroll keys; `j`/`k` there never move the Board selection.
- [x] An unreadable row still shows its file and reason, and a group header still shows nothing.

## Model decision

New behaviour on the existing `Ticket`, carried by the board read. There is no new query and no new aggregate.

- **`board` domain**: `Ticket` gains `Frontmatter`, an ordered list of key → value text holding every key the file has. It also gains `Body | NoBody`, everything after the closing `---`, where an empty or whitespace body is `NoBody`.
- **`board` infrastructure.secondary**: `BacklogRepository` already reads each whole file and now keeps the body instead of dropping it. `TicketFrontmatterMapper`, still the only place that touches SnakeYAML's untyped map, turns each value into its display text: a scalar as written, a list as `[A, B]`.
- **`board` infrastructure.primary**: `TicketView` carries the frontmatter and the body.
- **`shell`**: renders the body with `tamboui-toolkit-markdown`, a new dependency, using `MarkdownElement.markdown(src).scroll(n)`. The shell owns the scroll offset, clamped with `MarkdownView.computeHeight(width)`, and the main view's keys.
- **Why the board read and not a per-ticket query on selection**: the board already opens every file, so carrying the rest costs only memory, which is trivial at backlog size. One snapshot means the detail never disagrees with its board row. The refresh that keeps the board current covers the detail too. A per-ticket read would add a "file gone since the board was read" case. This choice gets an ADR in the implementing change.

## Design direction

```
┌ main view ────────────────────────────┐
│ BOARD-2 · draft · board · feature     │  ← facts line, as today
│ tracker_id  73                        │  ← every other key, file order
│ created     2026-10-10                │
│ ───────────────────────────────────── │
│ The ticket detail in the main view…   │  ← body rendered (its H1 is the title)
│ ═════════════════════════════════════ │
│ Acceptance criteria                   │
└───────────────────────────────────────┘
 j/k scroll  ^d/^u half page  g/G top/bottom  h/l panel  alt+hjkl focus  q quit
```

- The title is no longer its own line: the body's H1 carries it. A ticket with no H1 shows its facts line and body only.
- Keys, with focus on the main view: `j`/`k` row, `ctrl+d`/`ctrl+u` half page, `g`/`G` top/bottom. No wrap, reset to the top on a new selection.
- Unreadable rows and headers are unchanged from the board's first slice.

## Glossary impact

- Adds *Main view*: the right column's top pane, which shows whatever the selection points at (here a ticket's frontmatter and rendered body).
- *Ticket*'s existing entry ("frontmatter plus body") becomes true in code; no word displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in Markdown via `tamboui-toolkit-markdown` ([LAZY-4](LAZY-4-tamboui-capabilities.md)). The PR and checks lines belong to REVIEW-1.
