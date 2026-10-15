# 30. Allowed verbs live in `board`; the pick and the ticket worktree live in `session`

Date: 2026-10-16

## Status

Accepted. Settles the verb and worktree questions of LAZY-9, LAZY-7 and LAZY-15, and departs from LAZY-9 on the pull request query.

## Context

Starting a session from a ticket needs three things: which verbs `/ticket` accepts for the ticket, which one to start on, and a
worktree to run it in. None had a home. `board` knows a ticket's status, `session` knows which sessions ran which verb, and
`review` knows which pull requests are merged.

## Decision

- **Allowed verbs belong to `board`.** `Verb` is published; `Status.verbs()` gives the verbs of a status and `Ticket.verbs()` adds
  `design` for an in-progress ticket whose branch touches `frontend/**`. `board` reads that with `git diff --name-only
  main...<branch>` through `BranchChangesPort`, only for in-progress tickets. A branch that cannot be diffed offers no `design`.
  `TicketView` carries the verbs. `board` stays read-only.
- **The pick belongs to `session`.** `VerbChoice` takes the allowed verbs, whether a session already ran `design` on the ticket
  (which only `session` knows) and whether the ticket branch is merged, and preselects `design` while none ran it, else `done`
  once merged, else the first verb. This adds one edge: `session` reads `board.published` (`Verb`). `session` still never reads
  `board`'s ticket types; the shell hands it the verbs.
- **The ticket worktree belongs to `session`.** The path rule `../<repo>.worktrees/<ticket branch>` moves to `shared.domain.Project`,
  which `board`'s worktree check also uses. `WorktreePort`, implemented by `WorktreeRepository` over `git worktree`, finds where a
  branch is checked out and adds a worktree on the branch, or on a new branch from `main`. A branch already checked out elsewhere, the
  root checkout included, runs the session there. A path that exists without being that branch's worktree, or a failing `git worktree add`,
  starts nothing and warns. Resuming a ticket's session whose recorded directory is the gone ticket worktree adds it back first.
- **Review's last merged read stands in for LAZY-9's fresh PR query.** The shell hands `session` whether the ticket branch is among
  the heads of the merged pull requests `review` last fetched, which it does every 60 s. Until the first read lands the picker says
  `checking PR…` and its cursor follows the preselection only while it has not been moved.

## Consequences

- A failed merged read is an empty list, so `respond` is preselected and "PR state unknown" is never shown; showing it needs `review` to keep a failure apart from an empty list.
- The merged read covers the 100 newest merged pull requests, so an older merged ticket branch reads as unmerged.
- A session counts as having run `design` by its last verb on the ticket, because a link keeps only that one.
- The preselection reads the sessions afresh on the shell's thread; an unreadable registry holds it up for the `claude agents` patience.
- Resuming into a gone ticket worktree whose branch is checked out elsewhere fails with git's message, since the transcript is only found from the recorded directory.
