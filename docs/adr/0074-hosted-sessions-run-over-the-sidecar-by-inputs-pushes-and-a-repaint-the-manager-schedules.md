# 74. Hosted sessions run over the sidecar by inputs, pushes and a repaint the manager schedules

Date: 2026-10-31

## Status

Accepted. Builds on ADR 0048, ADR 0049, ADR 0050 and ADR 0073. Amends ADR 0073: a Payload may hold a record. Amends
ADR 0049: the manager keeps each open session's size.

## Context

The sidecar served only `board/`. A desktop frontend that hosts `claude` needs the rest of ADR 0050: starting and
resuming (requests, since they can be refused), the pty's bytes (calls with no answer), the exit of a session, the
sessions' and the warnings' watches, and the repaint ADR 0049 promised. Four things were not settled by those ADRs.

## Decision

- **A call the sidecar takes and never answers is an input**, declared as `ProtocolInput<P>` and stated in the schema
  under `inputs` beside `methods` and `notifications`: `pty/input`, `pty/resize` and `session/repaint`. A frontend sends
  them as notifications; one sent as a request is answered with a null result. Rejected: a method with a made-up
  result, which would give the frontend a response to wait for and drop on every keystroke. Inputs are taken one at a
  time on one thread (`sidecar-input`) in the order they arrive, since keystrokes and resizes reordered are wrong;
  requests still run concurrently. `pty/resize` keeps ADR 0050's `cols` and `rows`, xterm's words for what it sends.
- **A record of one component is read from that component too.** A Payload holds `SessionId`, `PaneWidth` and
  `LinkedTicketId`, which the writer writes and the schema states as their component; Jackson would read them as an
  object. `UnwrappedRecordDeserializer` reads them as they are written, and leaves an object to the default reader so a
  method's own params, which are an object even when it has one component, are read as before. A value the record
  refuses is an invalid param. A sealed type in a Payload is still refused. Bytes are base64 text on both sides.
- **The repaint is `HostedSessionManager`'s.** It shrinks the session's pty by one row and, 50 ms later on a
  scheduler the composition root names (`session-repaint`), restores the size the pty has by then. To know that size
  the manager keeps the size each open session was launched with and follows every resize, which amends ADR 0049's
  "no size but the one a launch starts with". Resizing, the shrink and the restore take one lock, so a restore never
  applies a size older than one applied meanwhile. The scheduler is injected so the unit tests drive the gap without
  sleeping; the protocol test asserts the two resizes reach a stub `claude` with the gap.
- **`session/exited` is for a `claude` that exits by itself.** A session ended with lazyclaude tells no exit listener,
  the same distinction as the launch log's (ADR 0048): the frontend that closed stdin is gone, and one that is still
  there asked for nothing.
- **`warning/watch` answers `WarningsView`**, the status line's warning or `NoWarningView`, because the warnings
  primary adapter only pushed what was held and said nothing when none was. `WarningWatchPort` gained `held()` so the
  answer comes before the subscription's pushes, as the other watches do, and `warning/changed` carries
  `NoWarningView` when the last one is withdrawn. `WarningQuery.watch` makes that view, not the sidecar, and a
  none-held listener is told at once when none is held, so a warning withdrawn between the answer and the watch is
  still told, at the cost of a `warning/changed` that repeats an answer of none.
- **Closing stdin ends every hosted `claude`** after the requests still running are answered, through
  `HostedSessionCommand.endAll`, as the TUI's quit does.

## Consequences

- `ProtocolCoverageTest` lists the operations served under another name (`session/write` is `pty/input`,
  `session/resizePty` is `pty/resize`, `session/endAll` is closing stdin) beside those served by a watch.
- A pane of one row is not shrunk, so its repaint changes nothing (a `deferred:` comment on `PaneSize`).
- The stub `claude` of the protocol test polls its pty size every few milliseconds rather than trap `SIGWINCH`: bash
  runs a trap only when `read` returns under a pty, so a trap delivered the shrink late.
