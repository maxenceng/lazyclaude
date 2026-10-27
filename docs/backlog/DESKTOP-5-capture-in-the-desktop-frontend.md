---
id: DESKTOP-5
status: todo
context: desktop
type: feature
tracker_id: 116
created: 2026-10-27
---

# The desktop frontend captures a draft ticket

## Acceptance criteria

- [ ] `c` on the Board opens the capture popup as in the TUI and creates a label-less issue with the template body
      (ADR 0054).
- [ ] A failed create keeps the popup and its text and says why; it is never retried by itself.
- [ ] The created ticket shows on the Board through `board/changed`, selected.

## Model decision

No backend change: the capture method the sidecar gains with BOARD-9's command.

## Design direction

The capture popup as in the TUI, modal (ADR 0051).

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md), recorded in ADR 0057. Blocked by
[DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md) and
[BOARD-9](BOARD-9-capturing-a-draft-ticket.md).
