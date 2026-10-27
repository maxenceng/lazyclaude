# 54. lazyclaude captures a draft ticket, and every other ticket write goes through a `/ticket` verb

Date: 2026-10-27

## Status

Accepted. Amends ADR 0009 ("the one write") and the read-only charter in CLAUDE.md, once the board reads GitHub Issues
(ADR 0052); until then they stand.

## Context

The desktop evolution moves the backlog to GitHub Issues and asks that lazyclaude become writable on them without
silently overwriting a remote change. GitHub has no conditional write, only an `updatedAt` to compare before writing
(LAZY-29), and the harness already writes every status move and body section through `ticket-tracker`, which refuses a
section write when `updatedAt` moved (ADR 0053). lazyclaude already changes a ticket's status today, but only by
starting a session on a `/ticket` verb from the verb picker. The board reads `docs/backlog/` under a file watch, and
keeps the board on screen with a warning when a read fails.

## Decision

- **lazyclaude makes two writes to GitHub: ADR 0009's pending review, and capturing a draft ticket.** `c` on the Board
  asks for a title in a one-line popup and creates an issue with no labels, which ADR 0052 reads as an unassigned
  `draft`. Its body is the ticket template's sections from `.github/ISSUE_TEMPLATE/ticket.md`, or empty without one.
  A create overwrites nothing, so it checks nothing first.
- **Every other ticket write goes through a `/ticket` verb in a session.** Status moves, body sections, labels and
  links stay with the harness, so their rules and the `updatedAt` check live in one place. Rejected: status moves from
  the board, a key or a drag between status columns, which would restate `/ticket`'s refusals in lazyclaude; and
  editing a body or labels in lazyclaude, against "edit in your own editor or through a session". A direct write
  beyond capture needs a new ADR.
- **A failed create keeps the popup open** with the typed title and the reason; `enter` tries again, `esc` drops it.
  Nothing retries by itself, because a timed-out create may have succeeded and a blind retry would duplicate it.
- **A successful create reads the board again at once and selects the new draft** by the number `gh` returns. No row
  is inserted before that read.
- **The board reads Issues through `gh`**, the same process and authentication `review` uses. Rejected: `java.net.http`
  with a token taken from `gh`, a second auth path for no gain.
- **A 30 s ETag-conditional poll, plus `R`, replaces the file watch.** A 304 costs no rate limit. Rejected: reading only
  on `R` and at start, which leaves the board stale; and re-reading on focus, which terminals report unreliably.
- **No read is cached.** At start, an unreachable GitHub, or `gh` not logged in, shows on the Board as the reason, the
  latter naming `gh auth login`. A later failed read keeps the board on screen under the existing warning, cleared by
  the next good read.
- **A refresh keeps what the user was looking at.** The selection follows the issue number into its new status group;
  a ticket gone from the board hands the cursor to the next row of its old group. The ticket detail keeps its scroll,
  clamped when the body shrank. Folds and zoom are untouched.

## Consequences

- Capture works offline only in the sense that the popup opens; the create itself needs GitHub.
- A duplicate draft from an ambiguous failure is visible on the board and closed on GitHub by hand.
- Moving a ticket on still takes a session, even for a one-label change; the board offers no shortcut.
- CLAUDE.md's out-of-scope line names both writes in the change that lands capture.
