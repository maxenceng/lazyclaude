---
id: LAZY-30
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-29]
---

# How does a ticket map onto a GitHub issue?

## Question

How does a ticket map onto a GitHub issue?

- the id: the issue number, a `CONTEXT-n` kept in the title, or both — and the ticket branch's name, which today is the filename;
- status (`draft` … `done`): open/closed plus a label, or a Projects field; what replaces reading status from the ticket branch's copy (ADRs 0028, BOARD-3's rule), now that status no longer rides along in the branch;
- context and type, the body's sections (Model decision, Design direction, acceptance criteria), `map`/`decision` and `blocked_by`;
- what has no natural home on an issue, and where it goes.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0052](../adr/0052-a-ticket-is-a-github-issue-named-by-its-number-with-labels-for-status-context-and-type.md).

- **Id**: the issue number. The context moves to a required label; no `CONTEXT-n` counter. Old names of migrated
  tickets are LAZY-33's.
- **Ticket branch**: what `gh issue develop` creates, `<number>-<title-slug>`, linked by GitHub.
- **Status**: `status:draft|todo|in-progress|in-review` labels on an open issue; `done` is closed as completed, by the
  pull request's closing keyword on merge, whatever labels remain. No Projects v2 field, nothing derived.
- **What replaces the branch copy**: nothing — status is the issue's alone, the issue timeline is its history; ADR 0028
  and the LAZY-9/LAZY-17 status source are superseded when the backlog read moves.
- **Context, type, `blocked_by`, map/decision**: `context:<name>` and `type:<…>` labels; native dependencies for every
  type; a map's decision tickets are its sub-issues.
- **Missing labels**: no status label is `draft`; a `draft` may lack context and type (unassigned); from `todo` on,
  exactly one of each or the ticket is unreadable; two status labels, or closed as not planned, are unreadable too.
- **Body**: the template's `##` sections, no frontmatter; issue title is the title, `createdAt` the date.
- **No natural home**: backdated `created` and the old ids (LAZY-33); a project without a GitHub remote has no backlog,
  with the reason on the board, and no markdown fallback.
- **Words**: *ticket* stays the domain word; *issue* only for the GitHub object storing one, in the secondary adapter.
