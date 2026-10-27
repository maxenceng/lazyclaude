---
id: BOARD-9
status: todo
context: board
type: feature
tracker_id: 106
created: 2026-10-27
blocked_by: [BOARD-8, LAZY-42]
---

# `c` on the Board captures a draft ticket as a new GitHub issue

## Acceptance criteria

- [ ] `c` with the Board focused opens a one-line popup asking for a title; `c` shows in the key bar there.
- [ ] `enter` creates an issue with that title and no labels, its body the sections of
      `.github/ISSUE_TEMPLATE/ticket.md`, or empty when the project has none.
- [ ] On success the popup closes, the board is read again at once, and the new draft is selected by the number `gh`
      returned; no row appears before that read.
- [ ] On failure — offline, not logged in, or GitHub's refusal — the popup stays with the typed title and the reason
      under it; `enter` tries again, `esc` drops it. Nothing retries by itself.
- [ ] An empty title is refused in the popup, without a call.
- [ ] CLAUDE.md's out-of-scope line names both writes: ADR 0009's pending review and capturing a draft (ADR 0054).

## Model decision

A write on `board`: a `TicketCaptureCommand` in `infrastructure.primary` with a payload carrying the title, through
the application service to a domain port implemented over `gh issue create`; the template read is a secondary adapter
too. The domain checks the title is not blank.

## Design direction

```
┌ New ticket ─────────────────────────────┐
│ Title: The board shows a ticket's PR█    │
│ ⚠ gh: could not reach api.github.com     │
└ enter create · esc cancel ───────────────┘
```

## Glossary impact

*Capture* gains a row under `board`.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-32](LAZY-32-lazyclaude-writes-to-issues.md),
recorded in ADR 0054. The issue template comes with the harness's move to Issues.
