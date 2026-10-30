# 65. The board polls the newest updated issue with an ETag and says why when it cannot read issues

Date: 2026-10-30

## Status

Accepted. Implements ADR 0052 in `board`, and ends ADR 0031 and ADR 0028 for it.

## Context

ADR 0052 makes a ticket a GitHub issue and leaves the board's read open. The markdown read had a file watch, a branch copy
and a root copy; none of them exists for an issue. GitHub offers no webhook to a terminal app, a conditional request that
answers 304 costs no rate limit, and `gh` is the only client lazyclaude uses (CLAUDE.md).

## Decision

- **One call reads the board.** `gh issue list --state all --limit 1000 --json number,title,body,labels,state,stateReason`
  in the project's directory, so `gh` finds the remote itself. An open issue is the status its one `status:` label names,
  a `draft` with none. A closed one is `done` when it closed as completed, or with no reason; any other reason (not
  planned, duplicate) is an unreadable ticket. A `done` ticket, like a `draft`, may lack its `context:` and `type:` labels,
  so a draft closed straight from GitHub reads as done rather than unreadable for good (ADR 0052). The mapping lives in the domain's
  `GitHubIssue`: `IssueRecord`, the adapter, only reads the fields and hands over the state and labels, since a label rule
  is a value only the domain can judge (ADR 0043). Rejected: the rules in `IssueRecord`, beside the rest of the
  reading of what `gh` wrote, which left the domain with no say over which issue is a ticket.
- **The poll watches the most recently updated issue.** Every 30 s, `gh api --include --silent
  'repos/{owner}/{repo}/issues?state=all&per_page=1&sort=updated&direction=desc'` with `If-None-Match` set to the last
  answer's `ETag`. Any change to any issue moves it to the top and changes the answer; a 304 changes nothing and costs no
  rate limit. `gh` exits non-zero on a 304, so the adapter reads the status line, not the exit code. Rejected: polling
  `gh issue list` itself, which re-downloads every body each time; GraphQL, which has no conditional request.
- **The first check only takes the `ETag`.** The board was just read; saying "changed" would read it twice at start. A read
  of the board that fails tells the poll to forget its `ETag`, so after a start that could not read the issues, or a
  re-read the poll asked for that failed, the next good check counts as a change and the board catches up.
- **A check that fails is a change.** `changed` runs, the shell reads the board, and that read fails the same way, which
  raises the warning and keeps the board on screen. A board that was never read has nothing to keep: the shell reads it
  whole, so the reason it shows is the latest and no warning claims a board as last read. The `ETag` is forgotten, so the first good check afterwards also
  counts as a change and the board catches up on what it missed; that good read withdraws the warning (ADR 0066), and a
  later failure is named again. A failed poll that said nothing would leave a stale board
  with no warning until `R`.
- **A board that cannot be read at start still opens.** `BoardReading` is a `Board` or an `UnavailableBoard` with the
  reason (as `Docs` is a tree or `UnavailableDocs`), so the shell opens on the docs and sessions with the Board saying why.
  The shell reads it in the same shape, a `BoardView` or an `UnavailableBoardView`, so the zoomed status columns show the
  reason alone, not beside five empty columns.
  The three reasons are told from what `gh` said: no GitHub remote, not logged in (naming `gh auth login`), and the rest
  as unreachable in `gh`'s own words. "No GitHub remote" is checked before "log in": `gh` suggests `gh auth login` for a
  remote that is not on GitHub. Nothing is cached, so the next read, `R` or the poll, replaces the reason.
- **The ticket branch is derived, not read back.** `<number>-<title slug>`, as `gh issue develop` names it by default
  (ADR 0052). `linkedBranches` would need a GraphQL query per read and a ticket with no branch yet, which the shell and
  `session` would have to model.
- **The id is the number everywhere.** `TicketId` is an issue number; `session` links by it as `LinkedTicketId`, and a
  transcript's `/ticket start 42` or `#42` names it. A refresh keeps the selection on the same number, following it into its
  new group; one that is gone leaves the cursor on the next row of its old group, else the previous, else the header.

## Consequences

- A deleted or transferred issue that is not the newest updated one does not move the poll's answer, so the board shows it
  until another issue changes or `R` is pressed.
- deferred: the 1000 most recently created issues are read, so older ones, open or closed, show nowhere — close it when a
  project has that many.
- deferred: the ticket branch name is guessed, so a branch made under another name is not matched to its ticket — close it
  when the board reads `linkedBranches`.
- `TreeWatcher` and `Utf8Text` in the shared kernel have `docs` as their only user in `board`'s absence; moving them there
  is a follow-up.
