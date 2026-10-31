# 50. The sidecar speaks JSON-RPC over JSON Lines and serializes the primary adapters' Views

Date: 2026-10-27

## Status

Accepted. Builds on ADR 0048 and ADR 0049. Amends ADR 0011: `sidecar` is a second module of kind `SHELL`.

## Context

The desktop frontend drives the Java backend as a sidecar: Tauri spawns it, writes to its stdin and reads its stdout
(ADR 0048). LAZY-36 showed that hosted ptys travel well over one JSON message per line, base64-encoded, with stdout for
the protocol and stderr for logs. Each context's `infrastructure.primary` already answers the TUI through `*Query` and
`*Command`, taking `*Payload`s and answering sealed `*View`s. A refusal or an unavailable read is already a View
there, not an exception, and watches push their results. The protocol could invent its own envelope and DTOs, or reuse
what is already there.

## Decision

- **JSON-RPC 2.0, one message per line.** Requests carry an `id`, notifications carry none, and a fault is the
  `error` object. Rejected: an envelope of our own, which would need a spec and edge cases of our own for what JSON-RPC
  already settles.
- **A `sidecar` module beside `shell` serializes the existing Views and Payloads.** It composes the contexts through their
  `infrastructure.primary` and `published`, under the shell's rules: nothing depends on it. Rejected: one protocol
  adapter per context calling the application services, and protocol DTOs mapped from the Views, both of which give each
  outcome a second shape.
- **One method per `*Query`/`*Command` operation**, named `<context>/<operation>` after the Java method
  (`board/boardFor`, `session/startTicket`). The protocol grows when a primary adapter grows.
- **A domain outcome is a result.** Refused and unavailable Views travel as results, as the TUI gets them. `error`
  carries only faults: an unparseable line, an unknown method, invalid params, and an uncaught exception, which gets a
  short message while its stack trace goes to stderr. The codes are JSON-RPC's standard ones plus one internal code.
- **Watches are requests and pushes.** `board/watch`, `docs/watch`, `session/watch` and `warning/watch` each answer the
  current View (where the primary adapter only signals a change, the sidecar reads the View again itself), then push `<context>/changed` notifications carrying the next one in full, the latest winning. Calling
  a watch again returns the current View and opens nothing new. A watch nobody asked for never runs. Rejected:
  notifications that only say something is stale, which add a round trip and a race.
- **pty traffic is notifications.** `pty/output {session, data}` goes out, `pty/input {session, data}` and
  `pty/resize {session, cols, rows}` come in, with `data` in base64, and `session/exited {session, code}` goes out.
  `session` is the `SessionId` passed at launch. Starting and resuming are requests, because they can be refused; ending is closing stdin, as the TUI's quit is. There
  is no flow control.
- **Readiness and version.** The sidecar's first line is `ready {protocol: <int>}`, written once its adapters are
  built. The frontend sends nothing before that, and refuses to run on a version it was not built for. There is no
  negotiation, because one sidecar is bundled with one frontend.
- **Requests run concurrently** on the sidecar's executor, and responses come back in any order, matched by `id`. One
  writer thread owns stdout. There is no cancellation: a frontend drops a response that comes back too late.
- **Closing stdin is the only shutdown**, and there is no `shutdown` request. If the sidecar dies, the frontend says the
  backend stopped, shows the tail of its stderr and offers a restart. It never restarts by itself. The sessions
  are already gone, and the launch log has them as interrupted (ADR 0048).
- **Jackson, confined to `sidecar`.** Views and Payloads carry no annotations. A sealed View's `kind` is its simple name
  without `View`, in lower camel case (`UnavailableDiffView` → `unavailableDiff`). Paths, instants and ids are written as
  strings.
- **Code first.** A JSON Schema is generated from the Views and Payloads and committed under `docs/protocol/`, and the
  desktop's TypeScript types are generated from it. The build fails when the committed schema drifts. A protocol test
  spawns the built sidecar and talks JSON Lines to it, with a stub `claude` for hosted sessions. Rejected: a hand-written
  schema, and a prose spec.
- **One jar, two composers.** `lazyclaude` starts the TUI and `lazyclaude --sidecar` starts the sidecar, both in the
  project's directory. The TUI keeps calling `*Query`/`*Command` in-process. Rejected: the TUI speaking the protocol
  too, which serializes every keystroke to test what the spawned-sidecar test already tests.

## Consequences

- Renaming a View or a Payload field changes the protocol. The schema drift check is where that shows.
- A contract test checks that every public `*Query`/`*Command` operation has a method, so the protocol cannot fall
  behind the TUI unnoticed.
- `ArchitectureTest`'s rule that Jackson stays in secondary adapters gains `sidecar` as its one other home.
- Moving the tracker to GitHub Issues changes `board`'s and `review`'s methods along with their operations.
- A View that cannot be written as JSON (a callback, a stream) has to change shape before it reaches the protocol.
- A sealed type's variants are written as an object led by `kind`, a record that is not a variant and has one component is that
  component (`Title`, `TicketId`, `ProjectRoot` are strings), any other record is an object of its components, and an enum constant is its
  name. The sidecar does this by reading records, so no View or Payload needs an annotation, and a new View is written without a change here.
- A `DomainException` a `*Query` throws, such as a directory that belongs to no project, has no View to travel as, so it answers
  as the internal fault with its message and its stack trace goes to stderr. It becomes a result when its primary adapter gives it a View.
- Requests run on a virtual thread each, named `sidecar-request-n`, and a thread named `sidecar-writer` writes stdout. `LazyClaude`
  points `System.out` at stderr before starting the sidecar, so a library that prints cannot break a line.
