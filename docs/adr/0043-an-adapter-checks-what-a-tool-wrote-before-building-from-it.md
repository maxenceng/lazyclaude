# 43. An adapter checks what a tool wrote before building from it

Date: 2026-10-24

## Status

Accepted. Applies ADR 0025 to reading a gap in a tool's output. Follows next-suggestions' ADR 0064.

## Context

Every secondary adapter that read `gh`, Claude Code's JSON, a transcript or a ticket's frontmatter built the domain types first,
let a value object's `Assert` refuse a missing field, then caught `DomainException` around the whole mapping and turned the
message into a reason. Review rejected this: the domain is not there to handle external errors, and a gap in what a tool wrote
must be caught before the domain is reached. The catch also swallowed any domain refusal, including one that was a bug.

## Decision

- **A record read from outside names what it lacks before anything is built from it.** Each record has `missingFields()`, which
  gathers the names of the fields it lacks through `MissingFields` (shared kernel, `infrastructure.secondary`). The reader turns
  a non-empty answer into the context's own *unavailable* exception, or into the ticket's *unreadable* reason, naming the fields.
  The record maps only after that, and calls no `Assert`.
- **A value only the domain can judge is asked of the domain without a throw.** A ticket's id goes through `TicketId.fits`, its
  status through `Status.find`, so the frontmatter phrases its own refusal. `UnknownStatusException` goes with `Status.parse`.
- **A title or a prompt written blank is no title**, the same as none, so a blank AI title still lists the session.
- **`ArchitectureTest` enforces it.** In `infrastructure.secondary`, `secondary_adapters_catch_no_domain_refusal` fails a catch
  of `DomainException` itself or of an `AssertionException`, and `secondary_adapters_assert_nothing` fails a call to `Assert`.

## Consequences

- An adapter may still catch its own *unavailable* exception to fall back or re-throw, since the domain does not throw it, and a
  JDK failure such as `InvalidPathException` for a `cwd` that is not a path.
- A field no tool has been seen to leave out is not checked. If one is ever missing, the domain's `Assert` throws and the read
  fails loudly instead of showing a reason. That is deliberate, as in next-suggestions' ADR 0064.
- `ReviewQuery.anchorOf` still catches the domain's refusal of a range: there the domain refuses a person's choice of lines,
  which is its own rule, not a gap in a tool's output.
