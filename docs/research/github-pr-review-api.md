# GitHub PR review: the `gh` and API calls, and their rules

Researched 2026-10-09 against `gh` 2.102.0, docs.github.com (REST "2022-11-28" reference and
GraphQL reference) and the cli.github.com manual. Read-only checks were run with `gh` against
`maxenceng/next-suggestions`. Nothing was created, commented on or submitted.

Each claim is tagged with its source: **[doc]** is a quote or close paraphrase of a primary
source linked below, **[observed]** was seen in a read-only `gh` call, and **[unverified]** is
widely reported but not stated in the docs and not tested here, because testing it would mean
writing to GitHub.

## Summary

| Need | Call |
|---|---|
| Open PR for a branch | `gh pr list --head <branch> --state open --json number,url,headRefOid` |
| Unified diff | `gh pr diff <n>` (or REST `GET /pulls/{n}` with `Accept: application/vnd.github.diff`) |
| Per-file patches | `gh api --paginate repos/{o}/{r}/pulls/{n}/files` → `.[].patch` |
| Threads + resolved state | `gh api graphql`: `pullRequest.reviewThreads { isResolved isOutdated path line … }` |
| Submit line comments + summary as a Comment review | one REST call: `POST /pulls/{n}/reviews` with `event=COMMENT`, `body`, `comments[]` using `line`/`side` |
| Merged? | `gh pr view <n> --json state,mergedAt` (`state` is `OPEN`/`CLOSED`/`MERGED`) |

## 1. Find the open PR for a branch

- `gh pr list --head <branch> --state open` filters by head branch. The `<owner>:<branch>`
  syntax is **not supported** there. `--state` accepts `open|closed|merged|all`. [doc: gh pr list]
  Observed: `gh pr list -R maxenceng/next-suggestions --head SYNC-1-close --state all --json number,state`
  returns `[{"number":119,"state":"MERGED"}]`. [observed]
- `gh pr view [<number> | <url> | <branch>]` also accepts a branch. With no argument it uses
  "the pull request that belongs to the current branch". [doc: gh pr view] Because it does not
  filter by state, it may find a closed or merged PR for that branch. Use `pr list --state open`
  when "open" is what you need.
- REST equivalent: `GET /repos/{owner}/{repo}/pulls?head=<owner>:<branch>&state=open`. `head`
  is "in the format of user:ref-name or organization:ref-name", and `state` is
  `open|closed|all` (default `open`). [doc: REST pulls] Observed: it works with
  `head=maxenceng:SYNC-1-close&state=all`. [observed]
- GraphQL `Ref.associatedPullRequests` only works while the branch still exists. A deleted
  branch returns `ref: null`. [observed] Do not rely on it after a merge.
- `gh pr view --json` fields that matter here are `number, url, state, mergedAt, headRefName,
  headRefOid, baseRefName, isDraft, author, reviewDecision, files, reviews, latestReviews`.
  **There is no `reviewThreads` field.** [doc: gh pr view]

## 2. Fetch the diff

- `gh pr diff [<number>|<url>|<branch>]` prints the unified diff. Flags: `--patch` (patch
  format), `--name-only`, `--color always|never|auto`, `--web`. [doc: gh pr diff] Pass
  `--color never` when you parse the output.
- REST: `GET /repos/{o}/{r}/pulls/{n}` with `Accept: application/vnd.github.diff` returns the
  raw unified diff (`vnd.github.patch` returns patch format). [doc: REST pulls] Observed with
  `gh api … -H 'Accept: application/vnd.github.diff'`. [observed]
- Per-file patches: `GET /repos/{o}/{r}/pulls/{n}/files` gives each file's `filename`,
  `status`, `additions`, `deletions` and `patch` (the hunks only, starting at `@@`, with no
  file header). `per_page` can be at most 100, so use `gh api --paginate`, and "Responses
  include a maximum of 3000 files". [doc: REST pulls; observed] `patch` is optional and is
  missing for binary files and very large diffs.
- Limits: in a pull request, "no total diff may exceed 20,000 lines … or 1 MB of raw diff
  data"; one file's diff may not exceed 20,000 lines or 500 KB; "the maximum number of files in
  a single diff is limited to 300". [doc: repository limits] Beyond these limits the whole-PR
  `.diff` request reportedly fails with HTTP 406 ("diff too large"). [unverified] The `/files`
  endpoint is the fallback.

