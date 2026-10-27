---
id: SIDECAR-1
status: todo
context: sidecar
type: feature
tracker_id: 97
created: 2026-10-27
---

# `lazyclaude --sidecar` answers the board over JSON Lines

## Acceptance criteria

- [ ] `lazyclaude --sidecar`, started in a project, writes `{"jsonrpc":"2.0","method":"ready","params":{"protocol":1}}`
      as its first stdout line once its adapters are built; `lazyclaude` with no flag still starts the TUI.
- [ ] Every stdout line is one JSON-RPC 2.0 message; logs, pty4j's SLF4J output included, go to stderr only.
- [ ] `board/boardFor` answers the board's View; `board/watch` answers the current View, then on each change `BoardQuery`
      signals reads the board again and pushes a `board/changed` notification carrying that View in full. A second `board/watch` returns the current View and
      opens no second watch.
- [ ] A sealed View carries `kind`, its simple name without `View` in lower camel case; paths, instants and ids are
      strings. Views and Payloads carry no Jackson annotation.
- [ ] An unparseable line, an unknown method and invalid params answer JSON-RPC's standard error codes; an uncaught
      exception answers the internal code with a short message, and its stack trace goes to stderr. A domain refusal or
      unavailable read is a result, never an `error`.
- [ ] Requests run concurrently and their responses may come back out of order, matched by `id`; one writer owns
      stdout, so lines never interleave.
- [ ] Closing stdin ends the sidecar with exit code 0.
- [ ] A protocol test spawns the built sidecar in a fixture project, writes JSON Lines to its stdin, and asserts on the
      lines it reads back.

## Model decision

A new module, `sidecar`, of kind `SHELL` (ADR 0050): a second composer beside `shell`.

- `LazyClaude` builds the adapters once and picks the composer by `--sidecar`.
- `sidecar` reaches `board` through `BoardQuery` and `published` only, as the shell does; nothing depends on it.
- Jackson is configured in `sidecar` alone: `ArchitectureTest`'s rule that Jackson stays in secondary adapters gains
  `sidecar` as its one other home, watched to fail on a Jackson import elsewhere.
- No domain change.

## Design direction

n/a: no screen.

## Glossary impact

*Backend*, *Frontend*, *Sidecar* and *Protocol* drop their `(expected)` marker where this lands them.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-38](LAZY-38-sidecar-protocol.md),
recorded in ADR 0050. Framing, base64 and stderr findings from [LAZY-36](LAZY-36-pty-over-a-sidecar.md).
