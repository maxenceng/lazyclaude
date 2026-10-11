# 11. Modules declare their kind, and each layer has one naming convention

Date: 2026-10-10

## Status

Accepted. Extends ADR 0010. The primary-adapter naming and what `published` holds are amended by ADR 0012.

## Context

`ArchitectureTest` told contexts apart from the rest by name: any direct subpackage of the root
except `shell` and `shared` was a context. Adding a second kernel (the error types every domain
throws) would have meant another hardcoded name, and a module nobody classified passed every
rule by default. The shell imported `board.domain` directly, so the domain was its API.

The review of the first context also asked for the claude-tools `spring-ddd` template's
conventions: failures as `DomainException`s, input checks through `Assert`, one class per file,
and the same name for each role in every context.

## Decision

- Every direct subpackage of `dev.maxenceng.lazyclaude` has a `package-info.java` carrying
  `@ApplicationModule(type = …)`: `CONTEXT`, `KERNEL` or `SHELL`. The architecture rules read
  that kind, and a class in an undeclared module fails the build.
- `error.domain` is a second kernel next to `shared.domain` (ADR 0010): `DomainException`,
  `DomainErrorStatus` and `Assert`, trimmed to the checks in use.
- The shell asks a context through its `infrastructure.primary` adapter and reads only its
  `published` types; it never imports a context's domain or application layer.
- Each context names its roles alike: `<Thing>ApplicationService` in `application` calls a
  `<Thing>Manager` in `domain`, which calls `<Thing>Port`s implemented by `<Thing>Repository`
  classes in `infrastructure.secondary`. The shell's entry point is a `<Thing>Controller` in
  `infrastructure.primary`, answering in `published` views.
- No nested types except builders: each concept is a top-level type, findable by name.

## Consequences

- A new module starts with its `package-info`, or the build fails.
- `published` now serves the shell as well as other contexts, so a type there is an API
  change for both.
- Mapping domain types to published views is a cost every context pays at its primary adapter.
- *Manager* leaves the glossary's list of avoided words: it now names the domain service of
  this convention, nothing else.
- A `*Repository` that implements no `*Port` fails the build (`ArchitectureTest`). A helper an
  adapter keeps for itself takes another name, as `TranscriptCacheFile` does.
