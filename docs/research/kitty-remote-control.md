# kitty remote control for lazyclaude

Researched 2026-10-09 against kitty 0.49.2 (the installed version) for LAZY-5.

## Question

What can kitty's remote control (`kitty @` / `kitten @`) do for lazyclaude: focus the
window or tab running a live `claude` process, open a new tab or window running a command
in a given cwd, and tell whether lazyclaude runs inside kitty? What does it require of the
user's config, and what happens outside kitty or with remote control off?

## Sources

- [RC] Remote control docs — https://sw.kovidgoyal.net/kitty/remote-control/
- [CONF] kitty.conf reference — https://sw.kovidgoyal.net/kitty/conf/ (`allow_remote_control`,
  `listen_on`, `remote_control_password`); the same text is the option definition in the
  installed source, `/usr/lib/kitty/kitty/options/definition.py`.
- [LAUNCH] `launch` docs — https://sw.kovidgoyal.net/kitty/launch/
- [SRC] Installed kitty 0.49.2 source under `/usr/lib/kitty/kitty/`: `boss.py`
  (`_handle_remote_command`, `listen_on` setup), `child.py` (child environment),
  `window.py` (`matches`, `as_dict`), `rc/base.py` (`--match` help), `rc/launch.py`,
  `rc/focus_window.py`, `remote_control.py` (`--to` help).
- [TMUX] tmux(1) man page — https://man.openbsd.org/tmux.1
- Local observation: `kitty --version`, the environment of this shell and of a running
  `claude` process (`/proc/<pid>/environ`), the user's kitty config.

## Findings

### 1. Detecting kitty

