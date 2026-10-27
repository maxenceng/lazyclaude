---
id: DESKTOP-4
status: todo
context: desktop
type: feature
tracker_id: 115
created: 2026-10-27
---

# The desktop frontend shows the Review screen

## Acceptance criteria

- [ ] `p` on a ticket whose branch heads an open pull request opens its Review screen: the diff and its threads, as in
      the TUI.
- [ ] The pending review is written comment by comment and submitted as a Comment, each refusal shown as the TUI shows
      it (ADR 0009, ADR 0041).
- [ ] A dragged diff range selects lines as in the TUI (ADR 0051).
- [ ] Loading, unavailable, refused and populated each render and have a test.

## Model decision

No backend change: SIDECAR-4's `review/` methods.

## Design direction

The Review screen as in the TUI.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md), recorded in ADR 0057. Blocked by
[DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md) and
[SIDECAR-4](SIDECAR-4-docs-and-review-over-the-sidecar.md).
