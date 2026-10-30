---
id: BOARD-8
status: done
context: board
type: feature
tracker_id: 105
created: 2026-10-27
blocked_by: [LAZY-33]
---

# The board reads the project's tickets from GitHub Issues

## Acceptance criteria

- [ ] The board lists the open issues of the project's GitHub remote and the ones closed as completed, read through
      `gh`, grouped by their `status:` label, with `done` for closed as completed (ADR 0052); `docs/backlog/` is no
      longer read and its file watch goes, in the same change.
- [ ] An open issue with no status label is a `draft`, unassigned without context and type labels; from `todo` until
      `done`, a ticket without exactly one `context:` and one `type:` label is unreadable, as is one with two status
      labels or closed as not planned — each listed with its reason. A `done` one, like a `draft`, may lack them.
- [ ] The ticket detail shows the issue number, title, labels as facts, and the body's `##` sections as markdown.
- [ ] A project without a GitHub remote, an unreachable GitHub at start, and `gh` not logged in each show on the Board
      as the reason, the last naming `gh auth login`; nothing is cached.
- [ ] The board polls every 30 s with an ETag-conditional request and re-reads only on a change; `R` reads at once.
- [ ] A failed poll or `R` keeps the board on screen under the existing warning, cleared by the next good read.
- [ ] A refresh keeps the selection on the same issue number, following it into its new status group; a ticket gone
      from the board leaves the cursor on the next row of its old group. The ticket detail keeps its scroll, clamped
      when the body shrank; folds and zoom are untouched.
- [ ] Sessions still link to tickets by their id, now the issue number.

## Model decision

Same aggregate, new source: `BacklogPort` keeps its shape and gains an issues repository over `gh` in
`board.infrastructure.secondary`, replacing the markdown one; `BoardWatchPort` is implemented by the poll. The branch
copy, root copy and their ports go (ADR 0052 supersedes ADR 0028). Ticket id becomes the issue number across `board`'s
published types.

## Design direction

Unchanged screen; the facts line shows `#<number>` and the labels in place of the frontmatter keys.

## Glossary impact

*Ticket* becomes an issue's title, labels and body; *GitHub issue* drops `(expected)`; *Ticket branch*, *Branch copy*,
*Root copy* and *Status* change meaning or go (ADR 0052).

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-32](LAZY-32-lazyclaude-writes-to-issues.md),
recorded in ADR 0054, model in ADR 0052. One cut-over: lazyclaude's own board is empty until its backlog is migrated,
hence `blocked_by` the migration decision.
