# 36. kitty.conf is edited only on a yes, and never over a conflict

Date: 2026-10-17

## Status

Accepted. Builds on ADR 0035.

## Context

Jumping to a kitty window needs `allow_remote_control` and `listen_on` set before kitty starts; neither can be reloaded. The user's config
sets neither. Editing someone's terminal config is the one write lazyclaude makes outside its own files.

## Decision

- **The offer is made only when kitty refuses and lazyclaude itself runs in kitty** (its own environment carries `KITTY_PID` and
  `KITTY_WINDOW_ID`, even under tmux). Outside kitty it never offers; it warns that remote control is off.
- **The file** is `$KITTY_CONFIG_DIRECTORY/kitty.conf`, else `$XDG_CONFIG_HOME/kitty/kitty.conf`, else `~/.config/kitty/kitty.conf`:
  the order kitty looks in, so the file edited is the one kitty reads.
- **The lines** are `allow_remote_control socket-only` and `listen_on unix:@kitty-{kitty_pid}`; the jump's socket is derived
  from that same `listen_on` line, so the two cannot drift. The adapter reads `kitty.conf` and every file it `include`s or `globinclude`s,
  a file's lines applied in order and an included file where the include is written; the domain takes the last line for a key as the
  effective one and decides offer, conflict or restart.
- **Conflict.** `allow_remote_control` set to `socket`, or to `yes` (`y`, `true`), allows control over the socket as `socket-only`
  does, so it is no conflict. When either key's effective value is anything else (`no`, `password`), the popup shows the file and line that sets it and what is wanted, and
  only closes; nothing is edited. This holds for a key set in an included file.
- **Only the lines not yet set are offered.** When both are set as wanted, `o` warns that kitty needs a restart and opens no popup.
- **`y` appends** the lines under `# added by lazyclaude` and says kitty needs a restart. It reads the file again first, so a key set
  another way since the offer is a conflict and nothing is written. `n` and `esc` close; a refused offer is not remembered, the next
  refused `o` offers again. A failed write warns on the status line.
- An include that does not exist is skipped, a cycle is not followed twice (by real path, so through a symlink too), and relative
  includes resolve against the including file. As kitty does, an include path has `~` and `$VAR`/`${VAR}` expanded, and `**` in a
  glob matches zero directories too.

## Consequences

- A tmux server started from kitty and attached from another terminal leaves lazyclaude a stale `KITTY_*`, so it is still offered
  the lines, the opposite of ADR 0035's tmux-first rule. Kept: a refusal shows a kitty with remote control off on this machine, and
  `kitty.conf` is per user. Requiring lazyclaude's own residence to be in kitty would hide the offer from lazyclaude in tmux in kitty.
- A `kitty.conf` only under `$XDG_CONFIG_DIRS` is not found; one is started in the user's directory, which then shadows it.
- `envinclude`/`geninclude` are not followed; a key set only through them is not seen.
- Without a `kitty.conf` the file and its directory are created.
