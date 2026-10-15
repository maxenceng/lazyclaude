# 29. A published type is read outside its module

Date: 2026-10-15

## Status

Accepted. Applies ADR 0025 to the rule that `published` only exists once another module reads it.

## Context

Review kept removing things that were built before anything read them: a domain copy of the branch-to-head match that
only its own test ran, a `HeadCommit` published for a later ticket, and a checking state that nothing produced. The
`published` part of this was already a written rule, but nothing checked it, and `Hosting` sat in `session.published`
while the shell only ever read `isHosted()`.

The simplest check, "a class outside the module depends on it", flagged 28 types that are genuinely read. The shell
reaches most published types through a primary `*View`'s accessor, and a call records a dependency on the view, not
on the type it returns.

## Decision

`ArchitectureTest.published_types_are_read_outside_their_module` counts a published type as read when:

- a class outside its module depends on it, or calls a method that returns it;
- it is a variant of a sealed published parent that is read;
- it is the nested builder of a type that is read;
- it is a `DomainException` that a read type in the same module throws, since `published` cannot reach `domain`.

A type exposed on a view's accessor that no outside class calls still fails. That was `HeadCommit`'s case, and the rule
was watched to fail on it.

`Hosting` moved to `session.domain`.

## Consequences

- A type published ahead of its reader fails the build. It waits in `domain` until a ticket reads it from another module.
- A type reached only through a generic return (`List<T>`), where the outside code never names `T` or calls one of its
  methods, counts as unread. Nothing does that today; if something legitimately does, add the case here, not an exemption
  per type.
- The domain half of the correction — a rule or a state that only its own test runs — is not checked. Telling a tested
  domain method from a used one needs the test classes in the import, which every other rule leaves out.
