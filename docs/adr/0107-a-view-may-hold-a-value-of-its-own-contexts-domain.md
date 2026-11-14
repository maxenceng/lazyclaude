# 107. A View may hold a value of its own context's domain

Date: 2026-11-13

## Status

Accepted. Amends ADR 0012 and the context map: a `*View` is made of the context's `published` values, other Views, and values
of its own context's domain.

## Context

The context map said a View is made of `published` value objects. Three did not: `PullRequestView` holds `OpenedAt`,
`CommentCount` and `IssueClosing`, `SessionAgeView` holds `SessionAge`, and `PtyOutputView` holds `PtyBytes`, all from
their context's `domain`. Only the sidecar reads them, by serializing them, so no rule caught it. The pull requests table's
review asked whether to move them to primary view types, to `published`, or to accept them; the user accepted them.

## Decision

- **A View may hold a value of its own context's domain**: a record, an enum, or a sealed interface, alone or in a carrier
  such as `List` or `Optional`.
- **It never holds another context's domain type, nor a manager, a port or an exception.** ArchitectureTest's
  `views_hold_only_values_of_their_own_domain` fails the build on either.

## Consequences

- The shell still reaches a context only through `infrastructure.primary` and `published` (hard rule 7), so a TUI screen
  cannot yet read one of these fields by its type. The first that needs to moves the value to `published`, as ADR 0029 has a
  `published` type wait for its first reader.
- A View no longer has to mirror a domain value field by field just to stay out of `domain`.
