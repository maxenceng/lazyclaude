# GitHub Issues as a ticket tracker: what `gh` and the API can carry

Researched 2026-10-26 against `gh` 2.102.1, the live GraphQL schema (introspected with
`gh api graphql`), docs.github.com (REST version `2026-03-10` pages and the GraphQL reference) and
the cli.github.com manual. Read-only checks ran against `maxenceng/*` personal-account repos, plus
`cli/cli` as an organization-owned comparison. Nothing was created, edited or deleted.

The question: which parts of a ticket can GitHub Issues hold on a **personal account's** repos?
A ticket today carries frontmatter (`id`, `status`, `context`, `type`, `created`, `blocked_by`) and
markdown sections (acceptance criteria, Model decision, Design direction, and so on).

Each claim is tagged with its source. **[doc]** is a quote or close paraphrase of a primary
source listed at the end. **[schema]** was read from GraphQL introspection or `gh … --help`.
**[observed]** was seen in a read-only call. **[unverified]** has not been confirmed, because
confirming it would mean writing to GitHub.

The current token's scopes are `gist, read:org, repo`, with no `project` or `read:project`. That
matters for the Projects rows below.

## Summary

| Ticket part | Best home on a personal repo | Read via `gh` | Write via `gh` |
|---|---|---|---|
| title, body sections | issue title / body (markdown) | `gh issue view --json title,body` | `gh issue create/edit -t -F` |
| `id` | issue `number` (repo-scoped); a prefix only as a title convention or label | `number` | assigned by GitHub, not choosable |
| `status` (open/closed only) | issue `state` + `stateReason` | `state,stateReason` | `gh issue close -r completed\|"not planned"\|duplicate`, `reopen` |
| `status` (5 values) | labels, or a Projects v2 single-select (needs `project` scope) | `labels` / `gh project item-list` | `--add-label/--remove-label` / `gh project item-edit` |
| `type` | **labels**. Issue types cannot be set on personal repos | `labels` | `--add-label` |
| `context` | labels (or a milestone, which is single-valued) | `labels`, `milestone` | `--add-label`, `-m` |
| `created` | `createdAt`. Always the API call time, cannot be set | `createdAt` | no |
| `blocked_by` | native dependencies | `blockedBy,blocking` | `--blocked-by`, `--add-blocked-by` |
| map → decision tickets | sub-issues (`parent`, `subIssues`) | `parent,subIssues,subIssuesSummary` | `--parent`, `--add-sub-issue` |
| branch | linked branch (`gh issue develop`) | `gh issue develop --list`, GraphQL `linkedBranches` | `gh issue develop -n <name>` |
| PR | closing keyword or manual closing reference | `closedByPullRequestsReferences` | PR body `Closes #n`; GraphQL `addCloseIssueReferences` |

## 1. Metadata: what exists, and whether a personal repo gets it

### Issue types: not usable on a personal repo