## 3. Existing review threads, with resolved state

Only GraphQL exposes threads and their resolution. The REST review comments
(`GET /pulls/{n}/comments`) have no resolved flag.

```graphql
query($owner:String!, $repo:String!, $number:Int!, $after:String) {
  repository(owner:$owner, name:$repo) {
    pullRequest(number:$number) {
      state merged mergedAt headRefOid
      reviewThreads(first:100, after:$after) {
        totalCount
        pageInfo { hasNextPage endCursor }
        nodes {
          id isResolved isOutdated isCollapsed viewerCanResolve
          path line startLine originalLine diffSide startDiffSide subjectType
          comments(first:50) { nodes { author { login } body createdAt url } }
        }
      }
    }
  }
}
```

Run it with `gh api graphql -f query=@q.graphql -F owner=… -F repo=… -F number=…`. Add
`--paginate` and `$endCursor` when there are more than 100 threads.

- Fields [doc: GraphQL pulls]: `isResolved: Boolean!`, `isOutdated: Boolean!`,
  `path: String!`, `line: Int` (end line), `startLine: Int`, `originalLine: Int`,
  `diffSide: DiffSide!` (`LEFT|RIGHT`), `startDiffSide`, `subjectType` (`LINE|FILE`),
  `viewerCanResolve`, `resolvedBy`.
