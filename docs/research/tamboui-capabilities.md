# What TamboUI 0.5.0 offers lazyclaude v1, and where its gaps are

Researched 2026-10-09 against TamboUI **0.5.0** (Maven group `dev.tamboui`, released
2026-09-14). The project depends on `tamboui-toolkit` and `tamboui-jline3-backend`.

Sources, in order of trust:

- **jar**: the 0.5.0 binaries and `-sources.jar` files from Maven Central
  (`repo1.maven.org/maven2/dev/tamboui/<artifact>/0.5.0/`), read with `javap` and by reading the
  source. File references below are paths inside those sources jars.
- **exp**: small compile-and-run experiments against those jars (JDK 25, JLine 3.25.1), run
  under a real pseudo-terminal with `script(1)` and fed scripted keystrokes.
- **docs**: `https://tamboui.dev/docs/0.5.0/` (`markdown.html`, `api-levels.html`,
  `pilot-testing.html`).
- **gh**: `github.com/tamboui/tamboui` issues and the commits on `main` since the 0.5.0 tag.

## Summary

| Need | Verdict | What to use |
|---|---|---|
| Render markdown | **Supported** | `tamboui-toolkit-markdown`: `MarkdownElement.markdown(src).scroll(n)` |
| Multi-line text input | **Supported**, with two key caveats | `textArea(TextAreaState)` / `TextAreaElement` |
| Scroll content far larger than the screen | **Partial**: no virtualised list; write a small custom `Element` | `Element` interface, `ScrollbarElement` |
| Focusable lists and panels, lazygit-style | **Supported** | `.id(..).focusable()`, `list(..)`, `tree(..)`, `panel(..).focusedBorderColor(..)`, `FocusManager` |
| Suspend the TUI for a child process | **Missing** as an API; **workaround verified** | close the `ToolkitRunner`, run the child with `inheritIO()`, create a new runner |
| Off-render-thread work, results pushed back | **Supported** | `ToolkitRunner.runOnRenderThread`, `schedule*`, any executor |

## Markdown rendering: supported

- Two modules. `tamboui-markdown` holds the `MarkdownView` widget (core + widgets, CommonMark
  0.28.0 with GFM tables, task lists and strikethrough at runtime scope).
  `tamboui-toolkit-markdown` holds the DSL element `dev.tamboui.toolkit.markdown.MarkdownElement`
  (static `markdown(String)` / `of(String)`). Neither is in the project's pom today; v1 needs
  `tamboui-toolkit-markdown` (it brings `tamboui-markdown`). [jar: POMs; `MarkdownElement.java`]
- Renders headings (H1/H2 underlined), wrapped paragraphs, bullet/ordered/task lists, block
  quotes, fenced code in a rounded block with the info string as title and regex syntax
  highlighting, GFM tables (via `Table`), emphasis/strong/strikethrough/inline code, links as
  OSC-8 hyperlinks, horizontal rules. Inline/block HTML shows as dim escaped text; images as
  `[image: alt](url)`. [jar: `MarkdownView.java` javadoc; docs: `markdown.html`]
- Options: `.overflow(Overflow.WRAP_WORD)`, `.styles(MarkdownStyles)`,
  `.syntaxHighlighter(..)`, `.syntaxTheme(..)`, CSS properties `heading-1-color`,
  `link-color`, `blockquote-prefix`, … [jar]
- Scrolling is **external**: `.scroll(int firstRow)`; the element is not focusable and handles
  no keys, so the shell owns the offset and its keys. `MarkdownView.computeHeight(width)`
  gives the total row count for clamping and a scrollbar; the scroll offset is clamped so the
  viewport never goes blank (fix #393 is in 0.5.0). [jar: `MarkdownView.render`; gh #392/#393]
- **Cost**: every render re-parses and re-lays-out the whole document
  (`MarkdownParserHolder.parser().parse(..)` then `MarkdownLayout.layout(..)` in `render`).
  Measured: a ~2,200-line markdown document at 200×50, scrolled to row 3000, takes **~4.8 ms
  per frame**. Fine for ticket bodies and docs; something to watch for very large files,
  given ticks re-render ~25×/s by default (see below). [jar; exp `Perf.java`]
- Front-matter is not special-cased: a ticket's YAML block would render as markdown (a
  thematic break and a paragraph). The shell should split frontmatter off and render it
  separately. [jar: no frontmatter extension among the dependencies]

## Multi-line text input: supported

- `Toolkit.textArea(TextAreaState)` → `TextAreaElement`; state class
  `dev.tamboui.widgets.input.TextAreaState` (`text()`, `setText`, `insert`, `clear`,
  cursor/scroll accessors). Element options: `.placeholder`, `.title`, `.rounded()`,
  `.showLineNumbers()`, `.wrapWord()`/`.wrapCharacter()`/`.clip()`, `.highlighter(language)`,
  `.focusedBorderColor`, `.onTextChange(listener)`. Handles bracketed paste
  (`handlePasteEvent`). [jar: `TextAreaElement.java`, `TextAreaState.java`]
