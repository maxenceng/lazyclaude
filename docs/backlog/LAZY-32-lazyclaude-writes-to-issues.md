---
id: LAZY-32
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-30]
---

# What does lazyclaude write to GitHub Issues, and how does it avoid overwriting a remote change?

## Question

What does lazyclaude write to GitHub Issues, and how does it avoid overwriting a remote change?

Which writes the board and the ticket detail offer (create, status, body, labels, links), how each is checked against the remote state first, what offline or a failed call shows, and how a refresh keeps the selection and scroll. Ends in the ADR that supersedes the read-only charter (CLAUDE.md, ADR 0009's "the one write").

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0054](../adr/0054-lazyclaude-captures-a-draft-ticket-and-every-other-ticket-write-goes-through-a-ticket-verb.md).

- **Writes**: capture only, `c` on the Board — a title popup, an issue with no labels (an unassigned `draft`), the body
  from `.github/ISSUE_TEMPLATE/ticket.md` or empty. Status, body, labels and links stay with the `/ticket` verbs in a
  session, so the `updatedAt` check and the status rules live only in `ticket-tracker` (ADR 0053). A create overwrites
  nothing, so it checks nothing. The review's pending review stays the other write (ADR 0009).
- **Failed create**: the popup keeps the title and shows the reason; `enter` retries, `esc` drops; no automatic retry,
  since a timed-out create may have landed.
- **After a create**: the board is re-read at once and the new draft selected by its number; no optimistic row.
- **Read**: through `gh`, as `review` does; a 30 s ETag-conditional poll plus `R` replaces the file watch.
- **Offline**: nothing cached; at start the Board shows the reason (`gh auth login` when not logged in); later, the
  board on screen is kept under the existing warning.
- **Refresh**: selection follows the issue number into its new group, else the next row of the old group; detail
  scroll kept and clamped; folds and zoom untouched.
- **Charter**: CLAUDE.md's out-of-scope line names both writes when capture lands.

Graduated: [BOARD-8](BOARD-8-the-board-reads-github-issues.md), [BOARD-9](BOARD-9-capturing-a-draft-ticket.md).