- **An outdated thread has `line: null`.** Only `originalLine` survives. [observed on PR #98]
  An outdated thread cannot be anchored in the current diff, so list it separately.
- Who can resolve: "if you opened the pull request" or "if you have write access".
  [doc: commenting on a PR] Observed: `viewerCanResolve: false` on a merged PR authored by the
  viewer, so it is false after merge. [observed] The mutation is `resolveReviewThread(threadId)`.
  [doc: GraphQL pulls] lazyclaude v1 does not need it.

## 4. Create a review with line comments and submit it as COMMENT

### Single call (recommended)

`POST /repos/{o}/{r}/pulls/{n}/reviews` [doc: REST reviews]

```json
{
  "commit_id": "<headRefOid>",
  "event": "COMMENT",
  "body": "summary text",
  "comments": [
    { "path": "src/A.java", "line": 42, "side": "RIGHT", "body": "…" },
    { "path": "src/B.java", "start_line": 10, "start_side": "RIGHT",
      "line": 14, "side": "RIGHT", "body": "multi-line…" }
  ]
}
```

Send it with `gh api -X POST repos/{o}/{r}/pulls/{n}/reviews --input review.json`. The
`comments` array is an array of objects, so `-f` flags cannot build it. Use `--input`.

- `event` is one of `APPROVE | REQUEST_CHANGES | COMMENT`. "By leaving this blank, you set the
  review action state to PENDING." `body` is "Required when using REQUEST_CHANGES or COMMENT".
  [doc: REST reviews]
- `comments[]` takes `path` (required), `body` (required), `line`, `side`, `start_line`,
  `start_side`, and the legacy `position`. [doc: REST reviews]
- Set `commit_id` to the PR's current head SHA. "Not using the latest commit SHA may render
  your comment outdated." [doc: REST review comments] If it is omitted, the API uses the most
  recent commit.
- If any comment is invalid, the whole request is rejected with a 422 and nothing is posted.
  [unverified; widely reported] The client should therefore validate every anchor against the
  hunks before sending (see §5).

### Pending, then submit (two calls)

1. `POST …/pulls/{n}/reviews` **without** `event` creates a PENDING review. "Pull request
   reviews created in the PENDING state are not submitted." [doc: REST reviews]
2. `POST …/pulls/{n}/reviews/{review_id}/events` with `{"event":"COMMENT","body":"…"}`.
   Here `event` is required, and leaving it blank returns a 422. [doc: REST reviews]

GraphQL equivalents [doc: GraphQL pulls]:

- `addPullRequestReview(input:{pullRequestId, body, event, threads:[DraftPullRequestReviewThread]})`.
  `threads` replaces the deprecated `comments`. A thread takes `path, line, side, startLine,
  startSide, body`.
- `addPullRequestReviewThread` adds a thread to a pending review.
- `submitPullRequestReview(input:{pullRequestReviewId | pullRequestId, event, body})`.

A user can hold only one pending review per PR. If one is left over from the web UI, creating
another fails. [unverified] For a TUI that composes everything locally, the single call is
simpler: there is no server-side draft to clean up.

`gh pr review --comment -b …` posts only a body. It has no line-comment flags. [doc: gh pr review]
Line comments therefore need `gh api`.

## 5. Line-comment coordinates and what is rejected

- `position` "is closing down. Use `line` instead." It counts lines below the first `@@` of the
  file's diff ("The line just below the "@@" line is position 1"). [doc: REST review comments]
- `line` is "The line of the blob in the pull request diff that the comment applies to". This
  is a **file line number**, not a diff offset. For a multi-line comment it is the last line.
  [doc: REST review comments]
- `side` is `LEFT | RIGHT`: "Use LEFT for deletions … RIGHT for additions or unchanged context
  lines". For a multi-line comment it is the side of the last line. [doc: REST review comments]
  - RIGHT + `line` → the line number in the new file (the `+c` counter of `@@ -a,b +c,d @@`).
  - LEFT + `line` → the line number in the old file (the `-a` counter).
- `start_line` and `start_side` are "Required when using multi-line comments", and
  `start_line` must be less than `line`. [doc; the ordering is unverified]
- `subject_type: "file"` comments on the whole file, with no `line`. This is for the single
  comment endpoint `POST /pulls/{n}/comments`. [doc: REST review comments]
- **Rejected:** a `line`/`side` that is not inside one of that file's diff hunks (added,
  removed or context lines shown in the patch). The API answers 422 (`pull_request_review_thread.line
  must be part of the diff` / `… could not be resolved`). [unverified: the docs only say "in the
  pull request diff"; the error text is third-party reported] Also rejected: a `path` the PR does
  not change. Rule for lazyclaude: offer a comment only on lines inside a hunk, using the
  counters from parsing `@@ -a,b +c,d @@`. `+` and context lines go RIGHT with the new-file
  number, and `-` lines go LEFT with the old-file number.

## 6. Author restrictions

- "Pull request authors cannot approve their own pull requests." [doc: reviewing proposed
  changes; approving with required reviews] **Confirmed.**
- Request changes on your own PR is **not stated in the docs**. It is widely reported to
  return a 422 ("Can not request changes on your own pull request"), and the web UI disables
  it for the author. [unverified, not tested because testing would mean writing] COMMENT on your
  own PR is allowed. The PRs in this account are all authored by the viewer (`author.login`
  equals the viewer). [observed] So a COMMENT review is the only verb lazyclaude offers.

## 7. Signals for the next-verb rule

- Merged: `gh pr view <n> --json state,mergedAt`. `state` is `OPEN | CLOSED | MERGED` (the
  GraphQL `PullRequestState`). [doc: GraphQL pulls; observed `"state":"MERGED"`] In REST,
  `state` is only `open|closed`, plus `merged: bool` and `merged_at`. `GET /pulls/{n}/merge`
  answers 204 when merged and 404 when not. [doc: REST pulls; observed 204]
- Unresolved threads: run the §3 query and count nodes with `isResolved == false`. You may
  want to split out `isOutdated`. One GraphQL call returns both `state`/`merged` and the
  threads.
- `reviewDecision` is `APPROVED | CHANGES_REQUESTED | REVIEW_REQUIRED | null`. It was null or
  empty on these self-authored PRs [observed], so it is not a useful signal here.

## Sources

- REST pull requests: https://docs.github.com/en/rest/pulls/pulls
- REST pull request reviews: https://docs.github.com/en/rest/pulls/reviews
- REST pull request review comments: https://docs.github.com/en/rest/pulls/comments
- GraphQL pulls reference (PullRequest, PullRequestReviewThread, mutations): https://docs.github.com/en/graphql/reference/pulls
- Repository diff limits: https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits
- Reviewing proposed changes (pending reviews, author cannot approve): https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/reviewing-proposed-changes-in-a-pull-request
- Approving with required reviews: https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/approving-a-pull-request-with-required-reviews
- Commenting on a PR (who can resolve): https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/commenting-on-a-pull-request
- gh manual: https://cli.github.com/manual/gh_pr_list, https://cli.github.com/manual/gh_pr_view,
  https://cli.github.com/manual/gh_pr_diff, https://cli.github.com/manual/gh_pr_review
