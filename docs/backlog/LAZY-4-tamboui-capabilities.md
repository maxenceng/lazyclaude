---
id: LAZY-4
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1]
---

# What does TamboUI 0.5.0 offer for what v1 needs, and where are its gaps?

## Question

What does TamboUI 0.5.0 offer for what v1 needs, and where are its gaps?

- rendering markdown (`tamboui-markdown`, `tamboui-toolkit-markdown`);
- multi-line text input, for a review comment and summary;
- scrolling content far larger than the screen (a diff, a transcript), lazily;
- lists and panels with focus, as lazygit uses them;
- suspending the TUI, handing the terminal to a child process (`claude`, `$EDITOR`), and restoring it on exit;
- running work off the render thread and pushing results back (polling `gh`, reading transcripts).

## Kind

research

## Resolution

TamboUI 0.5.0 covers v1 except suspending the TUI, which has a verified workaround. Details,
API names and experiments: [`docs/research/tamboui-capabilities.md`](../research/tamboui-capabilities.md).

- **Markdown — supported.** Add `tamboui-toolkit-markdown`; `MarkdownElement.markdown(src).scroll(n)`.
  Scroll is driven by the shell; it re-parses the whole document every frame (~5 ms for 2,200
  lines); YAML frontmatter must be split off first.
- **Multi-line input — supported.** `textArea(TextAreaState)`. Enter inserts a newline and Tab
  moves focus, so submit is a chord like Ctrl+S via `.onKeyEvent`; Ctrl/Alt+Enter is unreliable
  (upstream #298).
- **Huge scrolling content — partial.** No virtualised view; `list(..)` is O(n) per frame
  (10k rows ~6 ms, 50k ~30 ms). The diff and transcripts need a small custom `Element` that draws
  only the visible slice (100k lines in 0.016 ms) plus `ScrollbarElement`.
- **Focusable lists/panels — supported.** `.id(..).focusable()`, `list`/`tree`/`table`,
  `panel(..).focusedBorderColor(..)`, `FocusManager.setFocus(id)`, vim binding set. `ListElement`'s
  state is private: keep the selection in shell state and pass `.selected(i)` each frame.
- **Suspend for `claude`/`$EDITOR` — missing API** (upstream #323 open, nothing on `main`). The
  input-reader thread owns stdin while a runner lives. Workaround verified under a pty: `quit()` →
  `close()` the `ToolkitRunner` → run the child with `inheritIO()` → start a new runner and
  restore focus. App state must live outside the runner.
- **Background work — supported.** Any thread, then `runOnRenderThread(..)`; `schedule*` runs off
  the render thread. A pushed update shows on the next tick (40 ms by default); with `noTick()`,
  dispatch a `TickEvent` to force the redraw.
