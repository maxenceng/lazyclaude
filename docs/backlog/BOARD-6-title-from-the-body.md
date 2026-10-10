---
id: BOARD-6
status: in-review
context: board
type: chore
tracker_id: 90
created: 2026-10-10
---

# The ticket derives its title from its body, rather than the repository deriving both from the same lines

## Acceptance criteria

- [x] Every ticket shows the same title on the board and in the main view as before: the first `# ` line of its body outside a ```` ``` ```` or `~~~` fence, stripped.
- [x] A `# ` line inside a code fence, a `## ` heading, or a `#` with nothing after it never becomes the title; the ticket has `NoTitle` and is shown under its id, as today.
- [x] A ticket whose body is `NoBody` has `NoTitle`.
- [x] A `Ticket` cannot be built with a title its body disagrees with: nothing hands it a title, so there is no builder call to get wrong.
- [x] The title rule is tested on `Ticket` from a body alone, with no file; `BacklogRepository` and its tests no longer mention titles.

## Model decision

Behaviour moved onto the existing `Ticket`, in the `board` context. No new aggregate, no new type.

- **`board` domain**: `Ticket` loses its `title` component and its builder's `title(...)`. `title()` becomes a method that derives `Title | NoTitle` from `body`: `NoBody` gives `NoTitle`, a `Body` is scanned for its first `# ` line outside a code fence. Derived on each call rather than stored, so a title and a body can never disagree; the one caller, `TicketView.from`, calls it once per board read, which costs nothing at backlog size.
- **`board` infrastructure.secondary**: `BacklogRepository` drops `title(...)`, its fence and prefix constants, and the title from its javadoc; `TicketFrontmatter.toDomain` takes the body only.
- **`board` infrastructure.primary** and **`shell`**: unchanged — `TicketView` still carries a `TicketTitle`, taken from `ticket.title()`.
- **Why not `TicketTitle.of(TicketBody)` in `published`**, beside `TicketBody.of`: the rule is what makes a heading a ticket's title, which is the ticket's to know; `published` carries the shapes the shell reads, not the rules that produce them. No ADR — this follows ADR 0011's layering rather than choosing anything new.

## Design direction

n/a — no screen changes.

## Glossary impact

none — *Ticket* already reads "frontmatter plus body", and the title is part of the body.

## Notes

Raised in review of [BOARD-2](BOARD-2-ticket-detail.md) and deferred there: `BacklogRepository` reads the title and the body from the same lines, so they cannot disagree today, but the rule (first `#` heading outside a code fence) is a domain rule living in a secondary adapter.
