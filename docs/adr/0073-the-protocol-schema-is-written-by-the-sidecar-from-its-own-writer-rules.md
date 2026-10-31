# 73. The protocol schema is written by the sidecar from its own writer rules

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0050 (code first, a committed schema, a contract test of the operations).

## Context

ADR 0050 commits a JSON Schema of the protocol under `docs/protocol/`, generated from the Views and Payloads, and
leaves open what generates it. The sidecar writes a View by its own rules: a sealed type's variant is an object led by
`kind`, a record of one component that is no variant is that component, an enum constant is its name, anything else
that is not a record is its text. A generator library (victools' `jsonschema-generator` is the common one) maps a
record to an object of its fields; every one of those rules would be an override of it, and it is built on Jackson 2
where the sidecar is on Jackson 3. Params go the other way: Jackson reads a Payload as it reads any record.

## Decision

- **No generator library.** `sidecar` writes the schema itself. What makes a variant and what its `kind` is
  (`ViewShape`) is read by the writer and the schema alike; the other rules (one component unwraps, an enum is its
  name, an `Optional` is its value or `null`) are stated on each side, and the schema tests pin them. Rejected:
  victools with a custom definition per rule, which states the rules a second time and through Jackson 2.
- **Each method declares its Payload and its View, each notification its View**, as typed constants
  (`ProtocolMethod<P, R>`, `ProtocolNotification<V>`) the handler answers through: params are read into `P`, and an
  operation returning anything but `R` does not compile. `Sidecar` holds the one list of them that the schema, the
  dispatch and the contract test all read.
- **A sealed View is a `oneOf` with `discriminator: {propertyName: kind}`**, and each variant holds `kind` as a
  required `const`, so a type generated from it narrows on `kind`. `ready`'s params are `{protocol: <const>}`, from the
  version the sidecar writes.
- **A result is described as written, a Payload as Jackson reads it**: an object of its components, a one-component
  Payload included. A Payload holding a record or a sealed type is refused until a method takes one.
- **A shape the writer cannot carry fails the generation**: a `Map`, an array, a raw or wildcard generic, two types of
  one module and one simple name, a type in no module, a record of one component that holds itself. So does a declared type its values may be written
  differently from — an interface other than `Path`, an abstract or non-final class, `Object` — since the writer reads
  the runtime class and the schema the declared one, and a sealed type's variant that is not a record or a sealed
  type, which would carry no `kind`. It is renamed or reshaped, not described.
- **Params are read as the schema states them**: an enum by its name, not by `toString` (Jackson 3's default), and a
  path by `Path.of`, so `/tmp/a:b` is a path and not a URI with a scheme. A Payload component holding a record or a
  sealed type at any depth is refused, as above.
- **`lazyclaude --protocol-schema` prints it**, before anything is configured, and `make protocol-schema` writes it to
  `docs/protocol/protocol.schema.json`. Only the composition root prints, and the sidecar writes no file.
- **The drift check is a test**: it compares the committed file with a fresh generation, so `make test`, and so CI,
  fails on a stale schema.
- **The contract test names each public `*Query`/`*Command` operation `<context>/<operation>`**, as ADR 0050 does: a
  context's by its module, a kernel's by the type's role (`WarningQuery` is `warning`, so `warning/watch`). It requires a protocol
  method of that name, or that it is listed as served by a watch (`board/subscribe` and `board/boardAfterChange` by
  `board/watch`), or as not yet on the protocol. A listed operation that has a method fails it too, so the list only
  shrinks. Two operations of one context with one name fail it, since one method name would stand for both.

## Consequences

- Every operation of docs, review, session and the warnings, and the ticket capture, is listed as not yet on the
  protocol. Each leaves the list with the change that puts it on the sidecar; one that stays off for good (the TUI's
  own emulator) moves to a list of its own with the reason.
- A `kind` is the Java simple name's, and a `$defs` key is the module's and the simple name's (`board.Status`):
  qualified always, not on a clash, so a second context owning a `Status` renames no existing key and bends no
  model name to the wire. `kind` stays simple, being unique within its sealed type. Renaming a View, or a
  `published` value object a View holds, is a protocol change the drift check shows.
- Jackson reading a nested record in a Payload is assumed nowhere: the first method that takes one decides it, with a
  test of how it is read.
- The schema carries no descriptions; Javadoc is not there at runtime, and the Views stay free of annotations.
