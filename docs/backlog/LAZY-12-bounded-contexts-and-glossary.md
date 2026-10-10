---
id: LAZY-12
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-2, LAZY-3, LAZY-6]
---

# What are lazyclaude's bounded contexts, and their words?

## Question

What are lazyclaude's bounded contexts, and their words?

`docs/context-map.md` expects `board` and `session`; review, docs and notification may be their own or belong to one of those. Which context owns the ticket↔session link, the next-verb rule, a waiting state? Candidate terms from LAZY-7:
- *hosted session* (embedded on a pty) vs *outside session* (live in another terminal);
- *unlinked session* (no ticket);
- *ticket worktree*.

Candidate terms from LAZY-8:
- *waiting* (for permission / for input) vs *your turn* (idle);
- *project*, meaning the main worktree's root that sessions group under.

Each term resolved goes into `docs/glossary.md` in the same change.

## Kind

grilling

## Resolution

Grilled 2026-10-10. The words are in `docs/glossary.md` (marked *expected* until code
introduces them) and the contexts and edges in `docs/context-map.md`.

**Contexts.** Four, plus `shared`; feature tickets take the prefixes `BOARD`, `SESSION`,
`REVIEW`, `DOCS`.
- `board`: tickets, status, ticket branch and branch copy, allowed verbs. Read-only. It
  reads the `frontend/**` diff itself, since `design` is allowed only when the ticket
  branch touches it.
- `session`: discovery and state, the link rule (LAZY-3), the preselected verb, ticket
  worktrees, hosting on a pty, outside sessions and the kitty jump, desktop notifications
  (`notify-send` is a secondary adapter behind a port; "suppressed while focused" is an
  input from `shell`).
- `review`: the pull request, its diff and threads, your pending review, submit, merged
  state. Picked over folding it into `board`: nothing in its model is a ticket's.
- `docs`: the tree of documents under `docs/`, excluding `docs/backlog/`. Thin, but the only
  honest owner of that file-reading port; folding it into `board` would make *ticket* stop
  covering what `board` holds.
- Notifications are not a context: their only subject is a session's waiting state.
- `shared` starts with `Project`, the main worktree's root.

**Edges.** `session → board` (ticket id, status, allowed verbs, ticket branch);
`session → review` (PR head branch for the `pr-link` rule, merged state for `done`);
`review → board` (ticket branch). `board` knows nothing of sessions or pull requests;
a ticket's strongest marker is `shell`'s join. `docs` stands alone.

**Ownership.**
- Link rule: `session`, because its evidence is all session data.
- Verbs: split. `board` owns which verbs a status allows (`/ticket`'s lifecycle);
  `session` owns which one is preselected, since that needs past sessions and PR state.
- Ticket branch and branch copy: `board`, which names the branch from the ticket. Ticket
  worktree: `session`, which creates it from that name.

**Words.**
- *Project* is the main worktree's root. `~/.claude/projects/*` are *transcript folders*.
- A session has two independent properties. Where: *hosted* / *outside* / *past*; a
  hosted session whose `claude` exited is past. Ticket: *linked* / *unlinked*, plus
  *weak link* for the branch rule.
- States are *busy*, *your turn*, *waiting for input* and *waiting for permission*;
  *waiting* covers the last two and is the only one that notifies. The registry's `idle`
  is avoided, and `shell` maps to busy inside the adapter.
- *Verb* and *preselected verb*; "next verb" and "likeliest verb" are avoided.
- *Status* means the branch copy's when a ticket branch exists; there is no root status.
- `review` words: *pull request* (`PullRequest` in code, "PR" in UI text),
  *pending review*, *pending comment* (a line or a range inside one hunk), *thread*,
  *summary*, *submit*, *moved head*.

**Glossary timing.** The header now allows a term settled by a decision ticket to land
early, marked `(expected)`; the code change that introduces it drops the marker.

No ADR: the split is recorded in the context map, which `ArchitectureTest` will enforce,
and each context can still be renamed or merged before its first ticket lands.
