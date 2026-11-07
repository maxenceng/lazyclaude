# 98. `q` asks before quitting whenever a session is hosted, in either frontend

Date: 2026-11-07

## Status

Accepted. Narrows how ADR 0048's quit is asked for, not what it does: quitting still ends every hosted session.

## Context

`q`, and `ctrl+c` outside a pane, quit lazyclaude. A quit ends every hosted session (ADR 0048). The TUI asked first only
while a hosted session was busy, waiting for permission or waiting for input, or could not be read. A session idle at its
prompt ("your turn") or still starting quit without a word: it loses no turn in flight, the conversation can be resumed.

The desktop window had `q` in its key table and nothing handling it. When it was wired to the TUI's rule, the hand walk on
the AppImage quit a window holding an idle `claude` without asking, which read as a bug. Ending a `claude` you can still
see is not undone by a resume: the pane, its scrollback and the session's place in the dock go with it.

## Decision

- **`q` asks whenever any session is hosted, idle or not, in both frontends.** It quits at once only when none is. The TUI's
  status line asks `N hosted session(s) would end — quit? y/n`. The window asks in a `Dialog` with Quit and Stay. `y`
  quits; any other key keeps lazyclaude and its sessions.
- **One rule, in each frontend's count of hosted sessions.** The TUI counts `SessionsView.hosted()`, the window counts the
  sidecar's `hosted` list, or its own panes while the session watch has not answered. The domain's `isBusyOrWaiting` on a
  hosted phase, and the `busyOrWaiting` field on `HostedSessionView` that carried it to the window, existed only for the
  old rule and are removed, so no second rule is left to drift. Rejected: keeping the busy-or-waiting rule, which ends an
  idle session you can see on one key.
- **The window also quits from a Quit button on its status line**, which does what `q` does. `q quit` is not on the key
  bars: the Board's bar with the dock keys would pass 80 columns.
- **The window's own close button does not ask.** Asking there needs the relay to hold a close request until the page
  answers, which nothing does yet.

## Consequences

- Quitting with sessions open takes one more key, every time.
- The protocol loses `busyOrWaiting` on a hosted session. Its version stays 1, since one sidecar ships with one frontend
  (ADR 0050).
- Deferred: the window's close button ends hosted sessions without asking — close it when the relay can ask the page
  before a close goes through.
