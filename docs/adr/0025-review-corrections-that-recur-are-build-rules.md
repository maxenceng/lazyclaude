# 25. Review corrections that recur are build rules

Date: 2026-10-12

## Status

Accepted

## Context

The same review comments came back on PR after PR: a list of raw strings where a value object
belonged, a `for` loop where a stream would do, a magic number, a public static field, a domain
constructor that enforced a business rule. Each was fixed where it was flagged, then written
again in the next ticket. Earlier corrections, such as named exceptions and nested types,
stopped recurring once `ArchitectureTest` failed on them.

## Decision

A correction made twice becomes a check that fails the build.

- **`ArchitectureTest`** has four more rules. No class has a public static field. A domain or
  published type never carries raw values in a `List`, `Map` or pair, even when that is its only
  field. A domain constructor throws nothing itself, so its input checks go through `Assert`.
  A `*View`'s public methods take domain types, not raw values.
- **PMD** (`config/pmd-rules.xml`) fails on a `for` loop. A loop whose body throws a checked
  exception keeps the loop, because a lambda cannot throw one and `ArchitectureTest` forbids
  wrapping it. It is suppressed with `@SuppressWarnings("PMD.UseStreamsNotForLoops")` and a
  comment that gives the reason. `while` loops are not checked: they are the blocking polls.
- **Checkstyle** (`config/checkstyle.xml`) runs `MagicNumber` and nothing else, and ignores
  only -1, 0 and 1. Spotless still owns formatting. Checkstyle is pinned to a version that
  parses Java 25, because the plugin's bundled version cannot.

`make rules` runs PMD and Checkstyle, and `make ci` runs `make rules`.

## Consequences

- A rule that catches only what one reviewer dislikes is a rule someone has to argue with.
  Each of these was asked for at least twice, and was watched to fail on the code that was
  flagged.
- Magic strings are not checked. `MultipleStringLiterals` catches only repeats, and a check
  on every literal would flag every message. Review still catches them.
