---
id: LAZY-38
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-25, LAZY-26, LAZY-36, LAZY-37]
---

# What is the sidecar protocol?

## Question

What is the sidecar protocol?

Request and response envelopes, request ids, error codes and payloads, asynchronous events (pty output, exit, waiting, board and docs changes, warnings), versioning and startup readiness, stdout for protocol and stderr for logs, cancellation, shutdown and restart, and how pty input, output and resize are framed. Where the spec lives and how it is tested without a frontend.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0050](../adr/0050-the-sidecar-speaks-json-rpc-over-json-lines-and-serializes-the-primary-adapters-views.md).

- **Words**: *backend*, *frontend*, *sidecar*, *protocol*, in the glossary's new `sidecar` section.
- **Envelope**: JSON-RPC 2.0, one message per line. Rejected: an envelope of our own.
- **Where**: a `sidecar` module of kind `SHELL`, beside `shell`, serializing the existing `*View`s and `*Payload`s
  through each context's `infrastructure.primary`. Rejected: per-context protocol adapters, separate protocol DTOs, a
  new module kind.
- **Methods**: one per `*Query`/`*Command` operation, `<context>/<operation>` after its Java name.
- **Failures**: refused and unavailable Views are results; `error` is for faults only, standard codes plus one internal.
- **Watches**: explicit `board/`, `docs/`, `session/`, `warning/watch` requests answering the current View, then
  `*/changed` notifications carrying the next View in full. Rejected: stale-only notifications, watches started on
  `ready`.
- **pty**: `pty/output`, `pty/input`, `pty/resize` and `session/exited` are notifications, data base64, keyed by
  `SessionId`; starting and resuming are requests; no flow control.
- **Startup**: `ready {protocol: <int>}` first; a mismatch is refused, no negotiation.
- **Concurrency**: concurrent requests, responses by `id`, one stdout writer, no cancellation.
- **Shutdown**: closing stdin only; on a crash the frontend says so, shows stderr's tail and offers a restart, never
  automatic.
- **Serialization**: Jackson in `sidecar` only, no annotations on Views; `kind` derived from the record name.
- **Spec and tests**: code first, a JSON Schema generated into `docs/protocol/` with a drift check, TypeScript generated
  from it; a test harness spawning the built sidecar, a stub `claude` for ptys.
- **Entry point**: one jar, `lazyclaude --sidecar`, in the project's directory.
- **The TUI stays in-process**, calling `*Query`/`*Command` directly. Rejected: the TUI speaking the protocol too.

Graduated into [SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md),
[SIDECAR-2](SIDECAR-2-the-protocol-schema-is-generated-and-checked.md),
[SIDECAR-3](SIDECAR-3-hosted-sessions-over-the-sidecar.md) and
[SIDECAR-4](SIDECAR-4-docs-and-review-over-the-sidecar.md).
