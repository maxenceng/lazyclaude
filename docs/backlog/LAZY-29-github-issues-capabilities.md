---
id: LAZY-29
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# What can GitHub Issues and `gh` carry of a ticket?

## Question

What can GitHub Issues and `gh` carry of a ticket?

- issue types, labels, milestones, sub-issues, native issue dependencies (blocked by / blocking), Projects v2 single-select fields — which are readable and writable from `gh` and from GraphQL, on a personal account's repos;
- linking an issue to a branch and a pull request (`gh issue develop`, closing keywords, the development sidebar), and reading that link back;
- detecting a remote change before writing (`updatedAt`, ETags, conditional requests) so nothing is silently overwritten;
- rate limits for a polling client, and webhooks or events usable without a server;
- importing many issues with their dates and history kept, or not.

## Kind

research

## Resolution

On a personal account's repo, an issue can hold a ticket's title, body and open/closed state,
`blocked_by` (native dependencies: `gh issue create/edit --blocked-by`), and map → decision
(sub-issues: up to 100 per parent, 8 levels deep). Issue types and issue fields exist only for
organizations, so `type`, `context` and a five-value `status` must be labels. A Projects v2
single-select could hold `status`, but it needs the `project` scope, which the token lacks
today, and gives no webhooks on personal projects. `gh issue develop -n` links a branch, and a
closing keyword links a PR; both can be read back (`linkedBranches`,
`closedByPullRequestsReferences`). No write is conditional, so a client can only compare
`updatedAt` right before it writes. ETag polling returns 304s that cost no rate limit, there is
no supported push without a server, and `createdAt` cannot be backdated. The REST
sub-issue/dependency endpoints take the numeric `id`, not the issue number. Details and the
unverified points: [`docs/research/github-issues-capabilities.md`](../research/github-issues-capabilities.md).
