# Mouse support in TamboUI and in the hosted pty

Researched 2026-10-26 against TamboUI **0.5.0** (and its successor **0.6.0**, released
2026-10-12), jediterm-core **3.76**, Claude Code **2.1.310**, on Linux (CachyOS, JDK 25, kitty).

Sources, in order of trust:

- **jar**: the 0.5.0 and 0.6.0 `-sources.jar` files of `tamboui-tui`, `tamboui-toolkit`,
  `tamboui-core` and `tamboui-jline3-backend` from Maven Central, and `jediterm-core-3.76-sources.jar`.
  File references below are paths inside those sources jars.
- **code**: lazyclaude's own `shell` and `session.infrastructure.secondary` sources as of this date.
- **exp**: small programs run against the 0.5.0 jars, and captures of the real `claude` on a pty
  (see [Experiments](#experiments)).
- **docs**: `code.claude.com/docs/en/fullscreen`, kitty's `kitty/options/definition.py`, VTE's
  `src/vte.cc` (the gnome-terminal widget), `learn.microsoft.com/en-us/windows/terminal/selection`.
- **gh**: `github.com/tamboui/tamboui` open pull requests #480, #517, #301 and #397, and issue #287.

## Summary

| Question | Answer | What to use |
|---|---|---|
| Turn reporting on | **Supported**, off by default | `TuiConfig.builder().mouseCapture(true)` (lazyclaude has it on); `.mouseMotion(true)` adds hover |
| Event model | **Supported**: press, release, drag, move, four wheel directions, three buttons, Ctrl/Alt/Shift, 0-based cells | `dev.tamboui.tui.event.MouseEvent` |
| Double-click | **Missing**: no click count in TamboUI or in SGR | time two presses on the same cell yourself |
| Routing to elements | **Partial**: only left press, move and wheel up/down reach elements, outermost first | a global handler sees every event first; prefer it |
| Hit-testing a click to what is drawn there | **Element level only**: the element's `Rect`, never the row or span inside it | lazyclaude keeps its own geometry, captured at render |
| `claude` asks for the mouse | **Yes, verified**: `?1000h ?1002h ?1003h ?1006h` on entering the alternate screen | already forwarded as SGR by `MouseBytes` |
| Click versus selection inside the pane | **`claude` decides**: it does its own selection and copy | forward press, drag and release unchanged |
| Selection across the whole screen | **The outer terminal's bypass modifier**: Shift in kitty, VTE, Windows Terminal; Option in iTerm2 | nothing to build |

Mouse forwarding to a hosted `claude` is already built (ADR 0026) and works as designed. Two gaps
in it follow from this research: hover never reaches `claude`, and a drag that leaves the pane
never sends its release. For lazyclaude's own panels, TamboUI gives events and coordinates but not
a usable hit-test, so mouse support there is lazyclaude's own geometry plus the shell's global handler.

## Turning reporting on

- `TuiConfig` carries `mouseCapture` and `mouseMotion`, both `false` in `TuiConfig.defaults()`.
  `TuiRunner` calls `backend.enableMouseCapture(config.mouseMotion())` on start and
  `disableMouseCapture()` on close. [jar: `tui/TuiConfig.java`, `tui/TuiRunner.java:209,782`]
- `JLineBackend.enableMouseCapture(motion)` writes `CSI ?1000h` (normal), `?1002h` (button-motion),
  `?1003h` only when `motion` is true, then `?1015h` (urxvt) and `?1006h` (SGR). SGR is written last,
  so a terminal that honours both reports in SGR. [jar: `backend/jline3/JLineBackend.java:169`]
- lazyclaude sets `mouseCapture(true)` and does not set `mouseMotion`, so the outer terminal is
  asked for 1000/1002/1006 but not 1003: no `MOVE` event is ever reported. [code: `Shell.config()`]
- `FocusReportingBackend` (ADR 0034) forwards `enableMouseCapture()`, `enableMouseCapture(boolean)`
  and `disableMouseCapture()`. It strips only `ESC [ I` and `ESC [ O`. An SGR report `ESC [ < …`
  is pushed back unread, so focus reporting and mouse reporting do not interfere. [code;
  `FocusReportingBackendTest`]
- 0.6.0 changes none of this: `routeMouseEvent`, `parseMouseParams` and `enableMouseCapture` are
  identical to 0.5.0. [jar 0.6.0, diffed]

## The event model

- `MouseEvent(kind, button, x, y, modifiers)`: `MouseEventKind` is `PRESS`, `RELEASE`, `DRAG`,
  `MOVE`, `SCROLL_UP`, `SCROLL_DOWN`, `SCROLL_LEFT`, `SCROLL_RIGHT`. `MouseButton` is `LEFT`,
  `MIDDLE`, `RIGHT` or `NONE` (wheel and move). `x`/`y` are 0-based screen cells.
  `KeyModifiers` gives `ctrl()`, `alt()` and `shift()`. [jar: `tui/event/MouseEvent.java`]
- `EventParser` decodes **SGR only** (`ESC [ < b ; x ; y M|m`). It reads the modifier bits 4/8/16,
  takes 64–67 as the wheel, and 32 as motion (a drag when a button is held, a move when none is).
  `m` is a release. [jar: `tui/event/EventParser.java:384-482`]
- **X10/normal encoding (`ESC [ M Cb Cx Cy`) is not decoded in 0.5.0 or 0.6.0.** Where a terminal
  ignores 1006 (the Linux console, GNU screen 4), every click types its coordinate bytes. PR #517
  (open) shows a click in column 81 typing `q`. Outside a tab, `q` is lazyclaude's quit key.
  [jar; gh #517]
- **No click count.** Neither `MouseEvent` nor SGR carries one; a double-click is two
  `PRESS`/`RELEASE` pairs. `Actions.CLICK` is bound to `Mouse.Left.Press` and `RIGHT_CLICK` to
  `Mouse.Right.Press` (`bindings/standard.properties`). `MouseTrigger` offers `click`,
  `rightClick`, `ctrlClick`, `shiftClick`, `altClick`, `drag(button)` and the four wheels, with
  no double-click. [jar: `tui/bindings/MouseTrigger.java`]
- Drag support in the toolkit is `StyledElement.onDrag(DragHandler)` / `draggable(...)`. It is
  started by a left press on that element, and the `EventRouter` then sends it every `DRAG` and
  the `RELEASE`. [jar: `toolkit/event/EventRouter.java:378-397`]

## How events reach elements, and hit-testing

**The order.** `ToolkitRunner.handleEvent` calls `EventRouter.route`. For a `MouseEvent` the
router runs the **global handlers first** (`addGlobalHandler`), and only if none handles it does it
call `routeMouseEvent`. lazyclaude's shell is such a global handler, so it sees every mouse event,
of every kind, before any element does. [jar: `EventRouter.route:190-195`; code: `Shell.onStart`]

**What `routeMouseEvent` does.** [jar: `EventRouter.java:378-499`; exp `Route.java`, `Route2.java`]

- Elements are registered by area during render: `StyledElement.render` after drawing its content,
  `DefaultRenderContext.renderChild` after each child, and the root last. The list is cleared on
  every frame.
- Only a **left `PRESS`**, `MOVE`, `SCROLL_UP` and `SCROLL_DOWN` are routed to elements. A right or
  middle press, a `RELEASE`, a `DRAG` with no draggable element, and `SCROLL_LEFT`/`SCROLL_RIGHT`
  reach no element: `route` returns `UNHANDLED` (verified for right press, release, drag and
  scroll-right).
- The list is walked in reverse registration order, which, since a parent registers after its
  children, means **outermost first**. A focusable `panel` around a focusable `text`: clicking the
  text focused the **panel** (verified). A container's `handleMouseEvent` forwards to children whose
  `renderedArea()` contains the point, so a nested handler can run more than once per click.
  Open PR #480 fixes this and is not in 0.6.0: "clicks land on the container or the bottom layer,
  not the element under the pointer… Right clicks, releases and horizontal scroll never reached
  elements".
- A left press on a focusable element with an id moves TamboUI's `FocusManager` focus there. A left
  press on no element clears that focus. lazyclaude keeps its own `Focus` and declares no
  `focusable()` element, so this has no effect on it.
- Built-in mouse behaviour is wheel-only: `ListElement` and `TreeElement` move the selection by 3
  on the wheel, and `ScrollableElement` and `RichTextAreaElement` scroll. **Clicking a list or tree
  row does not select it** (verified: `listSelected` stayed 0); no element handles a press.
  [jar: `ListElement.handleMouseEvent:866`, `TreeElement:828`]

**Hit-testing.** TamboUI maps a point to an element's rectangle and no further.

- The router's element list is private. The public lookups are `ElementRegistry.getArea(id)` and
  `query(cssSelector)`, through `ToolkitRunner.elementRegistry()`, for elements that carry an
  `.id(..)`. These give a `Rect` per element, not a reverse "what is at (x, y)". [jar: `ElementRegistry.java`]
- `StyledAreaRegistry` records the `Rect` of every span written with a `Tags` style extension,
  keyed by the enclosing element id (`Frame.setStyledAreaRegistry`). It exists for effects
  targeting. It is the closest thing to span-level hit-testing, unused by lazyclaude and not tried
  here. [jar: `core/style/StyledAreaRegistry.java`, `core/terminal/Frame.java:201`]
- A plain custom `Element`, which is how lazyclaude draws its rows, *is* registered by
  `renderChild` and receives a left press and the wheel through `handleMouseEvent` when it is under
  the pointer and nothing outside it took the event (verified: `Custom.java`, a rows element inside
  `column(panel(..))`). It still gets no release or right-click. Translating `y` into a row is the
  element's own job, from the area it was drawn in and its first visible row.
- lazyclaude already captures geometry this way for the dock pane: `PaneProbe` records the `Rect`
  on render, and `TerminalInputs.mouse(event, pane)` maps an event to a pane-relative cell.
  [code]

So lazyclaude must keep its own layout rectangles, row and tab offsets and scroll positions, and
hit-test in the shell. The global handler is the one place that gets every kind of event.
Overriding `handleMouseEvent` on its own elements works only for left presses and wheel up/down,
outermost first, until upstream changes the routing.

## Forwarding to a hosted `claude`

**`claude` turns the mouse on.** Captured from Claude Code 2.1.310 on a 120×40 pty, default
settings: after its startup queries it writes `ESC[?1049h`, then `?1000h ?1002h ?1003h ?1006h`:
all-motion tracking, SGR encoding. It also turns on `?2004h` (bracketed paste), `?1004h` (focus) and
`?2031h`. Environment switches, verified the same way and documented on the fullscreen page:

| Environment | Modes `claude` sets |
|---|---|
| default | 1049, 1000, 1002, 1003, 1006 |
| `CLAUDE_CODE_DISABLE_MOUSE_CLICKS=1` | 1049, 1000, 1006 (wheel only, no click/drag/hover) |
| `CLAUDE_CODE_DISABLE_MOUSE=1` | 1049, no mouse |
| `CLAUDE_CODE_DISABLE_ALTERNATE_SCREEN=1` | classic renderer, no alternate screen, no mouse |

[exp `cap.py`; docs: fullscreen]

**What `claude` does with it.** Inside its UI the mouse does click-to-position in the prompt,
choosing menu rows, click-to-expand of tool results, Ctrl+click to open a URL on Linux, the wheel,
and **its own text selection**. It handles click-drag, word on double-click and line on
triple-click itself, and copies on release with `wl-copy`/`xclip`/`xsel` locally, or OSC 52 over SSH.
Hover highlights rows, and list scrollbars appear only while the pointer is over them, which needs
1003. [docs: fullscreen "Use the mouse", "Keep native text selection"]

**How jediterm exposes it.** `JediEmulator` maps DECSET 1000/1001/1002/1003 to
`MouseMode.MOUSE_REPORTING_NORMAL/HILITE/BUTTON_MOTION/ALL_MOTION`, and the reset of any of them to
`NONE`. 1005/1006/1015 map to `MouseFormat.MOUSE_FORMAT_XTERM_EXT/SGR/URXVT` (reset: `XTERM`). Both
arrive through `TerminalDisplay.terminalMouseModeSet` and `setMouseFormat`. **1004 is a stub**:
accepted, no focus events. [jar: `terminal/emulator/JediEmulator.java:659-711`]
jediterm's own `TerminalMouseEventEncoder` takes its `com.jediterm.core.input.MouseEvent` and
counts **any Shift-held event as local** (`isRemoteMouseAction`: `mode != NONE && !isShiftDown()`).
That is the IntelliJ convention for telling a selection from a reported click. [jar:
`TerminalMouseEventEncoder.java:264`]

**How lazyclaude re-encodes.** Already built (ADR 0026). `ScreenDisplay` keeps the mode and
format, and reports `NONE` unless the format is SGR. `MouseBytes.of(mouse, mode)` writes
`ESC [ < code ; col+1 ; row+1 M|m`. It sends clicks and wheel for normal and hilite, adds drags for
button-motion, and adds moves for all-motion. The Shift/Alt/Ctrl bits are 4/8/16. `Shell.mouse`
forwards an event only while a tab is selected, the pane is drawn, and the point is inside the pane.
It does not use jediterm's encoder. [code: `MouseBytes`, `ScreenDisplay`, `TerminalInputs.mouse`,
`Shell.mouse`]

**Telling a click from a selection.** Inside the pane, lazyclaude does not decide: `claude` reads
press → drag → release as a selection and press → release as a click. A double-click is two press
pairs that arrive with their timing intact. What lazyclaude must keep is the sequence: every
`PRESS`, `DRAG` and `RELEASE`, in order, with modifiers. Over the whole screen (several panes, or
lazyclaude's own text), the outer terminal's bypass modifier does a native selection that never
reaches lazyclaude (next section).

**Gaps in the current forwarding, by code reading, not observed:**

- **No hover.** `claude` asks for 1003, but lazyclaude never asks the outer terminal for it
  (`mouseMotion` is off), so no `MOVE` ever reaches `MouseBytes`. Row hover and the hover-shown
  scrollbars in `claude`'s lists are lost. Turning `mouseMotion(true)` on would send a stream of
  `MOVE` events through the whole shell's global handler on every pointer move, and a move over
  the pane that is forwarded is reported as handled, which redraws.
- **A drag that leaves the pane loses its release.** `TerminalInputs.mouse` drops any event outside
  the pane, `DRAG` and `RELEASE` included, so a selection dragged past the pane's edge never sends
  its `m`. A real terminal keeps reporting a held button outside the window. Clamping the
  coordinates of a press that began inside the pane to the pane's edge would match that. Unverified
  what `claude` does with a press that is never released.
- **Focus.** `claude`'s `?1004h` lands on jediterm's stub, so the hosted `claude` never learns
  whether it is focused. This is unrelated to the mouse, but it is the same "the child asked and the
  host ignores it" shape.

## Terminal emulators

| Terminal | SGR 1006 | Native selection while an app has the mouse | Notes |
|---|---|---|---|
| kitty | yes | **Shift**+drag: default `mouse_map shift+left press grabbed mouse_selection normal` | `ctrl+shift+left` opens a URL even when grabbed; wheel maps can be per `grabbed`/`ungrabbed` |
| gnome-terminal (VTE) | yes | **Shift**+click starts a selection when mouse tracking is on | `vte.cc`: `if (m_mouse_tracking_mode != eNONE) { if (m_modifiers & GDK_SHIFT_MASK) start_selecting }` |
| Windows Terminal | yes, for VT-input apps | **Shift** ("If an application is in mouse mode, you can hold down Shift to make a selection") | see Windows below |
| iTerm2 | yes | **Option** | mouse reporting is a per-profile setting, *Enable mouse reporting*; OSC 52 copy is blocked by default |

[docs: kitty `definition.py:1333-1478`, `vte.cc:7486`, Microsoft *Selection*, Claude Code fullscreen]

- A native selection is by screen cells. It crosses lazyclaude's panes and borders, because the
  terminal knows nothing of them. That is the price ADR 0026 already names.
- Shift with the mouse rarely reaches lazyclaude in these terminals, since they keep Shift+left
  for themselves. A Shift bit in a forwarded event is therefore mostly theoretical.
- **Focus reporting interplay (ADR 0034).** Focus reports and SGR mouse reports are independent
  modes; both are on, and the backend separates them (above). Not checked: whether a click that
  focuses the outer window is also reported as a press, which differs per terminal.
- **Windows: unverified, likely no mouse at all in 0.5.0.** `JLineBackend` only writes the DECSET
  sequences. Open PR #301 adds `terminal.trackMouse(Terminal.MouseTracking.Any)` on Windows "to
  enable mouse events in CMD/PowerShell", and X10 decoding, because JLine translates native console
  mouse records to X10, which `EventParser` cannot read. Not tried on Windows.
- Terminals or multiplexers that ignore 1006 send X10, which 0.5.0 turns into typed characters
  (see the event model). tmux needs `set -g mouse on` to pass the wheel through (Claude Code docs).

## Upstream status (2026-10-26)

| Item | State | Effect when merged |
|---|---|---|
| PR #480: clicks reach the topmost element; every mouse kind routed | open | element handlers become a usable path for clicks, right-clicks and releases |
| PR #517: X10 mouse decoded, `ESC [ I` no longer swallows the next key | open | removes the X10 typed-characters hazard; overlaps ADR 0034's wrapper |
| PR #301: Windows mouse through JLine native tracking | open | mouse on Windows consoles |
| PR #397: `Scrollbar.handleMouseAction` (click-to-page, drag the thumb) | open | a scrollbar lazyclaude could hand events to |
| issue #287: selecting text with mouse capture on | open | no upstream answer yet |

None of these is in 0.6.0.

## Experiments

Kept outside the repo, under `/tmp/lazy27/`:

- `exp/Route.java`, `exp/Route2.java`: a focusable `panel` holding a focusable `text`, beside a
  `list`, rendered with `Frame.forTesting` into a 40×6 buffer through a `DefaultRenderContext`
  (render thread marked by reflection), then `EventRouter.route` fed each kind of event.
  Showed outermost-first focus, a handler on both panel and text running for one click,
  right press, release, drag and horizontal wheel `UNHANDLED`, and no row selection on click.
- `exp/Custom.java`: a plain `Element` with `handleMouseEvent`, inside `column(panel(..))`. It got
  the left press and the wheel, and not the release or the right press.
- `scripts/cap.py`: spawns `claude` on a 120×40 pty with lazyclaude's scrubbed variables removed and
  `TERM=xterm-256color`, reads 8 s of output, sends Ctrl+C twice, and lists every DEC private mode
  set or reset. It was run with default settings and with each of the three environment switches.
  No prompt was sent.
