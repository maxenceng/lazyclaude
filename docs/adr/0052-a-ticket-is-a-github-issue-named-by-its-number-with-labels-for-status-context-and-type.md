# 52. A ticket is a GitHub issue, named by its number, with labels for status, context and type

Date: 2026-10-27

## Status

Accepted. Supersedes ADR 0028 and the status source decided in LAZY-9 and amended in LAZY-17. The board reads issues
since BOARD-8 (ADR 0065); `docs/backlog/` is no longer read.

## Context

The desktop evolution moves the backlog from `docs/backlog/*.md` to GitHub Issues, for the whole claude-tools harness.
A ticket file carries an id that is also its filename and its branch name, a frontmatter (`status`, `context`, `type`,
`created`, `blocked_by`), and a body of `##` sections. Its status is read from the ticket branch's copy, because
`/ticket` commits each status change on that branch, and ADR 0028 settles which copy wins.

On a personal account's repo, issue types and issue fields do not exist, a Projects v2 field needs the `project` scope
the token lacks and gives no webhooks, dependencies and sub-issues are native, and `createdAt` cannot be backdated
(LAZY-29).

## Decision

- **The issue number is the ticket's id.** GitHub allocates it, so it is unique and never races. Rejected: a
  `CONTEXT-n` kept in the title, which needs a per-context counter built by scanning titles, which GitHub never enforces;
  and both at once, which keeps that counter.
- **The ticket branch is the one `gh issue develop` creates**, `<number>-<title-slug>`, linked to the issue by GitHub and
  read back through `linkedBranches`. The ticket worktree's path follows from it.
- **Status is a label on an open issue, and `done` is closed as completed.** `status:draft`, `status:todo`,
  `status:in-progress`, `status:in-review`. A closed-as-completed issue is `done` whatever labels it still carries, and
  the merge's closing keyword (`Closes #42` in the pull request body) is what closes it. Rejected: a Projects v2 field,
  for the scope and the missing webhooks; and status derived from linked pull requests, which GitHub's UI would not show.
- **Status is the issue's alone.** No copy rides along on the ticket branch, so the branch copy, the root copy and the
  rule that a root `done` wins have nothing left to settle. The issue's timeline of label events is the history.
- **Context and type are labels too**, `context:<name>` (`context:cross-context` included) and
  `type:<feature|fix|chore|spike|map|decision>`. `blocked_by` is GitHub's native dependencies, open to every type. A map's
  decision tickets are its sub-issues.
- **Missing labels read as the earliest state.** An open issue with no status label is a `draft`; a `draft` may lack
  its context and type labels, and reads as unassigned. From `todo` on, a ticket without exactly one context and one
  type label is an unreadable ticket. So is any ticket with two status labels, and any issue closed as not planned —
  there is no dropped status.
- **The body keeps the ticket's `##` sections and has no frontmatter.** The issue title is the title and `createdAt` is
  the creation date; nothing an issue field or label holds is repeated in the body.
- **A project with no GitHub remote has no backlog.** The board says why; `docs/backlog/` is not read as a fallback.
- **The domain word stays *ticket*.** *Issue* names only the GitHub object that stores one, in the secondary adapter,
  as *PR* does for a pull request.

## Consequences

- "What state was this ticket in when we merged" is no longer answered by the repo's history, only by the issue's
  timeline. The one property kept is that a ticket is not `done` before its pull request merges.
- Capture works from anywhere that opens an issue — the GitHub UI, a phone — since a bare issue is a `draft`.
- A label left behind on a closed issue is `/ticket done`'s tidy-up; the board never reads it.
- Migrated tickets lose their `CONTEXT-n` names and their `created` dates; how ADRs and commits citing them still
  resolve is the migration's question (LAZY-33).
- `Ticket branch`, `Branch copy`, `Root copy` and `Status` in the glossary change meaning or go when the backlog read moves
  to Issues, in that change.
