---
id: SESSION-4
status: in-progress
context: session
type: feature
tracker_id: 81
created: 2026-10-10
---

# A new unlinked session runs hosted: `claude` on a pty in a dock pane, a tab per session, `alt+h/j/k/l` focus

## Acceptance criteria

- [ ] `N`, from any panel, starts a plain `claude` in the project root on a pty and opens it as
      a new dock tab with focus. The tab reads `starting` until its registry record appears,
      then its marker and title, as the Sessions row shows them.
- [ ] While a dock tab has focus, every key goes to `claude` except the reserved
      `alt+h/j/k/l/m/z/w`. Tab, Shift+Tab, Esc, Ctrl+C and Ctrl+R reach it, and Ctrl+C never
      quits lazyclaude. Ctrl+Z is swallowed, so `claude` is never stopped.
- [ ] A paste reaches `claude` as one bracketed paste, so its newlines do not submit the
      prompt.
- [ ] Mouse wheel and clicks over the pane reach `claude` at the matching cell, so its
      fullscreen UI scrolls with the wheel.
- [ ] The pty takes the pane's size, and is resized whenever the pane's size changes.
- [ ] In the dock, `alt+h`/`alt+l` step through tabs, and `alt+h` on the first tab goes to the
      panels. `alt+k` goes to the main view and shows it if hidden. `alt+j` from the main view
      goes to the dock.
- [ ] `m` outside a session, or `alt+m` anywhere, hides or shows the main view. Hidden, the
      dock fills the right column. It stays hidden until toggled back.
- [ ] A hosted session is listed in the Sessions panel's unlinked section, with `pane` where an
      outside one shows `kitty`. `enter` on it focuses its dock tab.
- [ ] `enter` on an unlinked past session resumes it with `claude --resume <id>` in a new dock
      tab, in its recorded cwd. If that directory is gone, it runs in the project root and the
      status line warns that it did.
- [ ] When `claude` exits, its tab closes and the session is listed as past. Focus moves to the
      next tab, or to the panels after the last one. A non-zero exit code warns on the status
      line.
- [ ] Spawning that fails (no `claude` on the PATH, no pty) opens no tab and warns on the
      status line.
- [ ] `q` outside a session quits at once when no hosted session is busy or waiting, and
      otherwise asks y/n first. On yes, every hosted `claude` ends with lazyclaude.
- [ ] The child's environment carries no `CLAUDE*`, `KITTY*` or `TMUX` variable.
- [ ] pty4j and jediterm-core types appear only in `session.infrastructure.secondary`, and an
      architecture rule fails the build otherwise.

## Model decision

**New aggregate, `HostedSession`, in the existing `session` context.** No new context, and
no new edge between contexts.

- **Domain.** `HostedSession` is a `claude` process lazyclaude owns: *starting* (spawned, no
  registry record), then live with its state read from the registry by the child's pid, then
  exited and dropped, so the transcript lists it as past. `HostedSessionManager` holds the
  open ones in tab order and decides the cwd: the project root for a new unlinked session,
  the recorded cwd for a resume, the root with a warning when that is gone. `HostPort`
  declares spawn, resume, input, resize and the exit and screen-changed signals.
- **Secondary adapter.** `PtyHostRepository` alone touches pty4j and jediterm-core. It spawns
  with the scrubbed environment, feeds output to the emulator on a reader thread, encodes
  keys, paste (bracketed when the child turned on 2004) and SGR mouse into bytes, and copies
  the emulator's buffer into a published `TerminalScreen`. A new architecture rule confines
  both libraries to `session.infrastructure.secondary`.
- **Published.** `TerminalScreen` (rows of styled runs, colours incl. truecolor, the cursor),
  and `TerminalKey` / `TerminalMouse`, what the shell hands in. No TamboUI type crosses.
- **Primary.** `HostedSessionCommand` (start unlinked, resume, key, paste, mouse, resize) is
  the shell's write path; `SessionQuery`'s views carry the hosted sessions and their screens,
  and `LiveSessionView` says hosted or outside, which `SessionManager` learns from the hosted
  pids.
- **Shell.** A `TerminalScreenElement` draws the screen with `setString` per run. The shell
  maps TamboUI's `KeyEvent`/`PasteEvent`/`MouseEvent` to the published values, builds its
  `TuiConfig` with bracketed paste and mouse capture on and Tab/Shift+Tab unbound from focus,
  and repaints on the screen-changed signal as it does for the watchers.
- **Build.** The pom names JetBrains' `intellij-dependencies` repository for jediterm-core;
  the launcher passes `--enable-native-access`.
- **ADR, in the implementing change:** the hosted-session aggregate and its port, the encoding
  boundary in the secondary adapter, mouse capture on, and the lost Enter chords.
- **Deferred:** Ctrl+J, Shift+Enter, Ctrl+Enter and Alt+Enter are lost inside the pane, since
  TamboUI collapses them before the shell sees them. A `deferred:` note closes it when TamboUI
  supports the kitty keyboard protocol (upstream #446). A raw-byte backend was considered and
  left out as untried risk.
- Not in this ticket: sessions from a ticket, the verb picker and ticket worktrees
  (SESSION-5), the `n new` row, outside sessions (SESSION-6), notifications (SESSION-7),
  rebinding the reserved chords (SESSION-9), `z`/`alt+z` zoom.

## Design direction

The right column with two hosted sessions, the second focused:

```
╭ main view ───────────────────────────────╮
│ <ticket detail, doc or transcript>       │
╰──────────────────────────────────────────╯
╭ dock ─ ∼ lazyclaude-3f │ • Fix status ───╮
│ <claude's screen, cell for cell>         │
│                                          │
╰──────────────────────────────────────────╯
```

- One tab per hosted session, in start order: its marker in colour, then its title, or
  `starting`. The focused tab is highlighted, the frame accented while the dock has focus.
- With the main view hidden, the dock takes the whole right column.
- Sessions panel row: `∼ lazyclaude-3f  pane`.
- Quit prompt on the status line: `2 hosted sessions are busy or waiting — quit? y/n`.
- Keys: `N` new unlinked, `enter` on a hosted row focuses its tab and on an unlinked past row
  resumes it, `m`/`alt+m` hide the main view, `alt+h/j/k/l` move as in the criteria, `q`
  quits with the prompt above. The key bar over a focused tab lists only the reserved chords.

## Glossary impact

- **Hosted session** and **Starting** drop their *(expected)* marker. **Hosted session**
  gains: started by `N` or by resuming an unlinked past session; its tab closes when `claude`
  exits.
- Adds **Dock**: the bottom of the right column, holding every hosted session as a tab.
- Adds **Dock tab**: one hosted session's pane in the dock, labelled with its marker and title.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-7](LAZY-7-handing-over-to-a-session.md), [LAZY-14](LAZY-14-embedding-claude-on-a-pty.md), ADR 0007, ADR 0008. pty4j and jediterm-core come from the JetBrains repo, not Central.
