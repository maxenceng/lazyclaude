---
id: SIDECAR-2
status: todo
context: sidecar
type: feature
tracker_id: 98
created: 2026-10-27
---

# The protocol schema is generated from the Views and checked in the build

## Acceptance criteria

- [ ] A make target generates a JSON Schema of every method's params and result and every notification's params from
      the Payloads and Views, into `docs/protocol/`, committed.
- [ ] The build fails when the committed schema differs from a fresh generation, watched to fail on a renamed View field.
- [ ] A contract test fails when a public `*Query` or `*Command` operation has no protocol method, watched to fail on
      one added without a method.
- [ ] The schema names each sealed View's `kind` as a discriminator, so TypeScript types generated from it narrow on
      `kind`.

## Model decision

Tooling in `sidecar` and the build only (ADR 0050, code first); no domain change. The generator library is chosen here.

## Design direction

n/a: no screen.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-38](LAZY-38-sidecar-protocol.md),
recorded in ADR 0050. Blocked by [SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md). Generating the desktop's
TypeScript types from it is DESKTOP-1's, under `make frontend` (ADR 0057).
