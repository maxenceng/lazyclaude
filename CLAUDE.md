# lazyclaude

Java 25 terminal application on TamboUI. DDD bounded contexts with hexagonal layering, no
application framework (ADR 0006).

## What this is

A terminal IDE for Claude Code. It shows a project's `docs/backlog/` as a board and, from a
ticket, lets you jump into a live Claude Code session or start a new one — the `/ticket` loop
of the claude-tools harness, without leaving one screen.

It is for one developer working their own projects. The backlog's words win: ticket, status,
board, session — not issue, card, or chat.

Out of scope until an ADR says otherwise: editing anything (every view is read-only; edit
files in your own editor or through a session), talking to
the Claude API directly rather than through the `claude` CLI, and any tracker other than the
markdown in `docs/backlog/`.

Greenfield: nothing is built yet, and there are no users to keep compatible with.

## Commands

Use `make`, never `mvn` directly — the Makefile selects the Java 25 toolchain.

- `make doctor` — check the toolchain when something looks broken
- `make run` — build the jar and start the TUI in this terminal
- `make test` — all tests, architecture rules included
- `make test-one T=SomeTest` — one class or method, when the full run is too slow to iterate on
- `make contract` — the `@Tag("contract")` tests `make test` and CI exclude; run after a Claude Code upgrade
- `make fmt` / `make lint` — apply / verify formatting
- `make adr-check` — fail on a duplicate or missing ADR number; run it before choosing one
- `make verify` — everything CI runs, in CI's order; `make ci` is its first step
- `make verify-report` — the same run, into a log, printing the exit code and the totals
- `make pr-checks` — wait for CI on this branch's PR and exit on its verdict

## Structure

```
dev.maxenceng.lazyclaude.LazyClaude                  composition root: builds adapters, starts the shell
dev.maxenceng.lazyclaude.shell                       the TamboUI application: screens and keys
dev.maxenceng.lazyclaude.<context>.{published, domain, application, infrastructure.{primary, secondary}}
```

One module per direct subpackage of the root, its kind declared by `@ApplicationModule` in its
`package-info` (ADR 0011): a context, a kernel (`shared`, `error`), or the shell.
`board` is the first — `docs/context-map.md` lists the ones expected, and a context is created by
the first ticket refined into it. `published` only exists once another module — the shell or a context — reads it.

## Hard rules

Enforced by `ArchitectureTest`. When one fails, fix the code; do not weaken the rule without
an ADR.

1. Dependencies point inward: infrastructure → application → domain. A secondary adapter
   never calls the application layer.
2. The domain is plain Java — no TamboUI, no JLine, nothing outside `java.*` and other domains.
3. Only `shell` and `infrastructure.primary` touch the terminal (TamboUI, JLine).
4. Side effects — `ProcessBuilder`/`Process`, `java.nio.file.Files`, `java.net.http` — live in
   `infrastructure.secondary`, behind a port the domain declares. Running `claude`, `git` or
   `tmux`, and reading the backlog, are all side effects.
5. Contexts reach each other only through the other's `published` package.
6. Nothing depends on `shell`; it composes contexts, never the reverse.
7. The shell reaches a context only through its `infrastructure.primary` and `published`.
8. No nested types except builders.

## Conventions

- Wiring is constructor injection, done by hand in `LazyClaude.main`. No service locator, no
  static singletons.
- Roles are named alike in every context (ADR 0011): `*ApplicationService` → domain `*Manager`
  → `*Port`, implemented by a `*Repository`; the shell asks through a primary `*Query` (reads)
  or `*Command` (writes), with a `*Payload` in and a `*View` out (ADR 0012).
- A value a file may lack is a sealed value object with an explicit absent shape (`Title | NoTitle`),
  never `null` or a stand-in.
- A domain failure is a `DomainException`; an input check goes through `Assert`.
- A screen is tested by rendering it into a buffer (`Screen` in the shell's tests) and
  asserting on its text, not by driving a real terminal.
- A `*Test` class holds tests only; values come from the `<Type>Fixture` beside the type, and
  ports are mocked with Mockito (ADR 0013). `TestConventionsTest` enforces the first part.
- The `ddd-backend` skill describes a Spring project; its aggregate, value-object and
  port/adapter guidance applies here, its Spring, HTTP and persistence parts do not.

## Defaults

Settled. Act on these rather than asking.

- A coverage gap closes in the ticket that opened it. Write the test, do not defer it.
- A decision someone could reverse later without knowing why gets an ADR, in the same change.
- Work lands on a branch and a PR, never straight to `main`. Push once `make ci` is green.
- Formatting is whatever `make fmt` produces.
- Re-check an acceptance criterion against the pushed commit, never against an earlier run.
- A review finding with one obvious fix and no design choice gets fixed in the same pass,
  not reported and left. Only a finding that forks the design waits to be raised.
- Nothing under `src/test/` carries a comment — the test name is the explanation; the why goes
  in the assertion's `.as(...)`, the ADR or the ticket. Architecture rule holders are the one
  exemption (ADR 0003). `make test-comment-check` enforces it.
- A comment in a config file records the choice, never what the tool does, and fits in two
  lines. `make config-check` enforces the length.
- A limit deliberately chosen carries a `// deferred: <what it cannot do> — <what triggers
  closing it>` comment. `/debt` collects these with ADR *Consequences* and ticket *Notes*.
- A new rule, check or guard is not done until it has been watched to fail on the case it
  exists for. A guard under `scripts/check-*` has a fixture that trips it (ADR 0004).
- A build result is read from a captured exit code — `make verify-report`, or
  `make verify > log 2>&1; echo $?` — never from a pipeline. `Skipped: 0` is part of the result.
- Outside `docs/backlog/` and `docs/adr/`, name the behaviour rather than a landed ticket
  (ADR 0005). `make ticket-check` enforces it.

Ask when the choice is a genuine trade-off rather than a default: a review that contradicts
something asked for explicitly, a change to how sessions are launched or attached, or a rule
that would have to carve out an exception to hold.

## Where to look

- `docs/backlog/` — what to build and its status; driven by `/ticket`
- `docs/glossary.md` — ubiquitous language; use these words in code
- `docs/context-map.md` — contexts and their relationships
- `docs/adr/` — why things are the way they are
