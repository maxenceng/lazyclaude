# 84. `session` decides what a new session from a ticket opens and where a past one resumes

Date: 2026-11-05

## Status

Accepted. Amends ADR 0030 (the pick belongs to `session`) and builds on ADR 0074 (hosted sessions over the sidecar).
Departs from the hosted-panes ticket's model decision, which allowed no backend change.

## Context

The TUI's `Shell` decided two things the desktop's `app.ts` then copied: a ticket with no verb left starts an unlinked
session rather than opening the verb picker, and a past session listed under a ticket resumes in the ticket's worktree while
one linked to no ticket resumes where it ran. With two composers the rule lived twice. The desktop also opened the picker on
the first verb, because `session/verbChoiceFor` was not served and its payload's `merged` was a boolean the desktop cannot
honestly fill: the window does not read review's merged pull requests.

## Decision

- **`SessionQuery.newSessionFor`**, served as `session/newSessionFor`, answers `VerbChoice | UnlinkedStart`. The domain
  rule is `AvailableSessions.newSessionFor`: no verb allowed is an unlinked start, otherwise the choice ADR 0030 describes.
  It replaces `verbChoiceFor`.
- **`MergeState` is `MERGED | NOT_MERGED | UNKNOWN`**, in `session.published`, in place of the boolean. Only `MERGED`
  preselects `done`. The TUI sends `UNKNOWN` until review's first merged read lands; the desktop always sends `UNKNOWN`.
- **`HostedSessionCommand.resumePast`**, served as `session/resumePast`, takes the sessions as asked for, the past session,
  its recorded directory and what its row is listed under (`TicketListingPayload(branch) | NoTicketListingPayload`).
  `HostedSessionManager.resumePast` resumes in the ticket's worktree only when the session is a linked past one of the last
  push and its row is under a ticket; otherwise in the directory it ran in. It replaces `resume` and
  `resumeInTicketWorktree` on the command and on the protocol.
- Both frontends call these two operations and keep no copy of either rule.

## Consequences

- The TUI resumes every past session on its background executor, as it already did for a linked one: the tab appears once
  `claude` has spawned rather than within the key press.
- A past session missing from the last push is not known to be linked and resumes where it ran, so a gone ticket worktree
  is not added back for it.
- The desktop never preselects `done` until it reads review's merged pull requests; that is a `deferred:` marker in
  `session/sessionsController.ts`.
