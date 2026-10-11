# 22. Warnings go through the shared kernel and never to stderr

Date: 2026-10-11

## Status

Accepted. Extends ADR 0017.

## Context

Three adapters named a degraded mode by printing to stderr: the docs watch falling back to
polling, the sessions watch falling back to a sweep, and the live sessions read from
`claude agents --json` instead of the registry. A line printed once the screen is up lands on
top of the drawn frame. The warnings come from more than one context, and the shell may not be
depended on (ADR 0011), so none of them can hand the shell a message directly.

## Decision

- **A warning is a `Warning` in `shared.domain`, raised through a `WarningPort`.** Nothing
  lazyclaude writes goes to stderr while the screen is up.
- **A context's own domain raises it, never an adapter.** The `docs` and `session` managers
  hold the `WarningPort`; they turn the context's degraded fact into the warning text, so the
  wording lives in the domain. This follows from `ports_are_held_only_by_the_domain`: a port
  field in a secondary adapter is a violation, and the rule stays as it is.
- **An adapter reports that it runs degraded through the port its context already declares.**
  The port takes a callback named in the context's language, and the adapter calls it once:
  - `docs.domain.WatchFallback.polling(cause, every)` on `DocsWatchPort.watch`;
  - `session.domain.WatchFallback.sweepingAlone(cause, every)` on `SessionWatchPort.watch`;
  - `session.domain.SourceFallback.askingClaudeAgents(cause, every)` on
    `LiveSessionPort.liveSessions`.
  `DocsWatchRepository`, `SessionWatchRepository` and `LiveSessionRepository` therefore hold no
  warning sink and no `PrintStream`. This reverses the ticket's line that they "take a
  `WarningPort` in place of their `PrintStream`".
- **`WarningRepository` keeps every warning in memory** and replays the held ones to a
  subscriber, so a warning raised before the screen exists reaches its first frame. A
  `WarningWatchPort` is the read side, taken by the shared `WarningManager` in the same shape
  as the watch ports of `docs` and `session`.
- **The shared kernel gains a primary adapter**, `shared.infrastructure.primary.WarningQuery`,
  and with it an application service, because the shell reaches a module only through its
  `infrastructure.primary` and `published` (ADR 0011) and asks through a `*Query` returning a
  `*View` (ADR 0012). ADR 0017 gave the kernel a secondary package; this adds the primary one.
- **The status line shows the newest warning and the count**, right-aligned; there is no key or
  timer that clears one.
- **Both are enforced by `ArchitectureTest`.** `only_the_composition_root_prints` keeps
  `System.err`, `System.out`, `PrintStream` and `printStackTrace` out of every class but
  `LazyClaude`, and `the_shell_reaches_modules_only_through_primary_and_published` holds a
  kernel to the same entry points as a context, so the shell cannot skip `WarningQuery`.
- **A refusal to start is not a warning.** The board-read failure at start-up still prints to
  stderr and exits non-zero, because no screen has opened.

Rejected:

- **An exemption in `ports_are_held_only_by_the_domain` for the kernel's `WarningPort`.** It
  would make the rule say "a port is held by the domain, except this one", and the next shared
  port would ask for the same.
- **A bare `Consumer<Warning>` handed to the adapters.** It passes the rule by its type alone
  and puts a warning sink in an adapter, which is what the rule exists to prevent.

## Consequences

- The kernel grows by a value, a collection of it, two ports, a manager, an application
  service, a query, a view and an adapter; `docs` and `session` are coupled to `Warning` and
  `WarningPort`.
- A new degraded mode names itself by a callback on its context's port and a line in that
  context's manager, not by printing.
- `LiveSessionPort.liveSessions` now takes a `SourceFallback`; the two leaf sources
  (`SessionRegistryRepository`, `ClaudeAgentsRepository`) accept and ignore it, since they
  have nothing to fall back to.
- Warnings are never forgotten: a mode that flaps raises a warning each time and the count keeps
  growing. Each adapter that can flap tells its fallback once, as the three today do.
- An exception that ends a watcher thread is still printed by the JVM's default handler on
  stderr (the existing *deferred* notes on both watchers); closing those routes it here.