Every child kitty spawns gets `KITTY_PID` (the kitty process), `KITTY_WINDOW_ID` (the
window's id) and `KITTY_PUBLIC_KEY`; `TERM` is `xterm-kitty` unless the user changed `term`
[SRC `child.py`; RC: "The window id of the current window is available as the
KITTY_WINDOW_ID environment variable"]. `KITTY_LISTEN_ON` is set **only** when kitty is
actually listening on a socket (i.e. `listen_on`/`--listen-on` given *and* remote control
allows sockets) — otherwise kitty explicitly removes it from the child env [SRC `child.py`
lines ~386-391, `boss.py` ~448].

So: `KITTY_WINDOW_ID` present ⇒ lazyclaude runs inside kitty. `KITTY_LISTEN_ON` present ⇒
a socket is available. Neither tells you whether `allow_remote_control` permits commands;
the only reliable test is to try a harmless command (`kitten @ ls`) and read its result.

These variables are inherited, so they can be stale: a process started inside tmux (or
any long-lived server) launched from kitty carries the kitty values of wherever that server
was first started. Treat them as a hint, confirm with `ls`.

### 2. Config requirements

`allow_remote_control` (default `no`) [CONF, SRC `definition.py`]:

| value | over the window's TTY (`kitten @` with no `--to`) | over a socket |
|---|---|---|
| `no` (default) | refused: `Remote control is disabled` | refused |
| `yes` | accepted | accepted |
| `socket-only` | refused: `Remote control is allowed over a socket only` | accepted |
| `socket` | password-confirmed | accepted |
| `password` | password-confirmed (see `remote_control_password`) | password-confirmed |

[SRC `boss.py` `_handle_remote_command`.] `listen_on` (default `none`), e.g.
`unix:@kitty-{kitty_pid}` (Linux abstract socket) or `unix:${TEMP}/kitty`; if
`{kitty_pid}` is absent kitty appends `-<pid>`. It is ignored unless `allow_remote_control`
allows sockets. **Neither option can be changed by reloading the config** — kitty must be
restarted [CONF; SRC `boss.py` comment "we dont allow reloading the config file to change
allow_remote_control"]. `remote_control_password` can scope a password to specific
actions, e.g. `remote_control_password "pw" ls focus-window focus-tab launch` [CONF].

Recommended for lazyclaude: `allow_remote_control socket-only` + `listen_on
unix:@kitty-{kitty_pid}`. Only processes that can reach the socket (same user) may control
kitty, and programs merely printing escape codes to a window's TTY (e.g. a malicious file
`cat`-ed, or output inside a `claude` session) cannot.

### 3. Addressing kitty from lazyclaude

`kitten @` resolves its target as: `--to <address>`, else `$KITTY_LISTEN_ON`, else the
controlling terminal via escape codes (works only inside a kitty window) [RC; SRC
`remote_control.py` `--to` help]. For lazyclaude (a full-screen JLine/TamboUI app owning
the TTY), the socket path is preferable: the TTY path makes the kitten write a request to
and read the reply from `/dev/tty`, which competes with lazyclaude's own input reader.
*(Inference, not tested.)* Running `kitten @ ls` from a process without a controlling tty
(as this research shell had) fails with `open /dev/tty: no such device or address` —
observed locally.

Several kitty instances each listen on their own socket; with `{kitty_pid}` in `listen_on`
the socket for any process is derivable from its inherited `KITTY_PID`.

### 4. Focusing the window running a given `claude` pid

`--match pid:N` matches **only the window's direct child pid** (normally the shell), not
the foreground process [SRC `window.py` `matches`: `pat.pattern == str(self.child.pid)`].
A `claude` started from zsh therefore does not match `pid:<claude-pid>`. Two working routes:

1. **Read the claude process's environment** (`/proc/<pid>/environ`): it carries
   `KITTY_PID` and `KITTY_WINDOW_ID` of the window it was started in (observed: a live
   `claude` had `KITTY_PID=8067 KITTY_WINDOW_ID=1`). Then
   `kitten @ --to unix:@kitty-<KITTY_PID> focus-window --match id:<KITTY_WINDOW_ID>`.
   Cheap, no `ls` needed; subject to the staleness caveat above.
2. **Walk `kitten @ ls`**: its JSON (OS windows → tabs → windows) gives each window `id`,
   `pid`, `cwd`, `cmdline`, `env`, `at_prompt`, and `foreground_processes` (each with
   `pid`, `cwd`, `cmdline`) [SRC `window.py` `as_dict`; RC]. Find the window whose
   `foreground_processes` contains the claude pid, then `focus-window --match id:<id>`.
   Authoritative, also confirms remote control works.

`focus-window` activates the window, switching its tab and OS window if needed
(`set_active_window(..., switch_os_window_if_needed=True)`) [SRC `rc/focus_window.py`; RC].
`focus-tab --match` exists too, with tab fields incl. `window_id` [SRC `rc/base.py`]. Raising
another OS window may still be subject to the Wayland compositor's focus-stealing policy —
not verified.

### 5. Opening a new tab or window running a command in a cwd

`kitten @ launch --type=tab --cwd /path/to/project --tab-title LAZY-5 claude --resume <id>`
[LAUNCH, RC]. `--type` is `window` (default, same tab), `tab`, `os-window`, `overlay`,
`overlay-main`, `background`, …; `--cwd` takes a path or `current`/`last_reported`/
`oldest`/`root`; `--keep-focus` keeps focus where it was; `--hold` keeps the window after
the command exits; `--env K=V`; `--location` (e.g. `vsplit`) places it in split layouts;
`--match` picks the target tab [LAUNCH; SRC `rc/launch.py`]. It **prints the new window's
id**, so lazyclaude can record it for a later `focus-window --match id:N` [SRC
`rc/launch.py` desc "Prints out the id of the newly opened window"].

Note: `launch` with a command runs that program directly, not through the user's shell, so
when it exits the window closes (unless `--hold`), and shell rc-only PATH entries are not
applied — `--copy-env` exists for that [LAUNCH].

### 6. Outside kitty, or with remote control off

- Outside kitty (no `KITTY_WINDOW_ID`, no `KITTY_LISTEN_ON`): `kitten @` has no target and
  fails; lazyclaude should not try.
- Inside kitty with `allow_remote_control no` (the default): every command returns
  `{"ok": false, "error": "Remote control is disabled"}` and the kitten exits non-zero
  [SRC `boss.py`]. With `socket-only` and no socket: `Remote control is allowed over a
  socket only`.
- So lazyclaude needs a fallback path regardless: e.g. suspend the TUI and run
  `claude --resume <id>` in its own terminal as a foreground child, returning on exit. That
  can start or resume a session but cannot *bring forward* a session already running in
  another window — a live session can only be focused through the terminal/multiplexer.

### 7. Equivalent fallback: tmux (brief)

Inside tmux, `$TMUX` is set. `tmux list-panes -a -F '#{pane_id} #{pane_pid}
#{session_name}:#{window_index}'` lists panes; `pane_pid` is the pane's first process
(the shell), so as with kitty you walk the claude pid's parent chain to a pane pid.
`tmux switch-client -t <pane>` / `select-window -t` / `select-pane -t` focus it;
`tmux new-window -c <cwd> -n <name> 'claude --resume <id>'` opens one, and `-P -F
'#{pane_id}'` prints its id [TMUX]. No config needed — tmux is always controllable by the
same user.

## The user's config today

`/home/max/projects/term/kitty.conf` and `user.conf` (identical to the live
`~/.config/kitty/`) set **neither `allow_remote_control` nor `listen_on`**, and include only
`dank-tabs.conf`, `dank-theme.conf` and `themes/noctalia.conf` besides `user.conf`
(none of which matched `remote|listen`). So remote control is **off** (default `no`).
Consistent with that, this shell has `KITTY_WINDOW_ID`/`KITTY_PID` but no `KITTY_LISTEN_ON`.
`shell_integration enabled` is set, which makes `--cwd last_reported` and `at_prompt`
meaningful. Enabling it needs two lines in `user.conf` plus a kitty restart:

```
allow_remote_control socket-only
listen_on unix:@kitty-{kitty_pid}
```

## Answer

kitty can do everything lazyclaude wants — focus a live session's window (by reading
`KITTY_PID`/`KITTY_WINDOW_ID` from the claude process's `/proc/<pid>/environ`, or by
finding the pid in `kitten @ ls` `foreground_processes`; `--match pid:` alone does not
work), and open a tab/window in a given cwd with `kitten @ launch --type=tab --cwd …`,
which returns the window id. It requires `allow_remote_control` (recommend `socket-only`)
and `listen_on`, set before kitty starts; the user's config has neither today, so remote
control is off. Detect via `KITTY_WINDOW_ID` (inside kitty) and `KITTY_LISTEN_ON` (socket
available), confirm with a probe `ls`, and fall back to tmux when `$TMUX` is set or to
running `claude` in lazyclaude's own terminal otherwise.
