# 33. A notification fires on the transition into waiting, and closes over D-Bus

Date: 2026-10-17

## Status

Accepted. Builds on ADR 0021 (the sweep that reads the machine's sessions), ADR 0022 (warnings) and ADR 0027 (the
application service composes managers).

## Context

`session` reads every live session on the machine, then keeps this project's. Telling the user that a session, in this
project or any other, has started to wait on them needs a decision made from what changed between two reads, not from the
reads. `refresh` and `knowTickets` hand the same states over again, and the first read at start-up finds sessions that
have been waiting for hours.

## Decision

- **Only a transition seen after the first read notifies.** `DesktopNotificationManager` records the states of the first
  read and fires nothing. After that it compares states, not deliveries: a read in which no session's state changed fires,
  replaces and closes nothing, however often the application service hands the same states to its listener.
- **What a transition does.** Entering waiting (for permission or for input) shows a notification unless the session is
  the watched one. A session moving between the two replaces its notification in place. One that stops waiting, or
  disappears from the machine, has its notification closed. Your turn, busy, unknown and starting never notify. A
  session that becomes watched has its notification closed; one that stops being watched while still waiting fires
  nothing, since only the transition into waiting does.
- **The notification** is titled `<project> · <session name>`, the project being the group's name (the directory when the
  session is outside git), and its body is the registry's `waitingFor` text. `WaitingForPermission` therefore carries its
  `WaitingReason`; it is recognised from that text, so unlike `WaitingForInput` it always has one.
- **Closing goes over D-Bus.** `notify-send` can show (`-p` prints the id) and replace (`-r <id>`) but cannot close, so
  `NotifySendRepository` calls `gdbus call --session ... org.freedesktop.Notifications.CloseNotification <id>`. A reply
  that the notification was not found counts as closed, since the user dismissed it first.
- **It sits on the machine-wide read, composed in the application service.** `SessionApplicationService` hands the
  manager every `Sessions` the sweep, the transcript watch and a refresh deliver, before the shell's listener, which is
  where the machine-wide live sessions (this project's, then each place elsewhere) are all in hand (ADR 0027). Its
  processes run on the watcher's thread, never the render thread.
- **A failure is one warning.** A missing `notify-send` or `gdbus`, or a notify, replace or close that fails, raises one
  warning through `WarnOnce` (ADR 0022) and the sessions go on.
- **The terminal is assumed focused until it says otherwise.** The watched session is the selected dock tab's live
  session while the dock and the terminal both have focus. A terminal that never reports focus never produces a
  focus-out, so it counts as focused and the selected tab of a focused dock still silences its own session.

## Consequences

- A session that begins waiting between lazyclaude's first read and the sweep after it is notified; one that was already
  waiting at start-up never is.
- A notification the user leaves up while the session keeps waiting stays until the session is answered, dies or is
  watched.
- Notifications are on unless `notifications.desktop: false` in `config.yml` turns them off. The manager then never calls its
  port; markers and the status line, which do not come through it, still show a waiting session.
- The notification ids live in memory: a restarted lazyclaude no longer knows the ones the last run left open.
