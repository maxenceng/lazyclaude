# 24. Sessions are linked to tickets by args, then branch, against ids the shell hands over

Date: 2026-10-11

## Status

Accepted. Extends ADR 0015 and ADR 0023. Amended by ADR 0047: a link is known at launch, and the branch rule goes.

## Context

A ticket's sessions are what the board is for, but a session knows nothing of the board: the only record of what it
worked on is its transcript. `session` may not import `board` (ADR 0011), and a ticket created after a transcript was
read must still find the sessions that named it.

## Decision

- **The link rule is `/ticket` args, then branch.** A session whose transcript carries `/ticket <verb> <ID…>`, in a
  command's `<command-args>` or a `Skill` call whose skill is `ticket` or `*:ticket`, is linked only to the ids it
  named, matched without regard to case against the board's. Its `gitBranch` is ignored, even when it names another
  ticket, and ids the board lacks link nothing: a session whose named ids all miss is unlinked. A session with no such
  args is weakly linked to each ticket a recorded `gitBranch` names, read as a prefix of the board's own id shape
  (`^([A-Za-z][A-Za-z0-9]*-\d{1,18})(?!\d)`), so any id the board can hold can be named by a branch; `main`, or a branch with no
  ticket-id prefix, links nothing. A link carries the last verb run on its ticket; a weak one has none.
- **`pr-link` is deferred.** It needs a pull request's head branch, which is `review`'s, and `review` does not exist.
  It is a `deferred:` note on the rule, closed when `review` publishes head branches.
- **A live session is linked through its transcript, found by session id.** One with no transcript yet is unlinked and
  moves under its ticket once its transcript names it; the transcript watcher's change reuses the last read of the
  live sessions and links them again.
- **Raw link facts are kept in the cache.** Every `/ticket` run that named ids and every ticket-id prefix of a branch
  is stored as the transcript spelt it, not filtered by the board, so a ticket made later links its sessions without
  a transcript being read again. This is a bump of the cache's format version to 2; a cache of version 1 is empty and
  rewritten, as ADR 0023 has it.
- **The shell hands the ticket ids over.** `SessionPayload` carries them beside the project root, as ADR 0015 does for
  the root, so `session` imports nothing from `board`. `SessionManager` owns the rule against them. The ids are those
  of the board when it was read; a ticket created while lazyclaude runs links at the next start.
- **The marker ranking is `session`'s.** `Marker` (`!` > `?` > `•`) comes from `SessionState`; busy and unknown rank
  nothing. The views answer the strongest marker overall and per ticket, and which of a ticket's
  sessions holds it, so the shell never re-ranks. The board row
  and the status line are the shell's join.

## Consequences

- The first start after this change reads every transcript once more, off the UI thread, to find the link facts.
- A session that ran `/ticket` on a ticket and later worked on another through its branch alone stays linked to the
  first only; the args are taken as the user's word.
- Args whose first word has the shape of a ticket id, or that name no id, are not a run, so the branch decides.
  `session` does not know `board`'s verbs, so any other first word is taken as the verb.
- Linking depends on two more things Claude Code does not promise: the `<command-args>` markup and the `Skill` call's
  input shape. A change shows as a session under `unlinked`, not as a failure.
