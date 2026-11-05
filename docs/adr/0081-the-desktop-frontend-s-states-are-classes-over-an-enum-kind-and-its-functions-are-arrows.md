# 81. The desktop frontend's states are classes over an enum kind, and its functions are arrows

Date: 2026-11-02

## Status

Accepted. Builds on ADR 0057 and ADR 0079. Amended by ADR 0085: behaviour that keeps state is a class with private
methods, and no named function is declared inside another.

## Context

The first screen modelled each state as a union of object literals tagged by a string (`{ kind: 'read', bindings }`),
and callers asked the question themselves: `state.kind === 'read' ? state.bindings : []` in `app.ts`, the same
`kind === 'populated'` ternary in three places. The review of the first screen (PR #172) asked for enums, for classes
that answer what callers expect of a state, and for arrow functions over `function` declarations.

## Decision

- **Every hand-written state is a set of classes discriminated by an enum `kind`**: `Status` (`protocol/status.ts`),
  `BoardState` (`board/boardState.ts`), `ConfigurationState` (`keys/configurationState.ts`), a chord's key
  (`CharacterKey | NamedKey`) and `Binding`. The union of the classes keeps `switch` narrowing; each class answers the
  questions its callers asked — `shown()`, `reserved()`, `isReady()`, `isClosed()`, `matches(press)` — so the caller
  no longer compares kinds to get at a field.
- **A closed set of names is an enum**: `Action`, `LampColour`, `UnavailableCause`, `RelayMessageKind`. A design
  token prop (`size`, `tone`) stays a literal union, being a token rather than a meaning.
- **The generated protocol types stay as generated**: plain objects, string `kind`s, never edited (ADR 0057). A store
  turns them into a state class at the boundary.
- **Behaviour on a class is a prototype method**, not an arrow field, so two equal states compare equal.
- **Every other function is an arrow**, a component included. ESLint's `func-style: expression` and a
  `no-restricted-syntax` selector on a bare `function` expression enforce it.

## Consequences

- A test builds a state with `new`, and `toEqual` compares two states by their fields.
- A new state is a new class in its family's file and a new enum member, and every `switch` over the kind fails `tsc`
  until it handles it.
