---
id: SIDECAR-3
status: todo
context: sidecar
type: feature
tracker_id: 99
created: 2026-10-27
---

# Hosted sessions run over the sidecar

## Acceptance criteria

- [ ] `session/startUnlinked`, `session/startTicket`, `session/resume` and `session/resumeInTicketWorktree` answer what
      `HostedSessionCommand` answers the TUI: the started session's `SessionId`, or its refusal (resume refused,
      directory missing). There is no per-session end; closing stdin is `endAll`.
- [ ] Each pty read is pushed as `pty/output {session, data}`, `data` base64; `pty/input {session, data}` writes the
      decoded bytes as they are, and `pty/resize {session, cols, rows}` resizes the pty. None is answered.
- [ ] `session/exited {session, code}` is pushed when a hosted `claude` exits by itself.
- [ ] `session/repaint {session}` shrinks the session's pty by one row and restores it 50 ms later, so `claude`
      repaints its whole screen (ADR 0049); the protocol test asserts the two resizes reach the stub with the gap.
- [ ] `session/watch` answers the sessions View (states and the launch log's past sessions) and pushes
      `session/changed` with each next one; `warning/watch` does the same for warnings with `warning/changed`.
- [ ] Closing stdin ends every hosted `claude`, and the launch log records no end for them (ADR 0048).
- [ ] The protocol test runs a stub `claude` that echoes its input and exits on a given byte, and asserts input,
      output, resize, exit and shutdown over the spawned sidecar.

## Model decision

The nudge and its 50 ms gap live in `HostedSessionManager`. Otherwise no domain change: `sidecar` drives
`HostedSessionCommand`, `SessionQuery` and `WarningQuery` as the shell does, over
`HostPort`'s `PtyBytes` (ADR 0049).

## Design direction

n/a: no screen.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-38](LAZY-38-sidecar-protocol.md),
recorded in ADR 0050. Blocked by [SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md),
[SESSION-11](SESSION-11-session-id-known-at-launch.md), [SESSION-12](SESSION-12-the-launch-log.md) and
[SESSION-13](SESSION-13-the-pty-port-carries-bytes.md). `claude`'s full repaint on the nudge was checked in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md); restoring the modes it does not re-send is the desktop frontend's.
