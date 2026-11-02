# 78. The sidecar answers the configuration read at start-up, as a list of chords

Date: 2026-11-01

## Status

Accepted. Builds on ADR 0058 (the desktop frontend owns its keys), ADR 0050 (the sidecar) and ADR 0077 (one wiring).

## Context

ADR 0058 has the desktop frontend take the reserved chords from `ConfigurationQuery`'s method. Two things stood in the way.
`ConfigurationQuery.configuration()` reads `config.yml` and raises the keybinding-clash warnings each time it is called, so a
second read by a request would hold every clash twice. And its `ConfigurationView` held a `ReservedChords`, a record of
one `Map`, which the protocol cannot write.

## Decision

- **`session/configuration` answers the `ConfigurationView` start-up read**, held in `Contexts` beside the Queries it was
  built over. It is not a call to `ConfigurationQuery`: the file is read once, by `ConfigurationLoader`, as for the TUI.
  The method is named `<context>/<operation>` like the others; the operation is `ConfigurationQuery.configuration`.
- **A refused configuration never reaches the protocol.** `Launch.configured` prints `lazyclaude: <file>:<line>: <reason>`
  to stderr and exits non-zero before `ready` is written, for the sidecar as for the TUI. The frontend sees the sidecar stop
  and shows the tail of its stderr (ADR 0050), which names the file, the line and the reason. Rejected: reading the file
  again per request so a refusal can travel as a fault, which repeats the warnings.
- **`ConfigurationView` holds a list of `ReservedChordView`**, one per action in the order of `ReservedAction`, each an
  `action` and a `chord` of `modifiers` (`CTRL`, `ALT`, `SHIFT`) and a `key` led by `kind`: `typedCharacter` with its
  `codePoint`, or `pressedKey` with a `NamedKey` name. `reservedChords()` rebuilds the domain `ReservedChords` for the shell.
- The protocol version stays 1: ADR 0050 versions by the bundle, and docs, review and warning methods were added without a bump.

## Consequences

- The chords are fixed for the life of the sidecar: editing `config.yml` takes effect at the next start, as in the TUI.
- A frontend maps `NamedKey` names (`PAGE_UP`) to `KeyboardEvent.key` (`PageUp`) itself.
