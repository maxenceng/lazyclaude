---
id: SESSION-7
status: in-review
context: session
type: feature
tracker_id: 84
created: 2026-10-10
---

# Waiting sessions on the whole machine: the Elsewhere panel grouped by project, and `notify-send` on entering waiting

## Acceptance criteria

- [ ] The Elsewhere panel lists every live session outside this project, `sdk-cli` excluded,
      under one `▾ <project> (n)` header per project. Each row shows its marker, or `∼` when
      busy, and its name. Projects are ordered by their strongest marker, then by name;
      sessions within a project the same way. Headers cannot be selected: `j`/`k` skip them.
- [ ] A session whose `cwd` is outside git is listed under a group of its own, named by that
      directory. It is no longer dropped.
- [ ] A session in a ticket worktree is listed under its project, never as a project of its
      own. A session in this project, ticket worktrees included, is never in Elsewhere.
- [ ] With an Elsewhere row selected, the main view shows that session's facts: project, name,
      state, waiting reason and `cwd`. `enter` on it does nothing.
- [ ] The status line's left side reads `! n  ? n  • n` for this project's live sessions,
      listing only the markers present. It is followed by `elsewhere:` and each other project
      with a waiting session, under its strongest waiting marker
      (`elsewhere: ! next-suggestions  ? claude-tools`). Overflow is cut with `…` before the
      warning.
- [ ] A project whose sessions are at most on your turn is not listed after `elsewhere:`.
      With nothing waiting elsewhere, `elsewhere:` is not shown.
- [ ] A live session anywhere on the machine (hosted or outside, this project included)
      entering waiting fires one desktop notification through `notify-send`. Its title is the
      project and the session name. Its body is the registry's `waitingFor` text, permission
      prompts included.
- [ ] Nothing fires for a session already waiting on lazyclaude's first read. Nothing fires
      for your turn, busy, unknown or starting.
- [ ] A session moving between waiting for permission and waiting for input replaces its
      notification in place (`-r`); no second notification appears.
- [ ] A session leaving waiting, by being answered or by dying, closes its notification over
      D-Bus (`CloseNotification`).
- [ ] Nothing fires while that session is the watched session. Becoming the watched session
      closes its open notification.
- [ ] A watched session that stops being watched while still waiting fires nothing: it fires
      only on the transition into waiting.
- [ ] In a terminal that never reports focus, the terminal counts as focused. A selected dock
      tab with the dock focused still suppresses its own session's notifications.
- [ ] With `notify-send` missing, or a notify, replace or close failing, the status line shows
      one warning, named once however often it fails. Everything else keeps working.
- [ ] A sweep in which no session's state changes fires, replaces and closes nothing.

## Model decision

**New behaviour on `session`'s live sessions, and a new notifications manager in `session`.**
No new context and no new edge.

- **Where a live session belongs.** `SessionManager` already locates every live session's
  `cwd` machine-wide, then discards the result when it filters to this project. It now keeps
  it. A session's place is a sealed value: its `Project`, or *outside git*, keyed by its own
  `cwd` (an explicit shape, never a null project). The manager answers this project's live
  sessions, as today, and **Elsewhere**: every other place's live sessions, grouped and
  ordered. The shell reads Elsewhere through `SessionQuery`, as part of `SessionsView`.
- **Permission prompts keep their reason.** `WaitingForPermission` carries its `WaitingFor`,
  as `WaitingForInput` already does, so the notification body has it.
- **Notifications.** A new domain manager, composed by `SessionApplicationService` (ADR 0027),
  decides from the transition between the previous and current machine-wide states plus the
  watched session: fire, replace or close. It holds each open notification's id.
  - Its first read records states and fires nothing.
  - It sits on the machine-wide live read, not on the project-filtered push. `refresh` and
    `knowTickets` re-deliver unchanged states, so it compares states rather than reacting
    to deliveries.
  - A `DesktopNotificationPort` is implemented by `NotifySendRepository` in
    `session.infrastructure.secondary`: `notify-send -p` and `-r <id>`, and
    `gdbus call … org.freedesktop.Notifications.CloseNotification <id>`.
  - A failure raises one warning through `WarningPort`/`WarnOnce` (ADR 0022).
- **Focus.** The shell switches on focus reporting (`?1004`) and tracks terminal focus,
  counted as focused until a focus-out arrives. It hands `session` the **watched session**
  through a primary `*Command` (`WatchedSession | NoWatchedSession`): the selected dock tab's
  live session, while the dock and the terminal both have focus. Whether TamboUI passes focus
  events through to the application is checked first at `start`; if it swallows them, raise
  it then rather than working around it.
- **ADR**, in the same change:
  - Only transitions seen after the first read notify.
  - Closing goes over D-Bus, because `notify-send` cannot close.
  - The terminal is assumed focused until it says otherwise.

Rejected:
- Notifying only for other projects. LAZY-8 covers every live session on the machine.
- Notifying on the first read. Every start would repeat what is already known.
- Leaving a notification up once its session is answered.
- Pulling SESSION-9's `notifications.desktop` switch in. Notifications are always on until
  that ticket.
- Pulling SESSION-6's transcript view or kitty jump in for `enter`.
- Pulling `w`/`alt+w` (next waiting) in: no ticket assigned them here.

## Design direction

The status line, then the Elsewhere panel, the fourth in the left column:

```
! 1  ? 2  • 1   elsewhere: ! next-suggestions  ? claude-tools            ⚠ …
┌ Elsewhere ─────────────┐
│ ▾ next-suggestions (2) │
│   ! refactor-ports     │
│   ∼ add-endpoint       │
│ ▾ ~/scratch (1)        │
│   • scratch-notes      │
└────────────────────────┘
```

- Headers follow the Board's status headers. Groups are always expanded; there is no
  collapsing.
- Marker glyphs and colours are LAZY-10's: `!` red, `?` yellow, `•` cyan, `∼` green.
- Keys: `j`/`k` move between session rows. `enter` does nothing. No new key.
- The main view for an Elsewhere row is its facts: project, name, state, waiting reason and
  `cwd`.
- The notification: title `<project> · <session name>`, body the `waitingFor` text.

## Glossary impact

- adds **Elsewhere** (`session`): every live session outside this project, grouped by
  project, or by its own `cwd` when outside git.
- adds **Watched session** (`session`): the session whose dock tab is selected while the dock
  and the terminal both have focus; it is never notified.
- adds **Desktop notification** (`session`): the `notify-send` notification a session's
  transition into waiting fires, closed when it stops waiting or becomes watched.
- sharpens **Project** (`shared`): the outside-git grouping it already describes now exists in
  code.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-8](LAZY-8-detecting-a-waiting-session.md), [LAZY-10](LAZY-10-screen-layout-and-keys.md). Notifications are suppressed while that session's pane and the terminal have focus.
