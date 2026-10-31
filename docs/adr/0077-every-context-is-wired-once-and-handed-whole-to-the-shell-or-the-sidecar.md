# 77. Every context is wired once and handed whole to the shell or the sidecar

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0006 (wiring by hand in the composition root) and ADR 0050 (the sidecar).

## Context

`LazyClaude` grew one wiring per output. `serve` built the subset of queries and commands the sidecar served, `open`
built its own for the shell, and the two shared helper methods but not a list: a context added to one was easy to
miss in the other. `open` then threaded seventeen arguments through `run` and `shellOn`, and every test of `shellOn`
spelled them all out. Review asked for the root not to carry methods specific to each output.

## Decision

- **`Contexts` holds every context's Query and Command, built once by `Contexts.of`.** `main` builds it, then hands
  it to `serve` or to `open`; neither output builds an adapter of a context.
- **What only one output needs is built by that output.** The terminal emulators of hosted sessions listen to the
  session service and exist only for the shell to draw, so `open` builds them from the session service `main` passes
  it. The sidecar would run an emulator per session that nothing reads.
- **`Sidecar` and `Shell` keep their own constructors.** Neither may depend on the composition root's package
  (rule 6), so `serve` and `shellOn` read `Contexts` and pass what each takes.
- Rejected: a `main` per output (`ShellMain`, `SidecarMain`), which keeps the duplicated wiring this removes.

## Consequences

- Both outputs build the same adapters, so the sidecar also builds the commands it does not serve yet (capture,
  watched session, ticket worktree). They hold no resource until called.
- A test of `start` or `shellOn` builds a `Contexts` through `ContextsFixture`, naming only the queries it drives.
