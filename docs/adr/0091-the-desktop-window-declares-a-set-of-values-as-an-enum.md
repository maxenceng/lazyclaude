# 91. The desktop window declares a set of values as an enum

Date: 2026-11-06

## Status

Accepted. Carries ADR 0044 (a string is named) and ADR 0088 (a pattern is named) to the frontend's string unions.

## Context

The review of the desktop Review screen said "magic string" or "use enums" four times, on `Annotation`, `CommentRow`
and `TextArea`. Each time, a set of values was written as a union of string literal types (`'thread' | 'pending'`), and
each member was spelled again at every comparison and prop. Those were fixed in that pull request, but nothing stopped
the next one being written: `Arrangement`, `Pane`'s `layout`, `Lamp`'s `size` and the review key bar's panel were still
unions.

## Decision

- **A set of values written by hand is a TypeScript `enum`** (`Arrangement`, `PaneLayout`, `LampSize`, `ReviewFocus`),
  and the code compares and passes its members, never the strings.
- **ESLint enforces it** in `src/` outside tests: `no-restricted-syntax` refuses a string literal type inside a union.
  It was watched to fail on the four unions above.
- **A type argument or a type parameter's constraint is exempt.** `Pick<Reads, 'comments' | 'summary'>` lists property
  names, and `M extends 'review/pendingOf' | …` lists protocol method names. Neither is a set of values.

## Consequences

- The protocol's generated types stay string unions, because the generator emits them and `src/protocol/generated` is
  not linted. Comparing against their members (`line.kind === 'addedLine'`) is still allowed: the compiler checks every
  such literal against the union, so a misspelled member fails `tsc`.
- A lone string literal type (`kind: 'x'`) is not a union and is not checked. None exists yet.
