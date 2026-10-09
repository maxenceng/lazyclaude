# 6. The TUI is plain Java 25 on TamboUI, without an application framework

Date: 2026-10-09

## Status

Accepted

## Context

lazyclaude is a terminal application that reads a project's `docs/backlog/` and drives Claude
Code sessions. It has no HTTP surface, no database and no beans to wire; a framework would add
startup time to a tool that is opened and closed all day, and its conventions would be
conventions for a web service.

## Decision

Plain Java 25, built with Maven, rendered with TamboUI's toolkit DSL on the JLine 3 backend,
shipped as one shaded jar (`make run`). `LazyClaude.main` is the composition root: it
constructs adapters by hand and passes them in. `shell` is the TamboUI application; it
composes what contexts offer and holds no rules of its own.

Screens are tested by rendering an element into an in-memory buffer (`Screen` in the shell's
tests) and asserting on its text.

## Consequences

Constructor wiring in `main` is the whole dependency-injection story; it stays readable while
the number of adapters does.

TamboUI 0.5.0 asserts that elements render on a thread its runner has marked, and offers no
public hook to mark one in a test. `src/test/java/dev/tamboui/tui/RenderThreadForTests` sits
in TamboUI's package to call the package-private marker.
deferred: test-only split package into TamboUI — replace it once TamboUI ships a test renderer or a public marker.

TamboUI is pre-1.0. An upgrade is a deliberate change with `make verify`, not a version range.
