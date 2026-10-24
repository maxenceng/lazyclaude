# 44. A string is named, and a count is not compared to zero

Date: 2026-10-24

## Status

Accepted. Applies ADR 0025 to string literals and to the 0 that Checkstyle's `MagicNumber` ignores.

## Context

Review asked, more than once, for no magic number and no magic string. Checkstyle's `MagicNumber` ignores `-1, 0, 1` and nothing
checked strings, so `pane.rows().lineCount() > 0` and a box opened on `""` got through, along with every `gh` and `git` argument
written inline.

## Decision

Two PMD rules in `config/pmd-rules.xml`, run by `make rules`, over `src/main`:

- **`NameStringLiterals`**: a string literal stands only in a `static final` field's initialiser, an enum constant's arguments,
  an annotation, an `Assert` call's arguments, an exception's message (a constructor argument, or anywhere in an exception class),
  or as the template that `.formatted` is called on. Anywhere else it is named as a constant.
- **`NameZeroComparisons`**: a method's answer is not compared to the literal `0`; the question gets a name instead, such as
  `DiffRows.hasLines`, `Areas.hasNoRoom` or `isEmpty`.

A `gh api graphql` call is built through `GraphqlCall`, so a variable is named once and its `-f`/`-F` is chosen in one place.

## Consequences

- `0` and `1` are still allowed elsewhere, as in `IntStream.range(0, n)` or `index == 0`. Making them magic would need a name for
  "the first" at every index, which review has not asked for.
- A message built by `.formatted` may stay inline, the style the codebase already had for exceptions.
- A string in a test is not checked: a test's values come from its fixture (ADR 0013).
