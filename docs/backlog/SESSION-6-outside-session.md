---
id: SESSION-6
status: done
context: session
type: feature
tracker_id: 83
created: 2026-10-10
---

# An outside live session: a read-only transcript view, plus a kitty jump with the offer to add the `kitty.conf` lines

## Acceptance criteria

- [ ] Selecting an outside or past session in the Sessions panel shows its transcript in the
      main view: your prompts and Claude's replies, each tool call as one dim
      `⏺ <Tool>  <argument>` line. Tool results and thinking are not shown. An Elsewhere row
      keeps its facts view.
- [ ] While scrolled to the bottom, the view follows new entries as the transcript grows.
      Scrolled up (`j`/`k`, `ctrl+d`/`ctrl+u`, `g`), it holds its place; `G` follows again.
- [ ] A session whose transcript does not exist yet shows that it has none, and fills in once
      it appears.
- [ ] An outside row's label is where the session lives, read from its process's environment:
      `tmux` when it carries `TMUX_PANE`, else `kitty` when it carries `KITTY_WINDOW_ID`, else
      its age as today.
- [ ] `o` on an outside row, in the Sessions or Elsewhere panel, brings its window forward:
      in kitty with `kitten @ --to unix:@kitty-<KITTY_PID> focus-window --match
      id:<KITTY_WINDOW_ID>`, in tmux, when lazyclaude runs in the same tmux server, with
      `switch-client` to its pane.
- [ ] `o` on a session in tmux while lazyclaude is not in that tmux server, on a session in
      neither, or when the jump command fails, warns on the status line why and does nothing
      else. `o` on a hosted or past row does nothing.
- [ ] When kitty refuses because remote control is off and lazyclaude itself runs in kitty, a
      popup shows the file (`$KITTY_CONFIG_DIRECTORY/kitty.conf`, else
      `$XDG_CONFIG_HOME/kitty/kitty.conf`, else `~/.config/kitty/kitty.conf`) and the lines `allow_remote_control socket-only` and
      `listen_on unix:@kitty-{kitty_pid}`. `y` appends them under `# added by lazyclaude` and
      says kitty needs a restart; `n` or `esc` closes it.
- [ ] When either key is already set to a value that does not allow socket control (`socket` and
      `yes` do), in `kitty.conf` or any file it
      `include`s or `globinclude`s, the popup shows the conflict and nothing is edited.
- [ ] When both keys are already set to those values, `o` warns that kitty needs a restart and
      opens no popup.
- [ ] lazyclaude running outside kitty never offers the lines. A refused offer is not
      remembered: the next refused `o` offers again. A failed write warns on the status line.
- [ ] A session lazyclaude hosts is never marked outside under Elsewhere, so `o` does nothing
      on it there.

## Model decision

**New behaviour on `session`'s live and past sessions.** No new aggregate, no new context,
no new edge.

- **Where a live session lives.** A sealed value on the live session: `InKitty` (the
  `KITTY_PID` and `KITTY_WINDOW_ID` it carries), `InTmux` (its `TMUX_PANE` and server), or
  `Nowhere`. tmux wins over kitty, since a `claude` inside tmux carries the stale `KITTY_*` of
  wherever the server was started. A `ProcessEnvironmentPort` reads `/proc/<pid>/environ`,
  implemented in `session.infrastructure.secondary`; the sweep reads it with the registry.
- **The jump.** A `TerminalJumpPort` with a kitty and a tmux adapter in
  `session.infrastructure.secondary`, running `kitten @` and `tmux`. Whether lazyclaude itself
  runs in kitty or in the session's tmux server is read from lazyclaude's environment, handed
  over at wiring. A refusal for remote control off is told apart from any other failure.
- **The kitty.conf offer.** A `KittyConfigPort` locates `kitty.conf`, reads it and every file it
  includes for the two keys, and appends the lines. The domain decides offer, conflict or
  restart from what it reads. The popup and its y/n are the shell's, as the quit prompt is.
- **The transcript view.** A new read on the transcript, separate from the metadata read
  `TranscriptTail` does: entries become prompt, reply and tool-call lines, read from an offset
  so the view tails the file, driven by the existing transcript watch. A live session finds its
  transcript by its session id and `cwd`. The shell asks for it through `SessionQuery`, for the
  selected session only.
- **Fix.** `AvailableSessions.hosting` lays the hosted pids over Elsewhere too, not only over
  this project's live sessions.
- **ADR, in the implementing change:** where a session lives is read from its own environment,
  tmux first; the kitty.conf edit, its conflict rule across includes, and never asking outside
  kitty; what the transcript view renders and what it hides.
- **Deferred:** a session in a tmux server lazyclaude is not attached to cannot be jumped to.
  A `deferred:` note closes it when someone needs that jump (the attached client's kitty
  window).
- Not in this ticket: opening a new kitty tab or tmux window (sessions are hosted, LAZY-7),
  `w`/`alt+w` next waiting, zoom.

## Design direction

The offer, a popup over the main view:

```
╭ kitty remote control is off ──────────────────╮
│ append to ~/.config/kitty/kitty.conf:         │
│                                               │
│   # added by lazyclaude                       │
│   allow_remote_control socket-only            │
│   listen_on unix:@kitty-{kitty_pid}           │
│                                               │
│ kitty needs a restart afterwards              │
╰ y add · n cancel ─────────────────────────────╯
```

With a conflict, the body names the file and line that sets the key and the popup only closes
(`esc close`).

- Sessions panel row: `• Fix status  kitty`, `? lint pass  tmux`.
- The transcript view: prompts prefixed `›`, replies plain, tool calls dim `⏺ Bash  make test`.
- Keys: `o` jumps on an outside row; the key bar gains `o jump` there. `y`/`n`/`esc` answer the
  popup.

## Glossary impact

- **Outside session** drops its *(expected)* marker, and gains: labelled `kitty` or `tmux` by
  where it lives.
- Adds **Transcript view**: the main view's read-only, tailing render of an outside or past
  session's transcript — prompts, replies and one line per tool call.
- Adds **Jump**: `o` on an outside session, bringing its kitty window or tmux pane forward.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-5](LAZY-5-kitty-remote-control.md), [LAZY-7](LAZY-7-handing-over-to-a-session.md). Falls back to tmux under `$TMUX`.

Closed after [#32](https://github.com/maxenceng/lazyclaude/pull/32). ADR 0035 records reading where a session lives from its own environment, tmux first. ADR 0036 records the `kitty.conf` edit and its conflict rule, ADR 0037 what the transcript view shows. What the code will not show:

- Nothing was run against a real kitty or tmux. The jump and the `kitty.conf` adapters are tested over fake `kitten`/`tmux` scripts, so the first real jump is also the first test of the command lines.
- Review widened two criteria: `socket` and `yes` count as allowing socket control, and `$XDG_CONFIG_HOME/kitty/kitty.conf` is looked at between `$KITTY_CONFIG_DIRECTORY` and `~/.config/kitty`, as kitty does.
- A transcript read that lands after its session was selected again is dropped. Before review it replaced the whole transcript with its own tail. A transcript rewritten in place is only partly detected, and the rest is deferred in ADR 0037.
- `kitten` is a new runtime dependency that `make doctor` does not check. A missing one only shows up as the jump's warning.