- It only takes keys when focused (needs `.id(..)`; focusable by default). Keys: arrows,
  Home/End, Backspace/Delete, **Enter inserts a newline**, printable chars. [jar:
  `handleTextAreaKey`]
- Caveat 1 — submitting: Enter is consumed, and Ctrl/Alt chords fall through unhandled, so a
  "submit" key must be a chord such as Ctrl+S bound through `.onKeyEvent(..)` on the element
  (the router tries the element's lambda handler after its built-in one returns
  `UNHANDLED`). Ctrl+Enter / Alt+Enter is not reliably distinguishable (gh #298, open; kitty
  keyboard protocol is gh #446, open). [jar: `EventRouter.routeKeyEvent`; gh]
- Caveat 2 — Tab: the router handles `focusNext` (Tab in the standard bindings) *before*
  routing to the focused element, so the text area's own Tab→4-spaces branch is unreachable;
  Tab leaves the field. Harmless for review comments. [jar: `EventRouter.java` L243–275,
  `standard.properties`]

## Scrolling content far larger than the screen: partial

There is no virtualised (windowed) list or text view in 0.5.0. What exists:

- `ListElement` (`list(..)`, `.data(List<T>, Function<T,StyledElement>)`) draws only the
  visible rows through `ListWidget`, but on every frame it builds an element per item and
  asks each for its `preferredSize` — O(n) per frame. Measured at 200×50: 1k items 1.1 ms,
  10k items 6.1 ms, 50k items 29.5 ms per frame. [jar: `ListElement.renderContent`,
  `adaptItemElement`; exp `Perf.java`]
- `ScrollableElement` (`scrollable(..)`) measures every child each frame too, and scrolls by
  whole children. [jar]
- `MarkdownElement` and `RichTextAreaElement`/`TextAreaElement` lay out the whole text each
  frame.

What works well: `Element` is a plain public interface (`render(Frame, Rect, RenderContext)`,
`preferredSize`, `handleKeyEvent`, `isFocusable`, `id`), and `StyledElement` an extensible
base. A custom viewport element that keeps the lines in memory (or a line index into a file)
and writes only the visible slice with `frame.buffer().setLine(x, y, Line)` rendered a
100,000-line document in **0.016 ms per frame**. Pair it with `scrollbar(ScrollbarState)` /
`ScrollbarElement` for the thumb. That is the route for the PR diff and transcripts; plain
`list(..)` is fine for the board and session lists (hundreds of rows).
[jar: `Element.java`; exp `Perf.java`]

"Lazy" in TamboUI's own API means something else: `Toolkit.lazy(Supplier<Element>)` /
`LazyElement` just evaluates its supplier at render time; it is not windowing. [jar]

## Focusable lists and panels, lazygit-style: supported

- Any `StyledElement` becomes a focus target with `.id("x").focusable()`. `ListElement`,
  `TreeElement`, `TableElement`, `TextAreaElement` are focusable out of the box.
  [jar: `StyledElement`, element sources]
- Focus moves with Tab / Shift+Tab (bindings `focusNext`/`focusPrevious`), by mouse click, or
  programmatically via `ToolkitRunner.focusManager()` → `setFocus(id)`, `focusNext()`,
  `focusedId()`, `isFocused(id)`. A handler can return `EventResult.FOCUS_NEXT` /
  `FOCUS_PREVIOUS`. lazygit-style number keys (1–5 jump to a panel) are a global handler
  calling `setFocus`. [jar: `FocusManager`, `EventRouter`]
- `panel(title, children...).focusedBorderColor(Color)` highlights the panel whose subtree
  holds focus — the lazygit look. CSS `:focus` styling is available too. [jar: `Panel.java`]
- Keys reach the focused element first, then global handlers
  (`runner.eventRouter().addGlobalHandler(..)`, `.onAction(..)`, `.on(InputTrigger, ..)`).
  Bindings sets: `BindingSets.standard()/vim()/emacs()/…` or loaded from a properties file —
  vim's j/k drive `ListElement` via `Actions.MOVE_UP/MOVE_DOWN`. Standard bindings map
  `quit` to q/Q/Ctrl+C; a focused text area swallows `q` first. [jar: `bindings/*.properties`]
- Gotcha: `ListElement`'s `ListState` is private (the javadoc's `.state(listState)` example is
  stale — no such method in 0.5.0). Because the toolkit calls the root supplier every frame,
  either reuse the same `ListElement` instance across frames, or keep the index in shell
  state and pass `.selected(i)` each frame, reading `selected()` back. Same for
  `TreeElement` (`selected()`, `selectedNode()`). `TableElement` does take a `TableState`.
  [jar]
- Focus state lives in the `ToolkitRunner`'s `FocusManager`; when nothing valid is focused,
  the first focusable element in render order gets focus. [jar: `ToolkitRunner.run`]