- Docs describe issue types only as an organization feature ("Managing issue types in an
  organization"), with "up to 25 issue types" per org. [doc: issue types]
- `CreateIssueTypeInput.ownerId` is "The ID for the organization on which the issue type is
  created". [schema]
- GraphQL `repository(owner:"maxenceng",…).issueTypes` returns `null`. On `cli/cli` it returns
  `Task, Bug, Feature`. [observed]
- On an issue in a repo the viewer owns, `viewerCanType` is `false`. [observed] This is the
  deciding signal: the owner cannot set a type there.
- Inconsistency: REST `GET /repos/maxenceng/<repo>/issue-types` answers 200 with
  `Task, Bug, Feature` (ids `80085535…7`, `created_at 2026-08-20`). The ids are the same across
  every repo of the account. [observed] This endpoint is not in the public REST docs (the
  documented one is `GET /orgs/{org}/issue-types`, which needs `admin:org`). Whether
  `gh issue create --type Task` succeeds on a personal repo is **[unverified]**. Given
  `viewerCanType: false` and the null GraphQL field, expect it to fail. A design should not rely
  on it.
- `gh` support exists for org repos: `create --type`, `edit --type/--remove-type`,
  `list --type`, JSON field `issueType`. Mutation: `updateIssueIssueType`. [schema]

### Issue fields (org custom fields): not usable on a personal repo

- The schema has `Issue.issueFieldValues`, `Repository.issueFields` and mutations
  `createIssueField`, `setIssueFieldValue` and similar. `CreateIssueFieldInput.ownerId` is "The ID
  of the organization". [schema]
- On a personal repo, `issueFields` is `null`, `viewerCanSeeIssueFields` is `false`, and
  `viewerCanSetFields` is `false`. [observed] `gh` 2.102 has no flag for them.

### Labels and milestones: fully usable

- Labels: `gh label create/edit/list/delete/clone`. On issues: `--label`,
  `--add-label/--remove-label`, `list --label`. Labels are multi-valued and free-form, so a
  `status:` or `type:` prefix convention is only enforced by the client. [schema]
- Milestones: one per issue. `-m` on create/edit, `--remove-milestone`, `list --milestone`.
  `gh` has no milestone CRUD command, so creating one needs `gh api repos/{o}/{r}/milestones`.
  [schema] [unverified: not exercised]

### Sub-issues: usable, and they fit map → decision

- Up to 100 sub-issues per parent and eight levels of nesting. Sub-issues may come from other
  repositories. [doc: adding sub-issues]
- REST: `GET/POST /repos/{o}/{r}/issues/{n}/sub_issues`, `DELETE …/sub_issue`,
  `PATCH …/sub_issues/priority`, `GET …/parent`. The POST takes `sub_issue_id`, which is the
  issue's numeric **`id`, not its `number`**, and it "must belong to the same repository owner
  as the parent issue". It also takes `replace_parent`. [doc: REST sub-issues] `GET …/parent`
  answers 404 "No parent issue found" when there is none. [observed]
- GraphQL: `Issue.parent`, `subIssues`, `subIssuesSummary { total completed percentCompleted }`.
  Mutations: `addSubIssue`, `removeSubIssue`, `reprioritizeSubIssue`. `CreateIssueInput` has
  `parentIssueId`. [schema]
- `gh`: `create --parent`, `edit --parent/--remove-parent/--add-sub-issue/--remove-sub-issue`,
  JSON fields `parent, subIssues, subIssuesSummary`. These read back on a personal repo.
  [schema; observed]

### Native dependencies (blocked by / blocking): usable

- REST: `GET/POST /repos/{o}/{r}/issues/{n}/dependencies/blocked_by`,
  `DELETE …/blocked_by/{issue_id}`, `GET …/dependencies/blocking`. The POST takes `issue_id`,
  again the numeric id. Add and remove "may trigger secondary rate limiting". [doc: REST issue
  dependencies] Both GETs answer 200 `[]` on a personal repo. [observed]
- GraphQL: `Issue.blockedBy`, `blocking`, `issueDependenciesSummary { blockedBy blocking
  totalBlockedBy totalBlocking }`. Mutations: `addBlockedBy(issueId, blockingIssueId)`,
  `removeBlockedBy`. There is also an undirected `relatesTo` with `addRelatesTo/removeRelatesTo`.
  [schema]
- `gh`: `create --blocked-by/--blocking`, `edit --add-blocked-by/--remove-blocked-by/
  --add-blocking/--remove-blocking` (numbers or URLs), JSON `blockedBy, blocking`. [schema]
  Read back on a personal repo. [observed]
- The docs give no maximum count and do not say whether dependencies can cross repositories.
  The CLI example in them shows `octo-org/octo-repo#200`. [doc: creating issue dependencies]
  Cross-repo dependencies: **[unverified]**.

### Projects v2 (single-select Status and other fields): usable, at a cost

- A personal account can own a project ("at the user or organization level"). Field types are
  text, number, date, single select and iteration. Up to 50 fields and 50,000 items, archive
  included. [doc: about projects; adding items]
- New projects start with two workflows enabled: closed issue/PR → Status `Done`, merged PR →
  `Done`. [doc: built-in automations]
- **Scope:** every Projects v2 read needs `read:project`. With the current token,
  `gh issue list --json projectItems` fails with "The 'id' field requires one of the following
  scopes: ['read:project']". [observed] `gh project` says "The minimum required scope for the
  token is: `project`", added by `gh auth refresh -s project`. [schema: gh project --help]
- `gh project item-edit` sets a value by `--field <name> --value <v>`, or by `--field-id` plus
  `--single-select-option-id` / `--text` / `--number` / `--date` / `--iteration-id`, or clears
  it with `--clear`. [schema] GraphQL: `updateProjectV2ItemFieldValue`,
  `clearProjectV2ItemFieldValue`. [schema]
- A project is a second object beside the issue. A ticket's status there is not on the issue
  itself, and issue reads need extra queries (`projectItems { fieldValues … }`).
- Projects v2 webhooks (`projects_v2_item`) are available to **organizations only**. [doc:
  webhook events] A personal project's field changes emit no webhook.

## 2. Branch and pull request link

- `gh issue develop <n> [-n name] [-b base] [-c] [--worktree path]` creates a branch and links
  it. `gh issue develop --list <n>` reads the links back. [schema] The GraphQL mutation behind it
  is `createLinkedBranch(issueId, oid, name, repositoryId)`. It *creates* a branch at a commit
  `oid`, so linking an already-pushed branch is not what it does. [schema] Whether it accepts an
  existing branch name is **[unverified]**. The default branch name is "issue number and title".
  [schema] So a `<CONTEXT>-n-slug` name has to be passed with `-n`.
- Docs: creating a branch from an issue is "in public preview and subject to change". "When you
  create a pull request for one of these branches, it is automatically linked to the issue",
  and the branch link is then replaced by the PR in the Development sidebar. [doc: creating a
  branch for an issue]
- Read back: `Issue.linkedBranches { ref { name } }` (empty on the issues checked [observed]),
  `Issue.closedByPullRequestsReferences(includeClosedPrs, userLinkedOnly, …)`, and from the PR
  side `PullRequest.closingIssuesReferences`. [schema] `gh issue view --json
  closedByPullRequestsReferences`. [schema]
- Closing keywords: close/closes/closed, fix/fixes/fixed, resolve/resolves/resolved, in the PR
  description or a commit message. "The pull request must be on the default branch", otherwise
  the keyword is ignored. Cross-repo form: `Fixes owner/repo#100`. [doc: linking a PR]
- Manual link: write permission, up to ten issues per PR. [doc: linking a PR] Over the API:
  `addCloseIssueReferences(issueId, pullRequestIds)` ("Limit: 10") and
  `removeCloseIssueReferences`. The latter cannot remove a link made by a body keyword:
  "Auto-detected closing references from pull request body keywords are not" removed. [schema]
- `gh pr create` has no issue-link flag. The link comes from a closing keyword in the body, or
  from opening the PR on a branch `gh issue develop` created (per the doc above). [schema; doc]
  Whether that automatic link also happens for a PR opened with `gh pr create` and no keyword
  is **[unverified]**.

## 3. Detecting a remote change before writing

- **No compare-and-set on writes.** "Conditional requests for unsafe methods, such as `POST`,
  `PUT`, `PATCH`, and `DELETE` are not supported." [doc: REST best practices]
  `UpdateIssueInput` has no expected-version or `ifUpdatedAt` argument. [schema] GitHub
  therefore cannot refuse a stale write. Last write wins.
- What is available is detection: read `updatedAt` (GraphQL) or `updated_at` (REST) just before
  writing, compare it with the value read when editing started, and refuse on a mismatch. A race
  window between the check and the write remains. Body history is readable through
  `Issue.lastEditedAt`, `editor` and `userContentEdits`. [schema]
- Which changes bump `updatedAt` (labels? dependencies? sub-issues? project fields?) is
  **[unverified]**, because testing it needs a write. Project field values live on the
  `ProjectV2Item`, which has its own `updatedAt`. Assume an issue's `updatedAt` does not cover
  them.
- ETags exist on REST GETs as weak validators (`Etag: W/"…"`). Sending `If-None-Match` returns
  `304 Not Modified`. Observed both for a single issue and for `GET /issues?state=all`.
  [observed] They serve as a cheap "has anything changed" check for polling, not as a write
  guard.
- `Last-Modified` on an issue is **not** its `updated_at`. One issue last updated
  `2020-09-11T09:45:03Z` was served with `Last-Modified: Mon, 05 Oct 2026`. [observed] Use
  `If-None-Match`, not `If-Modified-Since`.
- GraphQL responses carry no `ETag` or `Last-Modified`. [observed] Conditional polling is
  REST-only.
- `gh issue edit` does not compare before writing. It has no such flag. [schema]

## 4. Rate limits and change notification without a server

### Limits

- REST primary limit: 5,000 requests/hour for an authenticated user. A `GITHUB_TOKEN` in
  Actions gets 1,000/hour/repo. [doc: REST rate limits] Observed `X-Ratelimit-Limit: 5000`,
  `Resource: core`. [observed]
- GraphQL primary limit: 5,000 **points**/hour/user. Cost is about connections requested ÷ 100,
  with a minimum of 1. `first`/`last` must be between 1 and 100, and one call may request at
  most 500,000 nodes. [doc: GraphQL limits] Observed: 100 issues with body, labels(20),
  blockedBy(20), subIssues(50) and parent cost **3 points**. [observed]
- Secondary limits: 100 concurrent requests, shared by REST and GraphQL. 900 points/minute for
  REST and 2,000 for GraphQL. 90 s CPU per 60 s. 80 content-creating requests/minute and 500/hour.
  [doc: REST and GraphQL limits] A GraphQL mutation costs 5 secondary points, against 1 for a
  query. [doc: GraphQL limits] The docs advise serial rather than concurrent requests, and at
  least one second between mutating requests. [doc: REST best practices]
- A `304` "does not count against your primary rate limit" when authorized. [doc: REST best
  practices] Observed: two consecutive 304s left `core.used` unchanged, measured with
  `GET /rate_limit`. [observed] The token is shared with other clients, so the counter moves on
  its own.
- For scale: polling one repo's issue list every 60 s with `If-None-Match` costs about nothing
  while nothing changes. A changed list costs one full GraphQL read of about 3 points.

### Change notification

- Docs prefer webhooks to polling. When polling, use a fixed schedule and honour `x-poll-interval`.
  [doc: REST best practices] `X-Poll-Interval: 60` is served on `GET /repos/{o}/{r}/events` and
  on `GET /notifications`. [observed]
- `GET /repos/{o}/{r}/issues?since=<ISO time>&state=all` returns only issues updated since then.
  It **includes pull requests**, and on this repo it returned PR numbers. [observed] GraphQL
  `issues(orderBy:{field:UPDATED_AT,direction:DESC})` and `timelineItems(since:)` are the GraphQL
  equivalents. [schema]
- Timeline event types include `BLOCKED_BY_ADDED_EVENT`, `SUB_ISSUE_ADDED_EVENT`,
  `PARENT_ISSUE_ADDED_EVENT`, `ISSUE_TYPE_CHANGED_EVENT`, `LABELED_EVENT`,
  `CONNECTED_EVENT`, `CROSS_REFERENCED_EVENT`, `PROJECT_V2_ITEM_STATUS_CHANGED_EVENT` and others.
  [schema]
- Webhooks need a reachable URL. The only server-less option GitHub documents is
  `gh webhook forward` (extension `cli/gh-webhook`, not installed here [observed]). It is
  "not supported for use in production" and allows "Only one person … at a time for each
  repository". [doc: forwarding webhooks] It does not fit a client that is always on.
- The webhook events that exist are `issues`, `issue_dependencies` (`blocked_by_added/removed`,
  `blocking_added/removed`) and `sub_issues` (`parent_issue_added/removed`,
  `sub_issue_added/removed`), for repository, organization and app. `projects_v2_item` is for
  organizations only. [doc: webhook events]
- GitHub Actions `issues` triggers (`opened, edited, … labeled, typed, field_added, …`) run on
  GitHub's infrastructure with no server of one's own. [doc: workflow events] They can react on
  GitHub's side, for example by enforcing a label rule. They cannot push to a local TUI.

## 5. Importing many issues while keeping dates and history

- **Normal API: dates are not kept.** `CreateIssueInput` has no `createdAt` or `closedAt`.
  [schema] REST create is the same. Every imported issue is dated at the import, and its
  timeline starts empty. The original date can only survive as text in the body, for example a
  `created:` line.
- **Issue Import API** (`POST /repos/{o}/{r}/import/issues`, header
  `Accept: application/vnd.github.golden-comet-preview+json`) accepts `created_at`,
  `closed_at`, `updated_at`, `closed`, `labels`, `milestone` and `assignee`, plus `comments[]` each
  with its own `created_at`. It runs asynchronously, a status is read at `…/import/issues/{id}`,
  and it needs admin on the repo. [doc: the gist below, written by a GitHub staff member] It is
  not on docs.github.com. The gist says "This API hasn't been finalized yet", and the author
  later commented that it "was never prioritized to be maintained" and may be removed.
  - Observed: `GET /repos/maxenceng/term/import/issues/1` returns 404 with
    `documentation_url` pointing at that gist. The route is still served and still documented
    only by the gist. [observed] Whether a POST still works today is **[unverified]**.
  - Even through it, sub-issue links, dependencies, labels' event history and git history are
    not imported. Relationships have to be added afterwards with the normal API (secondary rate
    limits apply). [unverified for dependencies: the import schema has no field for them]
- Git history already holds a markdown ticket's past. Whatever the import keeps, the
  pre-migration history stays in git, not in the issue timeline.
- Throughput: 80 content-creating requests/minute and 500/hour [doc: REST rate limits], and at
  least 1 s between writes [doc: best practices]. A backlog of about 40 tickets, each with a
  create plus a few relationship writes, fits comfortably inside one hour.

## Gaps and unverified claims, collected

- Whether `--type` works on a personal repo. The evidence says no (`viewerCanType: false`), but
  REST lists types.
- Which edits bump `Issue.updatedAt`.
- Whether dependencies may cross repositories, and their maximum count.
- Whether `createLinkedBranch` / `gh issue develop` can link an existing branch.
- Whether the Issue Import API still accepts POSTs.

## Sources

- Issue types (org): https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/managing-issue-types-in-an-organization
- Sub-issues: https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/adding-sub-issues
- Issue dependencies: https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-issue-dependencies
- REST sub-issues: https://docs.github.com/en/rest/issues/sub-issues
- REST issue dependencies: https://docs.github.com/en/rest/issues/issue-dependencies
- About projects: https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects
- Project item limit: https://docs.github.com/en/issues/planning-and-tracking-with-projects/managing-items-in-your-project/adding-items-to-your-project
- Project built-in automations: https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations
- Linking a PR to an issue: https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue
- Creating a branch for an issue: https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-a-branch-for-an-issue
- REST best practices (conditional requests, polling, pacing): https://docs.github.com/en/rest/using-the-rest-api/best-practices-for-using-the-rest-api
- REST rate limits: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- GraphQL rate and query limits: https://docs.github.com/en/graphql/overview/rate-limits-and-query-limits-for-the-graphql-api
- Webhook events and payloads: https://docs.github.com/en/webhooks/webhook-events-and-payloads
- Forwarding webhooks with gh: https://docs.github.com/en/webhooks/testing-and-troubleshooting-webhooks/using-the-github-cli-to-forward-webhooks-for-testing
- Workflow trigger events: https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows
- Issue Import API (unofficial, GitHub staff gist): https://gist.github.com/jonmagic/5282384165e0f86ef105
- GraphQL schema: introspected live with `gh api graphql` (`Issue`, `Mutation`, `CreateIssueInput`,
  `UpdateIssueInput`, `CreateIssueTypeInput`, `CreateIssueFieldInput`, `CreateLinkedBranchInput`,
  `AddCloseIssueReferencesInput`, `IssueTimelineItemsItemType`)
- gh manual / `--help` 2.102.1: `gh issue create|edit|list|view|develop|close`, `gh project`,
  `gh project item-edit`, `gh api`, `gh label`
