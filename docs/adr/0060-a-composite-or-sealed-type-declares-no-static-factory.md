# 60. A composite or sealed type declares no static factory

Date: 2026-10-28

## Status

Accepted. Applies ADR 0025 to the factories that DOCS-1's review and SESSION-10's both refused.

## Context

A value record carries no build logic; whoever owns its parts builds it. `composite_domain_types_are_built_through_their_own_builder`
let a type construct itself, so a static `of` could get past it, and nothing at all looked at sealed interfaces. That is how
`ReadFacts.of(Optional<String>, Optional<String>)` and `SessionTitle.of(...)` came in. Each took a transcript's raw strings and
decided which shape they made, which moved the transcript reader's work into a published value. `TicketBody.of`, `DocumentText.of`
and `Configuration.defaults` had the same shape: a file's reading, or a missing file's meaning, written onto the type it produces.

## Decision

`ArchitectureTest.composite_and_sealed_types_declare_no_static_factory`: in `domain` and `published`, a static method of a type
with two fields or more, or of a sealed type, builds neither that type nor one of its variants. Only the type's builder builds a
composite. The adapter that reads the source chooses a variant: a blank body is `NoBody` in `TicketFileReader`, a missing title
falls back to the first prompt in `CachedTranscript`, and the defaults of a missing `config.yml` are `ConfigurationNodes`'.

## Consequences

- A single-value type keeps its named factory, such as `NamedTicketId.fromWord`, `HostedSessionId.random` or
  `ReservedChords.defaults`. That parses one value; it does not assemble or choose a shape.
- An instance method that returns a changed copy, such as `PastSession.linkedTo`, still builds its own type. The rule looks only at
  static methods.
- A factory's tests move to the adapter's tests, which already covered the same cases through a real file.
