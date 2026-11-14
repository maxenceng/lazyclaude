# 113. A hosted session may start at `0 0` and is sized by the WINCH that follows

Date: 2026-11-14

## Status

Accepted.

## Context

`HostedPty` spawns `claude` through pty4j with `setInitialColumns` and `setInitialRows`, and a child that runs `stty size`
straight away can read `0 0`. Ticket #146; found by the research for #128 (`docs/research/jlink-sidecar-bundle.md`).

The cause is in pty4j 0.14.0 (`com/pty4j/unix/UnixPtyProcess.java`): the child is exec'd on the `Reaper` thread, and the
spawning thread waits for its pid and only then calls `setWindowSize` with the initial size. So the child is already
running before its pty has a size. Setting the size raises a SIGWINCH that carries it.

What was measured, with a child that prints `stty size` at start and on every WINCH, spawned at 100×30:

- In a fresh JVM the first spawn reads `0 0`, then a WINCH brings it to `30 100`. This happened 10 times in 10 when the
  ticket was written, and 9 in 10 when it was re-run for this ADR.
- In a warm JVM the spawn usually wins the race: 19 times in 19.
- With no initial size at all, the child stays at `0 0` and gets no WINCH.

## Decision

- **lazyclaude accepts the race and does not work around it.** The WINCH that pty4j's late `setWindowSize` raises corrects
  the size, and `claude` redraws when it is resized. So a hosted session that starts at `0 0` draws at its pane's size a
  moment later. `PtyHostRepositoryTest` checks that a child spawned at 100×30 reads no size but `0 0` or `30 100`, and ends
  at `30 100`. It cannot force the race: in the warm JVM of a test run the spawn usually wins it, so the `0 0` read is
  seen mostly in a fresh JVM.
- Rejected: setting the size before exec through an `sh -c 'stty rows R cols C; exec "$@"'` wrapper. It changes how
  sessions are launched, to fix a race that a real `claude` has not been seen to lose.
- Rejected: patching or forking pty4j, which would put the fix on a timeline lazyclaude does not control.

## Consequences

- A child that reads its size once and ignores WINCH starts at `0 0` and stays there. `HostedPty`'s spawn carries the
  `// deferred:` comment for this.
- Revisit when a hosted `claude` is seen drawing its first frame at the wrong width, or when a pty4j release sizes the pty
  before exec.
- A pty stand-in in the tests waits until `stty size` stops reading `0 0` before it reports anything.
