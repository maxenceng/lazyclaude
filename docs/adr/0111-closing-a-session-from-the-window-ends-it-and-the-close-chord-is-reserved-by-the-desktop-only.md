# 111. Closing a session from the window ends it, and the close chord is reserved by the desktop only

Date: 2026-11-14

## Status

Accepted. Amends ADR 0048: the launch log also records an end when a session is closed from the window. ADR 0048's rule that
quitting ends every hosted session without an end stands.

## Context

A hosted session could only leave the dock when its `claude` exited by itself, or lazyclaude quit. A developer who is done with
one session had to type `/exit` into it. The desktop window can offer to close it: a close action on the dock's title row, and a
chord. ADR 0048 made a missing end in the launch log mean that lazyclaude, a crash or a kill cut the session off, so a close
that left no end would list the session first as interrupted, with `↺`, although the developer chose to end it.

## Decision

- **Closing a session ends its `claude` through a host close that does not wait** (terminate now; the grace period and the
  forced kill on a virtual thread of the host's own), because `session/close` runs on the sidecar's one input thread and
  waiting there would hold up typing to every other session. A quit keeps the blocking end, so nothing outlives it. Rejected:
  running `session/close` on the concurrent request executor, the one input method off the input thread. Unlike
  quitting, a close does not mark the session as ending with lazyclaude. Its exit is then a self-exit as the manager sees it: the
  launch-log entry gets an end, the tab leaves the dock through `session/exited`, and it is listed as a past session that
  `enter` resumes. Only a quit, a crash or a kill leaves an interrupted session. There is no new notification.
- **The sidecar takes `session/close`, with the session id and no answer.** Closing a session that is no longer open does
  nothing and says nothing, since its `claude` may have exited between the click and the answer to a confirmation. Closing one
  session leaves every other running.
- **Whether a close asks first is the window's decision.** It asks when the session is busy, waiting or unknown.
- **`close` is an eighth reserved action, `alt+x` by default, rebindable in `config.yml`, reserved by the desktop only.** The
  terminal frontend neither reserves nor acts on it, so `alt+x` in a TUI dock tab still reaches `claude`. The configuration
  still refuses it on a chord another reserved action or a fixed chord holds, and still warns when a Claude Code binding
  shadows it. Rejected: reserving it in the terminal too, which would take a chord from `claude` for a gesture the terminal
  has no pane action to pair with.

## Consequences

- A closed `claude` exits with the signal's code, and no warning is raised for it, unlike a failing exit by itself.
- A `claude` that ignores the termination signal is killed once the grace period runs out, after the close has returned.
- `ReservedAction` carries whether only the desktop reserves it. A later desktop-only chord sets that flag and nothing more.
