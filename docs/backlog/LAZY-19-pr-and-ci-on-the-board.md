---
id: LAZY-19
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-6, LAZY-10]
---

# Does the board show each ticket's PR and CI status in v1?

## Question

Does the board show each ticket's pull request and CI status in v1, and if so, what, and fetched how?

The destination puts PR review behind the ticket (ticket → branch → open PR), not on the board. Showing PR state (open, draft, merged) and CI (pending, green, red) on every board row means a `gh` call per ticket branch, or one `gh pr list` with the right fields, plus refreshing it. Is this inside v1, or out of scope? If inside, what does the row show, and does it need `gh pr checks`?

## Kind

grilling

## Resolution

Grilled 2026-10-10. Facts first (gh 2.102.0, on next-suggestions): `gh pr list --json`
carries `headRefName`, `isDraft` and `statusCheckRollup` (each check run's
`status`/`conclusion`, each legacy status's `state`), so one call covers the whole board and
`gh pr checks` is never needed. Open PRs with checks: ~0.5 s. 100 PRs of any state: 0.56 s
without checks, 3.4 s with them.

- **In v1.** Board rows show each ticket's PR state and CI result. The ticket detail (main
  view) shows the PR's number, title, state and one line per check (name plus result), under
  the frontmatter.
- **Fetch:** one background `gh pr list --state open --json
  number,title,headRefName,isDraft,statusCheckRollup` call. No per-ticket call. How often it
  reruns is LAZY-21's decision. LAZY-9's merged check for `in-review` tickets is unchanged.
- **Which rows:** any ticket whose ticket branch has an open PR, draft included, whatever
  its status. In practice that means `in-progress` and `in-review`. The match is
  `headRefName` == the ticket branch exactly as the board resolves it, never an id prefix.
  Merged PRs get no glyph, because LAZY-9's preselected `done` already says so.
- **CI result:** failed if any check is `FAILURE`, `TIMED_OUT`, `CANCELLED`,
  `ACTION_REQUIRED`, `STARTUP_FAILURE` or `ERROR`. Otherwise pending if any is not
  `COMPLETED` (`QUEUED`, `IN_PROGRESS`, `PENDING`, `EXPECTED`). Otherwise passed (every
  check `SUCCESS`, `NEUTRAL` or `SKIPPED`). A PR with no checks has no CI result.
- **Glyphs:** a right-aligned column, separate from the session marker on the left, using
  shapes and never marker characters: `✓` green passed, `✗` red failed, `◌` yellow pending.
  A dim `⊙` marks a draft PR (shown alongside its CI glyph) or an open PR with no checks.
  The zoomed status-column view shows the same column. The CI result is not a marker: it
  stays out of the status line's strongest marker, `w`/`alt+w` and notifications.
- **GitHub unreadable** (`gh` missing, unauthenticated, offline, or a non-GitHub remote): no
  glyphs, nothing blocks, no popup. The ticket detail shows `PR: unknown (<reason>)`, as
  LAZY-9's picker does.
- **Ownership:** `review` owns checks and the CI result, and `shell` joins them onto the
  board row as it does the marker. `board` stays free of pull requests and no new edge
  appears (context map updated). New `review` glossary terms: *Checks*, *CI result*, *Draft
  pull request* (always written in full, because bare "draft" is the ticket status). No
  ADR: this is cheap to reverse and follows the existing join pattern.
