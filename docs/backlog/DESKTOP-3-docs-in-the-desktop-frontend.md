---
id: DESKTOP-3
status: todo
context: desktop
type: feature
tracker_id: 114
created: 2026-10-27
---

# The desktop frontend shows the Docs panel

## Acceptance criteria

- [ ] The Docs panel shows `docs/watch`'s tree and each `docs/changed` after it, in the order the TUI shows it.
- [ ] Opening a document shows its text as the TUI does; loading, empty, unavailable and populated each render and
      have a test.

## Model decision

No backend change: SIDECAR-4's `docs/` methods.

## Design direction

The Docs panel as in the TUI, keys and mouse by ADR 0051.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md), recorded in ADR 0057. Blocked by
[DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md) and
[SIDECAR-4](SIDECAR-4-docs-and-review-over-the-sidecar.md).
