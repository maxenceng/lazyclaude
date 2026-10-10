---
id: LAZY-14
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-4, LAZY-7]
---

# Can the real `claude` TUI be embedded on a pty inside a TamboUI pane?

## Question

Can the real `claude` TUI be embedded on a pty inside a TamboUI pane?

LAZY-7 decided that hosted sessions run inside lazyclaude. The plan is to spawn `claude` on a pty, feed its output to a headless terminal emulator, and draw the emulator's screen as a TamboUI `Element`.

Find out:
- **pty:** which Java library spawns a child on a pty on Linux. Candidates: pty4j (JetBrains), anything else maintained. Check its licence, its native bits, and whether it supports resize (`SIGWINCH`).
- **Emulator:** which headless VT/xterm emulator exposes a cell grid (characters, colours incl. truecolor, attributes, cursor). Candidates: jediterm-core, alternatives. Check how well it copes with what `claude`'s Ink UI emits (alternate screen, bracketed paste, mouse modes, synchronized output, OSC sequences).
- **Rendering:** mapping that grid onto a TamboUI `Element` (LAZY-4's viewport approach), and the cost per frame for a full screen.
- **Input:** passing keys from TamboUI through to the pty, including chords Claude Code relies on (Ctrl+C, Esc, Shift+Tab, Ctrl+R, paste), and what TamboUI swallows before the app sees it.
- **Resume cwd:** whether `claude --resume <id>` finds a session only when launched from the cwd its transcript was recorded under, or from anywhere.
- Whether there's prior art for embedding a terminal in a JVM TUI.

## Kind

research

## Resolution

Yes. The real `claude` ran on pty4j with jediterm-core and rendered correctly. The open work
is on TamboUI's input side. Details, API names and experiments:
[`docs/research/embedding-claude-on-a-pty.md`](../research/embedding-claude-on-a-pty.md).

- **pty — supported.** `org.jetbrains.pty4j:pty4j:0.14.0` (Maven Central, EPL-1.0, JNA plus a
  bundled `libpty.so` per arch). `PtyProcess.setWinSize` does `TIOCSWINSZ` and the child gets
  `SIGWINCH` (verified). Needs `--enable-native-access`. Scrub `CLAUDE*`/`KITTY*`/`TMUX` from
  the child's env.
- **Emulator — supported, small gaps.** `jediterm-core:3.76`, which is only in JetBrains'
  `intellij-dependencies` repo, not Maven Central (Apache-2.0/LGPL dual). It is headless and
  exposes `TerminalTextBuffer` lines of styled runs, truecolor, wide chars and the cursor. It
  copes with `claude`'s fullscreen UI: alt screen, mouse modes, bracketed paste, OSC 0/8/11.
  Gaps: no OSC 52 or notification OSCs, focus reports are a stub, the kitty-keyboard query goes
  unanswered, and XTVERSION is mis-read as a cursor shape (cosmetic).
- **Rendering — supported, cheap.** A custom `Element` writes the visible runs with
  `setString`. A 200×50 frame costs 0.11 ms (plain) to 0.88 ms (every cell a distinct
  truecolor). Resize the pty and buffer only when the pane's size changes.
- **Input — partial.** Ctrl+C, Esc, Ctrl+R and other Ctrl/Alt letters, arrows and paste all
  re-encode. Shift+Tab and paste were verified against `claude`. TamboUI takes Tab/Shift+Tab for
  focus before the pane sees them, so they need custom `Bindings`. Bracketed paste and mouse
  capture are off by default and must be turned on, or a paste's newlines submit the prompt.
  Ctrl+J/Enter, Shift/Ctrl/Alt+Enter are lost (no kitty protocol, upstream #446). Ctrl+Z would
  stop the child (unverified). A raw-byte `Backend` decorator could make input lossless
  (untried).
- **Resume cwd — supported (verified).** `claude --resume <id>` finds the session from any
  directory (since v2.1.223), and the session then runs in the *launch* cwd. So lazyclaude must
  launch it in the ticket's worktree. A bare resume still appends metadata lines to the
  transcript.
- **Prior art — exists, not for TamboUI.** IntelliJ's terminal (pty4j + JediTerm, Swing),
  Jexer/Casciian's `TTerminalWindow`, and ratatui's `tui-term`.
