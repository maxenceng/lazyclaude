# 77. Every context is wired once and handed whole to the shell or the sidecar

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0006 (wiring by hand in the composition root) and ADR 0050 (the sidecar).

## Context

`LazyClaude` grew one wiring per frontend. `serve` built the subset of queries and commands the sidecar served, `open`
built its own for the shell, and a context added to one was easy to miss in the other. `open` threaded seventeen
arguments through `run` and `shellOn`, and the root held the shell's startup: its first read, its subscriptions, its
emulators and the end of hosted sessions. Review asked for the root to stay agnostic of the frontends.

## Decision

- **`Contexts` holds every context's Query and Command, built once by `Contexts.of`.** `Sidecar` and `Shell` take it
  whole and read what they drive. It lives in the root package, which the frontends may read as they read
  `ApplicationModule`; nothing in it depends on a frontend.
- **`ConfigurationLoader` reads the configuration, then builds `Contexts` over it.** A file that cannot be used is a
  `DomainException`, thrown before anything is built. The warnings it raises are held in the contexts it builds.
- **How lazyclaude is launched is a `Launch`, picked from the arguments by `Launch.from`.** `SchemaLaunch` prints the
  schema, `SidecarLaunch` sends `System.out` to stderr and serves on the real stdout, `ShellLaunch` opens the shell.
  `LazyClaude.main` runs the one picked and exits with its code, and knows nothing of either frontend. A launch
  prints its own refusal: the configuration's `DomainException`, or a missing project's `Reason`.
- **The composition root is `LazyClaude` and the `Launch` implementations.** `ArchitectureTest` exempts them, and only
  them, from the rule that nothing prints (ADR 0022) and from the rules that keep modules off the shells: each launch
  touches the streams it owns.
- **The shell's startup lives in the shell.** `ShellStartup` holds the first read, the subscriptions, the
  uncaught-failure handler and the end of hosted sessions; a failure that ends `main` is handed back for `main` to print.
- **A frontend that draws hosted sessions asks for their emulators.** `Contexts` builds them idle;
  `HostedEmulatorCommand.emulate` starts them listening, and only the shell calls it. The sidecar hands the pty's bytes
  to its frontend and parses no terminal.
- Rejected: a `main` per frontend (`ShellMain`, `SidecarMain`), which keeps the duplicated wiring this removes; and
  launches handed their streams by `main`, which keeps the sidecar's stdout redirect in `main`.

## Consequences

- Both frontends build the same adapters, so the sidecar also builds the commands it does not serve yet (capture,
  watched session, ticket worktree). They hold no resource until called.
- A `DomainException` that escapes the shell after it opened is printed as a refusal, one line, as one thrown
  by the first read is: `ShellStartup.open` runs both.
- A test of `ShellStartup` or `Sidecar` builds a `Contexts` through `ContextsFixture`, naming only what it drives.
