# 118. A ticket's branch is the branch GitHub links to its issue

Date: 2026-11-19

## Status

Accepted. Supersedes the ticket branch bullet of ADR 0065 ("derived, not read back"), its deferred consequence about it, and the
`gh issue list` call that bullet's board read made; the rest of ADR 0065 stands. Builds on ADR 0052.

## Context

ADR 0065 derived a ticket's branch as `<number>-<title slug>`, "as `gh issue develop` names it by default". The slug turns every
non-alphanumeric run into a dash, so "session's" becomes `session-s`; GitHub drops the apostrophe, and `gh issue develop` links
`sessions`. The board then looked for the slug's branch while a worktree stood on the linked one, the session launch found the
worktree folder taken and refused with "exists but is not a worktree of", and the pull request glyph, the merge state and the
worktree removal all missed the ticket's real branch. Many closed issues have no linked branch at all, so the slug cannot be
dropped.

## Decision

- **The branch is the issue's first linked branch, else the slug.** `GitHubIssue`'s `TicketBranch` is the first ref of
  `linkedBranches(first: 1)` when one is linked; with none, `<number>-<title slug>` as before, and `<number>` when the title
  slugs to nothing. An issue node with no `linkedBranches` field reads as nothing linked, not as unreadable. With two or more,
  the first GitHub lists. `session`, `review` and the shell keep reading `ticket.branch()` and change nothing about how they ask.
- **One `gh api graphql --paginate --slurp` call replaces `gh issue list`.** It asks for the same fields (number, title, body,
  labels, state, stateReason, closedAt) plus `linkedBranches`, 100 issues a page, open and closed, newest created first, with
  `-F owner={owner} -F name={repo}` so `gh` finds the remote itself. `--slurp` wraps the pages in one array, so the output is
  one JSON document. `state` and `stateReason` are the same enum words `gh issue list --json` printed (`CLOSED`, `COMPLETED`,
  `NOT_PLANNED`; null where an open issue has none), so the mapping to a ticket is unchanged. Failures still go through
  `GhFailure`, which reads what `gh` said, not which subcommand said it.
- **The 1000 cap is the reader's.** `gh` has no limit on `--paginate`, so it fetches every page and the reader keeps the first
  1000, the newest created, which is what `--limit 1000` kept.
- **The ETag poll is unchanged.** `IssuesWatchRepository` still checks the newest updated issue (ADR 0065); only the full
  read behind it changes.
- Rejected: keeping `gh issue list` and a second call per ticket for its linked branch, which is one request per issue; a
  slug that drops apostrophes, which matches GitHub today and breaks on the next rule it has that the slug does not.

## Consequences

- deferred: only the 1000 newest issues are shown, though `gh` still fetches every page — close it when a project has that
  many.
- A branch linked after the board was read reaches it with the next read, which the poll triggers, since linking a branch
  updates the issue.
- A ticket whose branch has been deleted still names it: GitHub keeps no ref for it, so the ref is read as nothing linked and
  the slug stands.
