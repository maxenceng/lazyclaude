---
id: SIDECAR-4
status: todo
context: sidecar
type: feature
tracker_id: 100
created: 2026-10-27
---

# Docs and review are read and written over the sidecar

## Acceptance criteria

- [ ] `docs/docsFor` answers the docs View; `docs/watch` answers the current one and pushes each next one as
      `docs/changed`.
- [ ] Every `ReviewQuery` operation and every `PendingReviewCommand` operation has a `review/<operation>` method
      answering its View, refused writes included as results.
- [ ] The protocol test asserts one read and one refused pending-review write over the spawned sidecar, with `gh`
      stubbed.

## Model decision

No domain change: `sidecar` drives `DocsQuery`, `ReviewQuery` and `PendingReviewCommand` as the shell does.

## Design direction

n/a: no screen.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-38](LAZY-38-sidecar-protocol.md),
recorded in ADR 0050. Blocked by [SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md). `review`'s methods change with
its operations once the tracker moves to GitHub Issues.
