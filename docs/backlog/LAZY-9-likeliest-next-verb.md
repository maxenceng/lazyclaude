---
id: LAZY-9
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-6]
---

# Which `/ticket` verb is preselected when starting a session from a ticket?

## Question

Which `/ticket` verb is preselected when starting a session from a ticket?

`draft` → `refine` and `todo` → `start` are unambiguous. An `in-progress` ticket could take `design` (it changes a screen) or `review`; an `in-review` one `respond` (unresolved comments) or `done` (merged). What signals decide, and what is preselected when a signal cannot be read (no network, no PR yet)?

## Kind

grilling

## Resolution

Grilled 2026-10-10. Verb preconditions come from `/ticket`'s step files (ddd-workflow 2.2.2):
`refine` needs `draft`, `start` needs `todo`, `design` needs `in-progress` plus a change
under `frontend/**`, `review` moves `in-progress` → `in-review`, `respond` stays in
`in-review`, `done` refuses an unmerged PR.

- **Status source:** a ticket's status is read from its branch copy whenever the branch
  exists — the worktree's file, else `git show <branch>:docs/backlog/<file>` — and from
  the root checkout only when there is no branch. `/ticket` commits status changes on the
  branch, so the root copy reads `todo` until the merge. The board reads status the same
  way; this is not picker-only logic.
- **Offered verbs:** only those `/ticket` accepts for the status — `draft` → `refine`;
  `todo` → `start`; `in-progress` → `review`, plus `design` when the branch touches
  `frontend/**`; `in-review` → `respond`, `done`. One is preselected and the rest are
  reachable with the cursor. A verb `/ticket` would refuse is never listed.
- **`in-progress`:** `design` is offered only if `git diff --name-only main...<branch>`
  touches `frontend/**`. It is preselected when that holds and LAZY-3's transcript scan
  finds no `/ticket design <ID>` session yet; otherwise `review` is preselected. If the
  diff can't be read (no branch), `review` is preselected and `design` isn't offered.
- **`in-review`:** `done` is preselected only when the PR is confirmed `state == MERGED`;
  in every other case — open, closed unmerged, no PR found, no network — `respond` is,
  with the PR state shown as unknown when it couldn't be read. The unresolved-thread count
  (LAZY-6) is only a hint ("3 unresolved"), never a signal, because `respond` also answers
  top-level comments and review bodies that have no `isResolved`.
- **Timing:** the picker opens at once on the status-only default (`review` / `respond`)
  with a "checking PR…" marker. When the `git`/`gh` result lands, it moves the
  preselection, but only if the cursor hasn't been moved.
- **`done` tickets:** no verb. "New session" offers only a plain session with no ticket,
  run in the project root (the worktree may be gone) and not linked to the ticket.
