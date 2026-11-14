# 110. A new ticket is created by a `/ticket new` session, not by lazyclaude

Date: 2026-11-14

## Status

Accepted. Supersedes ADR 0054's capture write; the rest of ADR 0054 (every other ticket write through a `/ticket` verb,
the board read through `gh`, the poll, no cache, a refresh keeping the selection) stands. Supersedes ADR 0097's
`board/capture` answer. Restores ADR 0009's "the one write".

## Context

ADR 0054 had `c` on the Board create a GitHub issue with no labels through `gh issue create`, its body the project's
ticket template. That made lazyclaude a second creator of tickets beside the harness's `/ticket new`, with its own rules:
an issue with no `status:`, type or context label, read as an unassigned `draft`, a body that skipped what `/ticket new`
writes, and a create path (`TicketCapture*`, `TicketTemplate*`, `board/capture`) to keep in step with the harness. Every
other ticket write already goes through a `/ticket` verb in a session.

## Decision

- **Capture starts a hosted session on `/<plugin>:ticket new <description>`.** `c` on the Board, or the desktop's "New
  ticket", asks for a description in the same one-line popup; enter starts
  `claude --session-id <uuid> -n "new ticket" "/<plugin>:ticket new <description>"` in the project root, `<plugin>` being
  the configured ticket plugin, and focus moves into that session. It is a third launch of `session`'s hosted sessions,
  beside a plain one and a ticket's verb.
- **The description is one argument, verbatim.** It goes to the pty's argv as typed, no shell in between, so quotes and
  spaces reach `claude` unchanged. A blank one is refused in the popup and starts nothing.
- **The session is unlinked.** No ticket id exists yet, so it is listed among the unlinked sessions under its name, `new
  ticket`, and its launch is no ticket run. The issue it creates reaches the Board on the poll's next change (ADR 0108);
  the launch forces no re-read and selects nothing.
- **A launch that did not start says why in its answer.** `NotStartedHost` carries the `Reason` its warning carries, so
  the popup stays open with the description and shows that reason. The warning is still raised, so the other launches
  behave as before.
- **`board` is read-only again.** `TicketCapture*`, `TicketTemplate*`, their repositories and `board/capture` are deleted,
  so lazyclaude never runs `gh issue create`, and the pending review (ADR 0009) is its one write to GitHub.
- Rejected: keeping `gh issue create` and labelling the issue `status:draft` with a type and context, which restates
  `/ticket new`'s rules in lazyclaude; and a headless `claude -p` capture, which hides the questions `/ticket new` may ask.

## Consequences

- A captured ticket carries the labels and sections `/ticket new` gives it, rather than arriving label-less.
- Capture needs the `claude` CLI and the ticket plugin, not only `gh`; a project without the plugin gets a session that
  says the skill is unknown.
- The Board shows the new ticket only once the session has created it and the poll has seen the change, up to 10 s later.
- Reading an issue with no `status:` label as `draft` stays, for issues created elsewhere.
