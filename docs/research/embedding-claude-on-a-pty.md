# Embedding the real `claude` TUI on a pty inside a TamboUI pane

Researched 2026-10-09 against **pty4j 0.14.0**, **jediterm-core 3.76**, TamboUI **0.5.0** and
Claude Code **2.1.294**, on Linux (CachyOS, JDK 25, kitty).

Sources, in order of trust:

- **jar**: the binaries and `-sources.jar` files. pty4j comes from Maven Central
  (`repo1.maven.org/maven2/org/jetbrains/pty4j/pty4j/0.14.0/`). jediterm-core is **not** on
  Maven Central. It comes from JetBrains' `intellij-dependencies` repository
  (`packages.jetbrains.team/maven/p/ij/intellij-dependencies/org/jetbrains/jediterm/jediterm-core/3.76/`,
  published 2026-09-09). TamboUI comes from Maven Central. File references below are paths
  inside those sources jars.
- **exp**: `Exp.java`, a single-file program that spawns a command on pty4j, feeds jediterm-core,
  types scripted keys, dumps the cell grid, and measures the cost of copying the grid into a
  TamboUI `Buffer`. It was run against `bash`, `less` and the real `claude`. No prompt was sent,
  so the model was never called (see [Experiments](#experiments)).
- **docs**: `code.claude.com/docs/en/` (`sessions`, `cli-reference`, `fullscreen`,
  `interactive-mode`, `terminal-config`, `settings-reference`, `env-vars`).
- **gh**: `github.com/JetBrains/pty4j`, `github.com/JetBrains/jediterm` (READMEs, repo
  metadata), `github.com/tamboui/tamboui` issues, `github.com/a-kenji/tui-term`, and
  `codeberg.org/AutumnMeowMeow/jexer` / `github.com/crramirez/casciian`.

## Summary

| Need | Verdict | What to use |
|---|---|---|
| Spawn a child on a pty, with resize | **Supported** | pty4j 0.14.0: `PtyProcessBuilder`, `PtyProcess.setWinSize(WinSize)` |
| Headless emulator with a cell grid | **Supported**, with small gaps | jediterm-core 3.76: `JediEmulator` + `JediTerminal` + `TerminalTextBuffer` |
| Copy the grid into a TamboUI `Element` | **Supported**, cheap | a custom `Element` that writes `TerminalLine` runs with `buffer.setString`: 0.1–0.9 ms per 200×50 frame |
| Pass keys from TamboUI to the pty | **Partial**: TamboUI loses some chords and owns Tab/Shift+Tab | re-encode `KeyEvent`s. The pane must take Tab/Shift+Tab back from the bindings. Bracketed paste and mouse capture must be turned on |
| `claude --resume <id>` from another cwd | **Supported** (verified) | works from any directory since v2.1.223, and the session runs in the launch cwd |
| Prior art on the JVM | **Exists**, not for TamboUI | IntelliJ's terminal (pty4j + JediTerm, Swing), Jexer/Casciian `TTerminalWindow`, ratatui's `tui-term` |

The pipeline works end to end. In the experiment, the real `claude` ran on pty4j and jediterm
drew its fullscreen UI correctly. Shift+Tab, bracketed paste, a resize and Ctrl+C all reached
it, and it re-rendered at the new size and exited cleanly. The open work sits on the TamboUI side
(input fidelity and config), not on the pty or the emulator.

## pty: pty4j: supported

- **Coordinates**: `org.jetbrains.pty4j:pty4j:0.14.0` on Maven Central. Licence **EPL-1.0**.
  The repo was last pushed on 2026-10-02 and Maven metadata was updated the same day. It is the
  pty layer of every JetBrains IDE terminal. [jar: POM; gh]
- **Dependencies**: `kotlin-stdlib` 2.4.20 (compile scope), with JNA 5.19.1, `jna-platform`,
  `slf4j-api` and `annotations` at runtime scope. Together that is about 7.5 MB of jars,
  2.8 MB of it pty4j itself. [jar: POM]
- **Native code**: the jar bundles a small `libpty.so` per OS and arch under
  `resources/com/pty4j/native/linux/{x86-64,aarch64,arm,x86,ppc64le,riscv64,mips64el}/`, plus
  FreeBSD, macOS and Windows (winpty/ConPTY). It extracts the library to a temp dir at
  runtime, or to a dir named by `-Dpty4j.tmpdir` / `pty4j.preferred.native.folder`
  (`util/ExtractedNative.java`, `util/PtyUtil.java`). Everything else goes through JNA. On
  JDK 25, add `--enable-native-access=ALL-UNNAMED`, or put `Enable-Native-Access: ALL-UNNAMED`
  in the shaded jar's manifest, to avoid the restricted-method warning. [jar]
- **API**: `new PtyProcessBuilder(String[]).setEnvironment(map).setDirectory(dir)
  .setInitialColumns(c).setInitialRows(r).start()` returns a `PtyProcess`, which is a
  `java.lang.Process`. Its `getInputStream()` and `getOutputStream()` are the pty master.
  [jar: `PtyProcessBuilder.java`]
- **Resize**: `PtyProcess.setWinSize(new WinSize(cols, rows))` calls the native
  `set_window_size` (`unix/NativePtyExecutor.java`), which is `TIOCSWINSZ`. The kernel then sends
  `SIGWINCH` to the child's foreground process group. Verified: after a resize from 120×40 to
  80×24, a bash `trap … WINCH` printed `GOT-WINCH` and `tput` reported `80x24`. `claude`
  re-laid out its UI at 90×30. [jar; exp]
- **Environment**: the child inherits whatever map you pass. Set `TERM=xterm-256color` and
  `COLORTERM=truecolor`. **Scrub** `CLAUDECODE`, `CLAUDE_CODE_*`, `KITTY_*`, `TERMINFO` and
  `TMUX`. Otherwise the inner `claude` believes it is running inside kitty, tmux or another
  Claude session, which matters for LAZY-2's registry and for notifications. [exp]

## Emulator: jediterm-core: supported, with small gaps

- **Coordinates**: `org.jetbrains.jediterm:jediterm-core:3.76`, only from
  `https://packages.jetbrains.team/maven/p/ij/intellij-dependencies` (Maven Central returns
  404). The pom needs that `<repository>`. Licence: the POM says LGPL-3.0, and the README says
  "dual-licensed under both the LGPLv3 and Apache 2.0 … You may select, at your option". Choose
  **Apache 2.0**. Dependencies: `kotlin-stdlib` 2.4.0, `slf4j-api`, `annotations`. The jar is
  280 KB. [jar: POM; gh README]
- **No Swing needed**: `jediterm-core` is the UI-less half (`jediterm-ui` holds the Swing
  widget). Only two files mention `java.awt`: the deprecated `TtyConnector.resize(Dimension)`
  default methods and `TerminalStarter`. Running on a full JDK is fine. A jlinked runtime would
  need `java.desktop`. [jar]
- **Wiring** (as done in `Exp.java`):
  `TerminalTextBuffer(cols, rows, StyleState)`, then `JediTerminal(TerminalDisplay, buffer, style)`,
  then `JediEmulator(new TtyBasedArrayDataStream(TtyConnector), terminal)`. A dedicated thread
  loops `while (em.hasNext()) em.next()`. `terminal.setTerminalOutput(TerminalOutputStream)`
  carries the emulator's replies (DA, DSR, OSC 11 colour query) back to the pty.
  `TerminalDisplay` is a 15-method callback interface: alt screen, mouse mode, bracketed
  paste, title, cursor shape and visibility. `TerminalStarter` packages this loop, but it wants a
  `TerminalExecutorServiceManager` and isn't needed. [jar; exp]
- **The cell grid**: `buffer.getLine(y)` returns a `TerminalLine` of `TextEntry` runs
  (`CharBuffer` text + `TextStyle`). `TextStyle` has `getForeground()`/`getBackground()` as
  `TerminalColor`: `isIndexed()` + `getColorIndex()`, or `toColor()` for RGB. Indices 16–255
  come back already converted to RGB. Options are `BOLD, ITALIC, DIM, INVERSE, UNDERLINED,
  SLOW_BLINK, RAPID_BLINK, HIDDEN`. A wide character occupies its cell followed by the marker
  `CharUtils.DWC` (``). The cursor is `terminal.getCursorX()/getCursorY()`, 1-based.
  Reading and resizing must hold `buffer.lock()` / `buffer.modify(..)`, because the emulator
  thread writes concurrently. [jar]
- **Verified rendering**: truecolor (`38;2;255;100;0`), 256-colour, bold+underline, CJK
  (`你好`), an emoji (`😀`), alternate screen in and out (`less` drew in the alt buffer, and the
  main screen was intact after `q`), reflow of a wrapped line on resize, and `claude`'s whole
  fullscreen UI: logo, prompt box, status line, Shift+Tab mode change, and a two-line bracketed
  paste in the input. [exp]

### What `claude`'s Ink UI emits, and how jediterm copes

A byte-level census of `claude` 2.1.294 starting in its fullscreen renderer (the default for
this account, see [fullscreen docs](https://code.claude.com/docs/en/fullscreen#fullscreen-by-default)),
then Shift+Tab, a paste, a resize and Ctrl+C. [exp; jar: `JediEmulator.setModeOrPrivateMode`,
`doProcessOsc`, `processControlSequence`]

| Sequence | Meaning | jediterm 3.76 |
|---|---|---|
| `CSI ?1049 h/l` | alternate screen | handled |
| `CSI ?1000/1002/1003 h`, `CSI ?1006 h` | mouse tracking, SGR encoding | handled: reported through `TerminalDisplay.terminalMouseModeSet` / `setMouseFormat`. Forwarding mouse input is the host's job |
| `CSI ?2004 h/l` | bracketed paste | handled (`setBracketedPasteMode`) |
| `CSI ?1004 h/l` | focus in/out reports | accepted as a stub: no focus events are sent |
| `CSI ?25 h/l`, `ESC 7/8`, `CSI r`, `CSI H/G/B/C/K/J` | cursor, scroll region, erase | handled |
| SGR `38;2;r;g;b` (semicolon form) | truecolor | handled. The colon form `38:2::r:g:b` is **not** parsed (`ControlSequence` treats `:` as unhandled), but `claude` doesn't emit it |
| `OSC 0` | window title (`✳ Claude Code`) | handled (`setWindowTitle`), useful to show waiting state |
| `OSC 11 ?` | background colour query | answered from `TerminalDisplay.getWindowBackground()` |
| `CSI c` | primary DA | answered `ESC[?6c` |
| `CSI ?u` | kitty keyboard protocol query | ignored, no reply, so `claude` falls back to legacy keys |
| `CSI >0q` | XTVERSION query | **mis-parsed** as DECSCUSR (cursor shape → blinking block) and not answered. Cosmetic |
| `CSI ?2031 h/l`, `CSI ?1016 l` | colour-scheme notifications, SGR-pixel mouse | ignored, harmless |
| `CSI ?2026 h/l` | synchronized output | handled: the emulator buffers until the end mark, with a size and time cap (`SynchronizedOutput.kt`). `claude` did **not** send it here because it doesn't detect the terminal; `CLAUDE_CODE_FORCE_SYNC_OUTPUT=1` turns it on ([env-vars](https://code.claude.com/docs/en/env-vars)) |
| `OSC 8` | hyperlinks | handled (`setLinkUriStarted`) |
| `OSC 52`, `OSC 9/777` notifications | clipboard, desktop notification | **not handled** (only OSC 0/1/2/7/8/10/11/104/1341). Copy-on-select inside the pane won't reach the system clipboard unless lazyclaude implements OSC 52 itself |

## Rendering onto a TamboUI `Element`: supported, cheap

- Same shape as LAZY-4's viewport element: a custom `Element` whose `render(Frame, Rect,
  RenderContext)` locks the `TerminalTextBuffer`, walks `getLine(y).getEntries()` for the
  visible rows, and writes each run with `frame.buffer().setString(x, y, text, style)`. Map
  `TerminalColor` to `Color.indexed(n)` / `Color.rgb(r,g,b)`, and `TextStyle` options to
  `Style.bold()/dim()/italic()/underlined()/reversed()`. Skip the `DWC` marker. TamboUI's
  `setString` does the width maths. Put the cursor with `frame.setCursorPosition(x, y)` while
  the pane is focused and `CursorVisible` is on. [jar: `Frame.java`; exp]
- **Cost** (exp `perf`, JIT-warmed, 5,000 iterations, 200×50 grid):
  - plain text, two styles per line: **0.11 ms per frame**;
  - worst case, every one of the 10,000 cells a distinct truecolor: **0.88 ms per frame**.
  So copying a full screen is negligible next to TamboUI's 40 ms tick.
- Emulation itself runs on the reader thread, not the render thread. 190k characters of
  worst-case SGR took about 20 ms cold. The UI must then be told to redraw. With ticks on
  (TamboUI's default), the next tick picks the change up. With `noTick()`, dispatch a
  `TickEvent` from a `TerminalModelListener`/`TextBufferChangesListener`, debounced (LAZY-4's
  redraw gotcha).
- **Resize**: when the pane's `Rect` changes, call `proc.setWinSize(..)` and
  `buffer.modify(() -> terminal.resize(new TermSize(c, r), RequestOrigin.User))`. Do it when
  the size changes, not on every frame.

## Input: partial

TamboUI parses stdin into `KeyEvent`/`MouseEvent`/`PasteEvent` before any element sees it
(`dev/tamboui/tui/event/EventParser.java`). A terminal pane has to re-encode those events into
bytes for the pty. jediterm's `JediTerminal.getCodeForKey(vk, modifiers)` (`TerminalKeyEncoder`)
does that for special keys and respects application-cursor mode (DECCKM). Printable characters
and Ctrl letters are written as UTF-8 or control bytes.

**What arrives intact** (verified by sending the bytes straight to `claude` [exp], and checked
in the parser for TamboUI's side [jar]):

- **Ctrl+C**: JLineBackend turns `ISIG` off (`JLineBackend.java` L139–141), so it arrives as
  byte 3, which becomes `KeyEvent('c', CTRL)`. `ToolkitRunner.handleEvent` checks `isQuit()`
  **only if no element handled the event**, so a focused pane that returns `HANDLED` keeps
  lazyclaude alive and can forward `0x03`. In the experiment, Ctrl+C cleared the input and two
  more presses quit `claude`, exit 0.
- **Esc**: arrives as `KeyCode.ESCAPE` after a 50 ms peek (`PEEK_TIMEOUT`). Esc followed by a
  key within 50 ms becomes Alt+key. Forward `0x1b`.
- **Ctrl+R, Ctrl+O, Ctrl+G, Ctrl+B, Ctrl+T, Ctrl+S, Ctrl+L, Ctrl+V, Ctrl+_**: all become
  `Ctrl+<letter>` and encode back to the same byte.
- **Alt+P / Alt+T / Alt+O / Alt+B / Alt+F**: `ESC <char>` is parsed as `ALT`, and re-encodes as
  `ESC <char>`.
- **Arrows, Home/End, PgUp/PgDn, F-keys, Delete**, with modifiers in `CSI 1;m X` form.
- **Paste**: a `PasteEvent` with the whole text. Forward it as `ESC[200~ … ESC[201~` when the
  child has 2004 on. That is what `claude` received as a two-line paste in the experiment. But
  TamboUI's `TuiConfig.defaults()` has **`bracketedPaste` off** (and `mouseCapture` off). Without
  it, a paste arrives as keystrokes, and its newlines become Enter, which **submits the prompt**.
  lazyclaude must build its `TuiConfig` with bracketed paste on. [jar: `TuiConfig.java`
  L180–182]

**What TamboUI swallows or loses:**

- **Tab and Shift+Tab** are taken by `EventRouter.routeKeyEvent` for `focusNext` /
  `focusPrevious` *before* the focused element is asked (as LAZY-4 found). Shift+Tab is how
  Claude Code cycles permission modes, and Tab completes paths and commands. Fix: load custom
  `Bindings` (`BindingSets.load(..)`, `TuiConfig.builder().bindings(..)`) with
  `focusNext`/`focusPrevious` unbound or bound to another key, and move focus from a global
  handler when the pane is not focused. [jar]
- **`q` / `Q`** are `quit` in the standard bindings, but quit only fires when nothing handled
  the key, so a pane that handles every key is safe.
- **Ctrl+J and Enter collapse**: bytes 10 and 13 both become `KeyCode.ENTER`
  (`parseControlChar`). Ctrl+J is Claude Code's "works in any terminal" newline
  ([interactive-mode](https://code.claude.com/docs/en/interactive-mode)). Likewise Ctrl+H
  becomes Backspace and Ctrl+I becomes Tab. `\` + Enter and paste still give newlines.
- **Shift+Enter, Ctrl+Enter, Alt+Enter, Ctrl+Backspace**: TamboUI doesn't enable the kitty
  keyboard protocol (upstream #446 open), so kitty sends legacy bytes where Shift+Enter is a
  plain `\r`. `ESC \r` (Alt+Enter) is parsed as `UNKNOWN`. Shift+Enter (newline) and Ctrl+Enter
  (send queued messages now) are lost inside the pane.
- **Ctrl+Z**: forwarding `0x1a` makes `claude` suspend itself with `SIGTSTP` (documented
  "Suspend Claude Code … run `fg`"). Inside lazyclaude there is no shell to `fg` it, so the
  pane must intercept Ctrl+Z, or `SIGCONT` the child. *Not verified.*
- **Mouse**: `claude`'s fullscreen renderer turns on mouse tracking (1000/1002/1003 + SGR) for
  wheel scrolling, click-to-expand and selection. TamboUI only captures the mouse if
  `TuiConfig` enables it, and a `MouseEvent` would have to be re-encoded as SGR
  (`ESC[<b;x;yM`) relative to the pane origin. jediterm's `TerminalMouseEventEncoder` takes its
  own `MouseEvent` type, so a small adapter is needed. Alternative: start `claude` with
  `CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1` for the classic renderer, which needs no mouse but
  loses the fixed input box ([fullscreen](https://code.claude.com/docs/en/fullscreen)).
- **Lossless option (not tried)**: `TuiConfig.builder().backend(Backend)` accepts any
  `Backend` (`TuiRunner.create` L192–197). A decorator around `JLineBackend` that records the
  raw bytes behind each `read()` would let a focused pane forward exactly what the outer
  terminal sent: kitty's own sequences, Ctrl+J, Alt+Enter. The cost is that the pane would
  have to undo `peek()` lookahead.

## Resume cwd: from anywhere, verified

- Docs: "You can run `claude --resume <session-id>` from any directory … Claude Code looks
  for the ID in this order: 1. The current project directory and its git worktrees 2. Every
  other project on this machine. The cross-project search resolves the ID only when exactly
  one other project holds a transcript with messages for it … Before v2.1.223, the lookup
  stopped at the current project directory and its git worktrees."
  ([sessions](https://code.claude.com/docs/en/sessions#resume-a-session);
  [cli-reference](https://code.claude.com/docs/en/cli-reference) `--resume`). Resuming **by
  name** resolves only across the current repository and its worktrees. The interactive
  picker, for a session from an unrelated project, copies a `cd … && claude --resume` to the
  clipboard instead.
- **Verified** with 2.1.294: a `claude-tools` session (`eb663577…`, recorded with
  `cwd=/home/max/projects/claude-tools`) was resumed with `claude --resume <id>` launched from
  `/home/max/projects/next-suggestions`. It loaded the conversation, and the header showed
  `~/projects/next-suggestions`. **The resumed session runs in the launch cwd, not the recorded
  one.** So lazyclaude must launch `claude --resume` *in the ticket's worktree* (LAZY-7), or the
  agent works in the wrong checkout. [exp]
- Side effect seen: resuming without sending anything still appends `cost-state`, `mode` and
  `permission-mode` lines (cost 0) to the transcript and bumps its mtime. Mtime-based "last
  activity" (LAZY-2) should not trust a bare resume. The test transcript was restored from a
  backup afterwards. [exp]
- Related: a new directory such as a fresh `../<repo>.worktrees/<id>-<slug>` gets the
  "Accessing workspace … Is this a project you trust?" dialog on first launch (seen for
  `/home/max/projects/lazyclaude`). It renders fine in the pane. [exp]

## Prior art

- **IntelliJ's terminal** is pty4j + JediTerm, drawn in Swing. It is the production user of both
  libraries. [gh]
- **Jexer** (`codeberg.org/AutumnMeowMeow/jexer`, MIT; last on Maven Central as
  `com.gitlab.klamonte:jexer:1.6.0`, 2022) and its fork **Casciian**
  (`github.com/crramirez/casciian`, Apache-2.0, active 2026-10). Both are Java text-UI
  frameworks with their own ECMA-48 emulator and a `TTerminalWindow` that runs a shell inside
  the TUI. Casciian ships it as a separate `casciian-terminal-component` module. This is the
  closest JVM prior art, but it's a whole TUI framework, not something to plug into TamboUI.
  [gh READMEs]
- **tui-term** (`github.com/a-kenji/tui-term`, MIT, active): a pseudoterminal widget for
  ratatui, the Rust library TamboUI mirrors. It uses the `vt100` crate for the screen and
  `portable-pty`. Same architecture as proposed here. [gh]
- TamboUI itself has no terminal widget, and no open issue or PR proposes one (searched
  `pty`, `terminal emulator`, `embed terminal`). [gh]

## Experiments

Kept outside the repo, in the session scratchpad (`scratchpad/pty/`): `src/Exp.java`, run with
`java --enable-native-access=ALL-UNNAMED -cp 'lib/*:tamboui-core-0.5.0.jar' Exp.java <mode> <cwd> <outdir> <cmd…>`.

- `bash`: truecolor, 256 colours, CJK and emoji, a resize to 80×24 with the `SIGWINCH` trap,
  and `less` in and out of the alternate screen.
- `claude` in `/home/max/projects/lazyclaude` (trust dialog) and in
  `/home/max/projects/next-suggestions`, both with the default renderer and with
  `CLAUDE_CODE_NO_FLICKER=1` (both fullscreen): startup, Shift+Tab, a bracketed two-line paste,
  a resize to 90×30, and Ctrl+C ×3 to exit (exit 0). No prompt was submitted, and no transcript
  was created (the transcript count was unchanged).
- `resume`: `claude --resume eb663577-…` from another project, then two Ctrl+C presses. Cost 0.
  The appended metadata lines were reverted from a backup.
- `perf`: the cost of copying the grid into a TamboUI `Buffer`, figures above.
