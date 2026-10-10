# 14. The ticket detail rides the board read

Date: 2026-10-10

## Status

Accepted.

## Context

The main view shows the selected ticket's whole frontmatter and its body rendered as markdown.
The board read already opens every backlog file to take its `id`, `status`, `context`, `type` and title,
then drops the rest. The detail could come from that same read, or from a second query that reads
one ticket whenever the selection moves.

## Decision

- **`Ticket` carries its `Frontmatter` and its `Body | NoBody`**, and the board read keeps them.
  There is no per-ticket query. `TicketView` hands both to the shell.
- **`Frontmatter` is ordered key → display text**, the keys the board does not model as fields,
  in file order. The secondary adapter that reads `id`, `status`, `context` and `type` into fields
  leaves them out of it, so a modelled key is held once and the shell lists `Frontmatter` without
  filtering. The adapter turns SnakeYAML values into text: a scalar as written, a list as `[A, B]`.
  The domain interprets none of these keys. A value written over
  several lines shows on one row, its lines joined by a space, because a main view row holds one
  line.
- **`Body | NoBody` and the frontmatter's `FrontmatterKey` and `FrontmatterValue` are `published`**,
  not domain, because the shell reads them through `TicketView` (ADR 0012), as it reads
  `Title | NoTitle`.
- **The values are still loaded before their nodes are read.** Reading nodes keeps a scalar's text
  as written, but only loading refuses a key written twice, so a file that writes one stays an
  unreadable ticket, as it was before the board read nodes. A value that contains itself through
  an alias is refused too, since it has no text.
- **The shell renders the body** with `tamboui-markdown`'s `MarkdownView` and owns the main
  view's scroll offset. The whole detail scrolls as one unit: facts line, keys, rule and body.
  The toolkit's `MarkdownElement` scrolls only the markdown, so it would pin the head rows.

## Consequences

- The detail never disagrees with its board row, because both come from one snapshot, and the
  refresh that keeps the board current also keeps the detail current.
- There is no "file gone since the board was read" case to handle.
- Every body is held in memory for as long as the board is. At backlog size that is trivial.
  If memory ever shows up in a profile, that is what reopens this.
- A ticket's other frontmatter keys are display text, not typed values. A later feature that acts
  on one of them models it as a field, as `status` is, and adds it to the keys the adapter leaves
  out of `Frontmatter`; the shell then shows it wherever that feature puts it, and once.
- `Frontmatter` is not the whole file's frontmatter. Something that needs every key as written
  reads the file again rather than reassembling it from the fields.
