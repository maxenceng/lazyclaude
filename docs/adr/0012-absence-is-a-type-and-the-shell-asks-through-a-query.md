# 12. Absence is a type, and the shell asks a context through a query

Date: 2026-10-10

## Status

Accepted. Amends ADR 0011's primary-adapter naming and what `published` holds.

## Context

The first `board` model had raw `String`s in composite types and filled gaps with stand-ins: a
ticket with no heading took its id as its title, and a missing `context` or `type` became `""`.
Nothing could tell "untitled" from "titled with its own id". ADR 0011 named the shell's entry a
`*Controller`, a Spring word, and put the `*View` records in `published`, so a view had to be
built from the domain by whoever called it. The author's other DDD project already enforced the
rules that would have caught all of this; the template never carried them over.

## Decision

- **A field a file may lack is a sealed value object with an explicit absent shape**:
  `TicketTitle` = `Title | NoTitle`, `TicketContext` = `ContextName | NoContext`,
  `TicketType` = `TypeName | NoType`. The domain never substitutes a value; the shell decides
  how to show an absence (the id for `NoTitle`, nothing for the other two).
- **The shell asks through a `*Query`** in `infrastructure.primary`, because it only reads; a
  write would be a `*Command`. What it is asked with is a `*Payload` with `toDomain()`, what it
  answers is a `*View` with a static `from(domainType)`, both beside it. The query only calls
  the application service.
- **`published` holds value objects only** (`TicketId`, `Status`, the absence types, `FileName`,
  `Reason`): what both the domain and its readers name.
- **SnakeYAML Engine stays.** `TicketFrontmatterMapper` is the one place that reads its untyped
  `Map`, and hands back a typed `TicketFrontmatter`.
- **Ported rules**, now in `ArchitectureTest`: no defensive `copyOf` in domain or published
  types; a type with two fields or more holds only domain types (`List`, `Map` and `Pair` are
  carriers) and is built only through its own nested `*Builder`; a domain exception extends
  `DomainException`; domain state is final and has no setters; a manager uses no `Assert`;
  only the domain holds a port; a manager is wired with ports, never another manager.

## Consequences

- A new optional field costs a sealed interface and two records, and every reader handles both
  shapes; the compiler finds the ones that do not.
- Every composite has a builder, so a domain type with more than one field is longer to write.
- A type in `published` that holds two raw values fails the build, so a read model for another
  module is a `*View` beside its query, not a `published` record.
