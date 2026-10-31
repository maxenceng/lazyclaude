# 75. No class copies a collection it was given

Date: 2026-10-31

## Status

Accepted. Widens the rule ADR 0012 ported: no defensive `copyOf` in domain or published types.

## Context

The `copyOf` rule covered only domain and published types, so a record in the sidecar copied its lists in its
constructor and an adapter copied a list it had built itself before handing it over. Review asked for the call to be
banned. A copy in one type reads as a guarantee the other types do not give, and an input that has to hold is checked
through `Assert`, not repaired by copying.

## Decision

- **No main class calls `List.copyOf`, `Set.copyOf` or `Map.copyOf`.** `ArchitectureTest.no_class_copies_a_collection`
  fails the build on one, in every module.
- **State that is handed out is an immutable list, reassigned rather than mutated.** `WarningRepository` and
  `TranscriptTail` replace their list with a new one on each change, so what they hand over cannot change under the
  receiver and needs no snapshot.
- **A collection built for one caller is handed over as built.** `TreeStamps` returns the walk's own map.
- **A conversion is a stream, not a copy.** Where a caller holds a view another shape (a map's values) the caller
  streams it into the list the callee takes.

## Consequences

- A record states what it was given; whether that list can change is the caller's, as it already was in the domain.
- Tests are not covered: a test may still copy a collection to compare it.
