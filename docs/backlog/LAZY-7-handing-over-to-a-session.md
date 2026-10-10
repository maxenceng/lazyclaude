---
id: LAZY-7
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-2, LAZY-4, LAZY-5]
---

# How does lazyclaude hand the user to a session — live, past, or new?

## Question

How does lazyclaude hand the user to a session — live, past, or new?

Options include suspending the TUI and running `claude --resume <id>` / `claude "/ticket <verb> <id>"` in the same terminal until it exits, opening a kitty tab or window, focusing the kitty window a live session already runs in, or launching every session under tmux so any of them can be attached. A live session already owns another terminal, so "go into" may mean *go to* rather than *take over*.

## Kind

grilling

## Resolution

**lazyclaude hosts sessions itself. It doesn't hand the terminal over.**

**Hosted sessions** are new and resumed sessions.
- lazyclaude spawns the real `claude` on a pty, runs it through an embedded terminal emulator, and draws it in a pane.
- It behaves exactly like Claude Code itself. Keys go to it while the pane has focus.
- This was picked over a home-made chat view over `stream-json`, which would mean reimplementing Claude Code's UI and losing its dialogs.
- Whether it can be done is LAZY-14.
- No kitty tab and no tmux are used for these sessions.
- When lazyclaude quits, hosted sessions die with it. Quitting asks for confirmation while any of them is busy or waiting. Each one can be resumed later from its transcript.

**Outside sessions** are live sessions started somewhere else, which lazyclaude can't embed.
- "Go into" opens a read-only live view of the session's transcript, plus a jump to its kitty window (LAZY-5).
- When kitty remote control is off, lazyclaude:
  - shows the file it would edit (`$KITTY_CONFIG_DIRECTORY/kitty.conf`, else `~/.config/kitty/kitty.conf`) and the lines `allow_remote_control socket-only` and `listen_on unix:@kitty-{kitty_pid}`;
  - asks y/n;
  - appends them under `# added by lazyclaude`;
  - says kitty needs a restart.
- It never edits when either key is already set to something else; it shows the conflict instead.
- It never asks outside kitty.
- It doesn't remember a "no".

**Worktrees.**
- Every ticket session runs in the ticket's one worktree, `../<repo>.worktrees/<id>-<slug>`, on branch `<id>-<slug>` (the name `/ticket start` would pick).
- lazyclaude creates it with `git worktree add` before the ticket's first session. Every later session of that ticket (refine, start, respond, done) reuses it.
- A new session starts as `claude -n "<ID> <verb>" "/ticket <verb> <ID>"`.
- A resume runs in the session's recorded `cwd`. If that worktree is gone, lazyclaude recreates it at the same path, on the branch if it still exists, otherwise a fresh branch from `main`.

**Unlinked sessions.**
- lazyclaude can also start a plain `claude` that isn't tied to a ticket.
- These run in the project root, the one exception to the worktree rule.
- If one later runs `/ticket <verb> X`, LAZY-3's rule attributes it to X.

**No editing in v1.**
- There's no `e` and no `$EDITOR` handover anywhere.
- The board, ticket detail and docs are read-only views.
- TamboUI's missing suspend API (LAZY-4) therefore no longer matters for v1.
