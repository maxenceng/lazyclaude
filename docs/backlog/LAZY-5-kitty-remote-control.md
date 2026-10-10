---
id: LAZY-5
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1]
---

# What can kitty's remote control do for lazyclaude, and what does it require of the user's config?

## Question

What can kitty's remote control do for lazyclaude, and what does it require of the user's config?

Focus the window or tab running a given pid; open a new tab or window running a command in a given cwd; tell whether lazyclaude itself runs inside kitty. What happens outside kitty, or with `allow_remote_control` off?

## Kind

research

## Resolution

kitty can do all three. **Focus:** `--match pid:` only matches a window's direct child (the
shell), so read `KITTY_PID`/`KITTY_WINDOW_ID` from the claude process's `/proc/<pid>/environ`
(or find the pid in `kitten @ ls` → `foreground_processes`), then
`kitten @ --to unix:@kitty-<KITTY_PID> focus-window --match id:<id>` (switches tab and OS
window). **Open:** `kitten @ launch --type=tab --cwd <dir> --tab-title <t> claude …` prints the
new window id. **Detect:** `KITTY_WINDOW_ID` ⇒ inside kitty; `KITTY_LISTEN_ON` ⇒ a socket is
live; confirm with a probe `kitten @ ls`. **Config:** `allow_remote_control` (default `no`
refuses everything with "Remote control is disabled"; recommend `socket-only`) plus
`listen_on unix:@kitty-{kitty_pid}`, both needing a kitty restart. The user's config sets
neither today, so remote control is **off**. Fallbacks: tmux when `$TMUX` is set
(`list-panes`/`switch-client`/`new-window -c`), otherwise run `claude` in lazyclaude's own
terminal, which cannot bring forward a session live elsewhere.

Findings and sources: [docs/research/kitty-remote-control.md](../research/kitty-remote-control.md).
