# 108. The issues poll checks every ten seconds

Date: 2026-11-14

## Status

Accepted. Amends ADR 0065: the issues poll's interval is 10 s, not 30 s.

## Context

ADR 0065 has the board's poll ask GitHub every 30 s for the most recently updated issue, conditionally on the last
answer's `ETag`. A `status:` label moved by `/ticket` in a session took up to half a minute to show on the board, which made
the window feel behind the work it was showing.

## Decision

- **The poll checks every 10 s**, declared once as `IssuesWatchRepository.INTERVAL`. A 304 costs no rate limit, so an
  unchanged project costs one `gh api` spawn every 10 s per frontend process and no `gh issue list`. Everything else ADR 0065
  decides stays: the conditional request, the first check only taking the `ETag`, a failed check counting as a change, and a
  fixed delay after each check, so a slow `gh` (up to its 30 s patience) delays the next check rather than overlapping it.
- **A failing `gh` keeps the same cadence.** Each failed check still reads the board, which fails the same way and keeps its
  warning; the first good check afterwards catches up within one interval. Rejected: a backoff while `gh` fails, which would
  delay that catch-up for a failure the user is likely fixing right now (`gh auth login`, the network coming back).
- Rejected: a re-read on window or terminal focus, which ADR 0054 already turned down, and a board refresh on the desktop,
  which is a separate ask.

## Consequences

- Three times as many `gh api` processes while the app is open: about 360 an hour per frontend process, each a 304.
- While `gh` fails, the board is read and fails every 10 s, three times as often as before.