## Suspending the TUI for a child process: missing, workaround verified

- No suspend/exec API in 0.5.0, and none on `main` since (commits through 2026-10-08). The
  upstream request is gh **#323 "Support interactive child processes"** (open, 2026-05-03),
  which sketches a `runner.exec(Runnable)` in the spirit of Bubble Tea's `tea.ExecProcess`.
  [gh]
- Why it cannot be done inside a running runner: `TuiRunner` starts a daemon
  `tui-input-reader` thread that reads the backend for as long as `running` is true, and only
  `close()` stops it. A child started while the runner is alive would race it for stdin.
  There is no public way to pause the reader or toggle raw mode on the runner.
  [jar: `TerminalInputReader.java`, `TuiRunner.close()/cleanup()`]
- **Workaround that works**: end the runner, run the child, start a new runner.
  `ToolkitRunner.close()` stops the input reader, restores bracketed paste, mouse, cursor,
  leaves the alternate screen and closes the JLine terminal (restoring termios). Experiment
  `Suspend.java`, two rounds under a pty: in each round the TUI received a keystroke, quit,
  then `ProcessBuilder(...).inheritIO()` ran a child that saw `icanon echo` (cooked mode) and
  read a full line from the keyboard (`CHILD-GOT:hello`, `CHILD-GOT:world`); a fresh
  `ToolkitRunner.create()` then rendered and took keys again; no TamboUI/JLine threads were
  left. [exp]
- Consequences for the shell: the app's state must live outside the runner (the element
  supplier reads shell state, so it survives); focus must be restored with
  `focusManager().setFocus(id)` on the new runner; `close()` waits up to 2× poll timeout
  (80 ms by default) for the reader to stop; and the loop is "`quit()` from the key handler →
  `run()` returns → close → child → new runner". Leaving lazyclaude for `claude` and coming
  back is therefore a deliberate main-loop shape, not a widget call.
- `InlineToolkitRunner` / `InlineApp` (inline mode, no alternate screen) exist but do not
  change this. [jar]

## Off-render-thread work: supported

- All rendering and UI-state mutation happen on the thread running `TuiRunner.run()`, which
  `RenderThread.markAsRenderThread()` marks; wrong-thread rendering throws
  `IllegalStateException`. [jar: `TuiRunner.run`; docs: `api-levels.html#Threading Model`]
- Pushing results back: `ToolkitRunner.runOnRenderThread(Runnable)` (runs inline when already
  on the render thread, else queues a `UiRunnable`), `TuiRunner.runLater(Runnable)` (always
  queues), `TuiRunner.dispatch(Event)`. Work can run on any thread — virtual threads were used
  in the experiment. [jar; exp `Push.java`]
- Timers: `ToolkitRunner.schedule(r, delay)`, `scheduleRepeating(r, interval)`,
  `scheduleWithFixedDelay(r, delay)` return a cancellable `ScheduledAction`; they run on the
  runner's scheduler thread, **not** the render thread, so they must hop back through
  `runOnRenderThread`. `TuiConfig.builder().scheduler(..)` accepts an external executor.
  Good fit for polling `gh` and re-reading transcripts. [jar: `ToolkitRunner` javadoc]
- **Redraw gotcha**: running a `UiRunnable` does not itself trigger a redraw (`TuiRunner.run`
  `continue`s after it). With the default config, a tick every 40 ms re-renders, so a pushed
  result shows within one tick (verified). With `TuiConfig.builder().noTick()`, the pushed
  result did not appear — not even after an unhandled keystroke — until a redraw event was
  dispatched: `runner.tuiRunner().dispatch(TickEvent.of(0, Duration.ZERO))` after the update
  made it appear (verified). Ticks on means the whole element tree (and any markdown in it)
  is rebuilt ~25×/s; ticks off means every background update must also dispatch a redraw.
  [jar: `TuiRunner.run`, `ToolkitRunner.handleEvent`, `TuiConfig`; exp `Push.java`]

## Side finding: a supported test harness exists

`tamboui-toolkit` and `tamboui-tui` publish a `test-fixtures` classifier jar for 0.5.0
(`ToolkitTestRunner`, `ToolkitPilot`, `AbstractElementTest`; `TuiTestRunner`, `TuiPilot`),
documented in `pilot-testing.html`. It drives an app with keys, mouse and resizes and finds
elements by id. Not checked here: whether it can replace the split-package
`RenderThreadForTests` that ADR 0006 defers. [Maven Central; docs]

## Experiments

Kept outside the repo, in the session scratchpad (`scratchpad/exp/`): `Suspend.java`
(close → child → new runner, under `script -qfec`), `Push.java` (background push with and
without ticks), `Perf.java` (frame cost of `MarkdownElement`, `ListElement` and a custom
viewport element, rendered into a 200×50 `Buffer` via `Frame.forTesting`).
