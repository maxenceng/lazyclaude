---
id: SESSION-3
status: in-review
context: session
type: feature
tracker_id: 80
created: 2026-10-10
---

# A ticket's sessions are linked and listed in order, its strongest marker shows on the board, and the strongest overall shows in the status line

## Acceptance criteria

- [x] A session whose transcript carries `/ticket <verb> <ID…>` (in `<command-args>`, or a
      `Skill` tool_use whose skill is `ticket` or `*:ticket`) is linked only to the ticket ids
      it named, matched case-insensitively against the board's ids. Its `gitBranch` is
      ignored, even when it names another ticket.
- [x] A named id that is not on the board links nothing; a session whose named ids all miss
      is unlinked, not linked by its branch.
- [x] A session without `/ticket` args is weakly linked to every ticket a `gitBranch` it
      recorded names (`^([A-Za-z]+-\d+)`, case-insensitive). `main`, or any branch with no
      ticket id prefix, links nothing.
- [x] A live session is linked through its transcript, found by session id. A live session
      with no transcript yet is unlinked, and moves under its ticket once its transcript
      names it.
- [x] Selecting a ticket on the board shows its section in the Sessions panel: a dim header
      row with the ticket id, its live sessions newest-started first, then its past sessions
      by last activity, newest first. Every linked session is shown; there is no fold.
- [x] The unlinked section follows under a dim `unlinked` header row: live sessions, then the
      5 newest past ones and the `… N older` row, as the panel lists them today.
- [x] A session linked to two tickets is listed under each of them. A weak link carries a dim
      `(branch)` suffix and stays in the same order as the others.
- [x] A linked row reads its verb, then its title: the last `/ticket` verb the session ran on
      that ticket. A weak link has no verb. Unlinked rows keep their current text.
- [x] A ticket with no linked session shows a dim `no session` row in its section.
- [x] With no ticket selected (a status header, or an empty board), only the unlinked section
      shows.
- [x] Header rows are skipped by `j`/`k`. The selection follows the session id across
      refreshes; when the board selection moves to another ticket, it goes to the first row.
- [x] A ticket row on the board shows, in the gutter before its id and in its colour, the
      strongest marker among its linked live sessions, weak links included (`!` > `?` > `•`).
      A ticket whose sessions are all busy, unknown or past shows none.
- [x] The status line shows, on its left and in its colour, the strongest marker among this
      project's live sessions, linked or not. With none, it shows nothing there. Warnings stay
      on the right.
- [x] `enter` on a ticket moves to the Sessions panel on that ticket's session with the
      strongest marker; a tie goes to the first row. With no marker it lands on the ticket's
      first row, and on `no session` when it has none.
- [x] Ticket ids, verbs and branch ids are kept in the transcript cache raw, not filtered by
      the board, so a ticket created later links its sessions without reading a transcript
      again. A cache of the previous format version is treated as empty and rewritten.

## Model decision

**New behaviour in the existing `session` context; the board marker and the status line are
`shell`'s join.** No new context, and no new edge between contexts.

- **Transcript read (`session.infrastructure.secondary`).** `TranscriptTail` also collects
  every `/ticket` verb and id set, from `<command-args>` or a `Skill` tool_use, and every
  ticket-id prefix of a `gitBranch`. The prefilter lets those lines through. They go into
  the cache raw, which is a format version bump.
- **Domain.** `SessionManager` owns the link rule against the ticket ids it is handed:
  `/ticket` args, else branch (weak), else unlinked. A live session reaches its transcript's
  link facts by session id from the cache, the same entry that its live id hides from the
  past list. The marker ranking (`!` > `?` > `•`; busy, unknown and past rank nothing) is
  domain behaviour on `SessionState`'s side, so the shell asks rather than re-ranks.
- **Hand-over.** `SessionPayload` gains the board's ticket ids, handed over by the shell as
  it hands over the project root (ADR 0015). `session` still imports nothing from `board`.
- **Views.** Live and past session views carry their links: ticket id, strong or weak, and
  the verb. `SessionQuery` stays the shell's only entry.
- **Shell.** `SessionRows` builds the selected ticket's section and the unlinked section,
  with header and `no session` rows that the cursor skips or lands on as the criteria say.
  The board row and the status line take the strongest marker from the views. Coloured
  board rows need `RowsElement` to take a style per row.
- **`pr-link` is deferred.** It needs a pull request's head branch, which is `review`'s, and
  `review` does not exist yet. It is a `deferred:` note on the link rule, closed when
  `review` publishes head branches.
- **ADR, in the implementing change:** the link rule as built (args, then branch; `pr-link`
  deferred), raw link facts in the cache, and ticket ids handed to `session` by the shell.
  `docs/context-map.md`'s `session → board` arrow becomes that hand-over.
- Not in this ticket: the `n new: /ticket <verb>` and `N new unlinked` rows (SESSION-4,
  SESSION-5), per-state counts and `elsewhere:` on the status line (SESSION-7), hosted and
  outside sessions.

## Design direction

The Sessions panel with SESSION-3 selected on the board:

```
╭ Sessions ──────────────────────────╮
│ SESSION-3                          │
│ ! refine  Refine SESSION-3     5m  │
│ ○ start   Fix linking          2h  │
│ ○ Branch work (branch)         1d  │
│ unlinked                           │
│ ∼ lazyclaude-3f               12m  │
│ ○ Fix status line              3h  │
│ … 42 older                         │
╰────────────────────────────────────╯
```

- Header rows (`SESSION-3`, `unlinked`) are dim and never selected.
- The verb sits before the title. `(branch)` is dim.
- A ticket with no session: `SESSION-3`, then a dim, selectable `no session`.
- Board row: `! SESSION-3 Ticket title`, the marker in the two-space gutter in its colour
  (`!` red, `?` yellow, `•` cyan).
- Status line: the strongest marker on the left in its colour; the warning stays right.
- `enter` on a ticket moves focus to the Sessions panel, as above.

## Glossary impact

- **Linked session**, **Weak link** and **Unlinked session** drop their *(expected)* marker.
  **Linked session** names only `/ticket` args and branch until `pr-link` lands.
- **Past session** drops "Until linking lands, every past session is unlinked."
- Adds **Marker**: `!` waiting for permission, `?` waiting for input, `•` your turn; the
  strongest is `!` > `?` > `•`. Busy, unknown and past are not markers.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-3](LAZY-3-session-ticket-link.md), [LAZY-18](LAZY-18-ordering-a-tickets-sessions.md), markers from [LAZY-10](LAZY-10-screen-layout-and-keys.md). The marker on the board is `shell`'s join.
