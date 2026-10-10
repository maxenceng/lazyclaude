---
id: LAZY-6
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1]
---

# Which `gh` and GitHub API calls does the PR review need, and what are their rules?

## Question

Which `gh` and GitHub API calls does the PR review need, and what are their rules?

Find the open PR for a branch; fetch its unified diff; list existing review threads and whether each is resolved; create a pending review with line comments and submit it as `COMMENT` with a body. Which coordinates does a line comment take (`line`/`side`/`start_line` vs the deprecated `position`), and what is rejected (a line outside the diff hunks)? Also: is the PR merged, and does it have unresolved threads — the signals the next-verb rule needs.

## Kind

research

## Resolution

The `gh` CLI covers lookup and diff. Threads and line comments need `gh api`. Details and
sources are in [docs/research/github-pr-review-api.md](../research/github-pr-review-api.md).

- **PR for a branch:** `gh pr list --head <branch> --state open` (a bare branch name;
  `owner:branch` is not supported).
- **Diff:** `gh pr diff <n> --color never`. Per-file patches come from
  `GET /pulls/{n}/files` (paginated, at most 3000 files). GitHub caps a PR diff at 300 files,
  20k lines and 1 MB.
- **Threads:** GraphQL only. Use `pullRequest.reviewThreads { isResolved isOutdated path line
  startLine diffSide comments }`. An outdated thread has `line: null`.
- **Submit:** one REST call, `POST /pulls/{n}/reviews` with `commit_id` = head SHA,
  `event: COMMENT`, `body`, and `comments[]` of `{path, line, side[, start_line,
  start_side], body}`, sent with `--input`. Pending-then-submit (`…/reviews/{id}/events`) also
  works, but it leaves a server-side draft. `gh pr review` has no line comments.
- **Coordinates:** `line` is a file line number. `side` is RIGHT (new file, for `+` and context
  lines) or LEFT (old file, for `-` lines). `position` is closing down. A line outside the
  file's hunks gets a 422 that rejects the whole review, so offer comments only on lines inside
  a hunk.
- **Author:** the docs confirm an author cannot Approve their own PR. Request changes on your
  own PR is refused in practice but not documented. So the review is always COMMENT.
- **Next verb:** `gh pr view --json state` gives `MERGED`. Unresolved means a count of
  `isResolved == false` threads. One GraphQL query returns both.
