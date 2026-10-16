---
id: SESSION-5
status: in-review
context: session
type: feature
tracker_id: 82
created: 2026-10-10
---

# Starting or resuming a session from a ticket: the verb picker with a preselected verb, run in the ticket worktree, which is created if missing

## Acceptance criteria

- [ ] `n` on a ticket, or `enter` on its `n new: /ticket <verb>` row, opens the verb picker over
      the Sessions panel with the cursor on the preselected verb; `j`/`k` move, `enter` starts,
      `esc` cancels. Only the verbs `/ticket` accepts for the ticket's status are listed:
      `draft` → `refine`, `todo` → `start`, `in-progress` → `review` (and `design`, below),
      `in-review` → `respond`, `done`.
- [ ] `enter` starts `claude -n "<ID> <verb>" "/ticket <verb> <ID>"` in the ticket worktree, as a
      new dock tab with focus, reading `starting`. The session is listed under its ticket from
      its first frame, before its transcript links it.
- [ ] `in-progress`: `design` is offered only when `git diff --name-only main...<ticket branch>`
      touches `frontend/**`, and preselected when it is offered and no `/ticket design <ID>`
      session exists; otherwise `review` is preselected. When the diff cannot be read (no
      branch), `design` is not offered.
- [ ] `in-review`: `done` is preselected only when the ticket branch is among the merged pull
      requests' heads; otherwise `respond` is. Until review's first merged read lands, the
      picker reads `checking PR…`; when it lands, the preselection moves only if the cursor has
      not been moved.
- [ ] A missing ticket worktree is created before the session starts: on the ticket branch when
      it exists, else on a new ticket branch from `main`. A ticket branch already checked out in
      another worktree, the root checkout included, runs the session there instead.
- [ ] When the ticket worktree's path exists but is not a worktree, or `git worktree add` fails,
      no tab opens and the status line warns why.
- [ ] `enter` on a ticket's past session resumes it in its recorded cwd. When that cwd is the
      ticket worktree's path and it is gone, the worktree is recreated first, as above. Any
      other missing cwd still warns and resumes nothing.
- [ ] On a `done` ticket, `n` starts a plain unlinked session in the project root, as `N` does,
      and its sessions section has no `n new` row.
- [ ] `enter` on a ticket with no session showing a marker lands on its `n new` row.

## Model decision

**New behaviour on `board` (allowed verbs) and on `session`'s `HostedSession` aggregate (the
pick, the worktree, the launch).** No new context; one new edge, `session` → `board.published`.

- **`board`.** A published `Verb`, and each ticket's allowed verbs on `TicketView`, from its
  status and, for `design`, whether the ticket branch touches `frontend/**` — a diff read by
  board's git adapter. `board` stays read-only.
- **Shared kernel.** The ticket worktree's path rule, `../<repo>.worktrees/<ticket branch>`,
  moves from `BoardManager` onto `shared.domain.Project`; `board`'s worktree check uses it.
- **`session`.** Picks the preselected verb from the allowed verbs (read from
  `board.published`), its own past `/ticket design <ID>` runs, and the merged flag the shell
  takes from review's last merged read. A `WorktreePort` (find where a branch is checked out,
  add a worktree on an existing or fresh branch) is implemented by a git adapter in
  `session.infrastructure.secondary`. A new `LaunchCommand` variant carries the ticket and the
  verb, and `PtyHostRepository` turns it into `-n "<ID> <verb>" "/ticket <verb> <ID>"`.
  `HostedSession` carries the ticket it was started for, so it is listed under it while
  starting.
- **Shell.** The verb picker popup, `n`, the `n new` row, and handing `session` the ticket's
  allowed verbs and merged flag through `HostedSessionCommand`.
- **ADR, in the implementing change:** allowed verbs in `board` and the pick in `session`, with
  the new edge; worktree creation in `session`; review's last read standing in for LAZY-9's
  fresh PR query.
- Not in this ticket: removing a worktree (SESSION-8), outside sessions (SESSION-6).

## Design direction

The verb picker, a popup over the Sessions panel:

```
╭ SESSION-5 · in-review ── checking PR… ╮
│ › respond                             │
│   done                                │
╰ enter start · esc cancel ─────────────╯
```

- Title: the ticket id, its status, and `checking PR…` while review's first merged read is
  pending.
- Each ticket's sessions section ends with `n new: /ticket <preselected verb>`, except on a
  `done` ticket.
- Keys: `n` opens the picker (on a `done` ticket, starts an unlinked session), `j`/`k` move,
  `enter` starts, `esc` cancels. The key bar gains `n new`.

## Glossary impact

- **Verb** and **Preselected verb** drop their *(expected)* marker.
- **Ticket worktree**: its path moves to `shared`; `session` creates it, no longer expected.
- Adds **Verb picker**: the popup `n` opens on a ticket, listing its allowed verbs with the
  preselected one under the cursor.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-9](LAZY-9-likeliest-next-verb.md), [LAZY-7](LAZY-7-handing-over-to-a-session.md), [LAZY-15](LAZY-15-trusting-ticket-worktrees.md). The worktree is `../<repo>.worktrees/<id>-<slug>`. The PR-state refinement of the preselected verb needs REVIEW-1's merged state. The worktree's path rule already lives in `board` (`BoardManager.ticketWorktree`, from BOARD-3): share it — likely beside `Project` in `shared.domain` — rather than writing a second copy.

Refined 2026-10-15. Where it parts from LAZY-9, and the limits it accepts:

- The merged state is review's last read, not a fresh `gh` query when the picker opens: review
  already polls it every 60 s.
- Review turns a failed merged read into an empty list, so a failed read leaves `respond`
  preselected and LAZY-9's "PR state unknown" is not shown. Showing it needs review to keep the
  failure apart from an empty list.
- The merged read is capped at the 100 newest merged pull requests; a ticket branch merged
  before those reads as unmerged, so `respond` is preselected.
