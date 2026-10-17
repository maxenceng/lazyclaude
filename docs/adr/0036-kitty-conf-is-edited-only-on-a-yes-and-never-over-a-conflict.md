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
- **The file** is `$KITTY_CONFIG_DIRECTORY/kitty.conf`, else `~/.config/kitty/kitty.conf`.
- **The lines** are `allow_remote_control socket-only` and `listen_on unix:@kitty-{kitty_pid}`. The domain reads `kitty.conf` and every file
  it `include`s or `globinclude`s, a file's lines applied in order and an included file where the include is written, so the last line
  for a key is the effective one.
- **Conflict.** When either key's effective value is another one, the popup shows the file and line that sets it and what is wanted, and
  only closes; nothing is edited. This holds for a key set in an included file.
- **Only the lines not yet set are offered.** When both are set as wanted, `o` warns that kitty needs a restart and opens no popup.
- **`y` appends** the lines under `# added by lazyclaude` and says kitty needs a restart. It reads the file again first, so a key set
  another way since the offer is a conflict and nothing is written. `n` and `esc` close; a refused offer is not remembered, the next
  refused `o` offers again. A failed write warns on the status line.
- An include that does not exist is skipped, a cycle is not followed twice, and relative includes resolve against the including file.

## Consequences

- `envinclude`/`geninclude` are not followed; a key set only through them is not seen.
- Without a `kitty.conf` the file and its directory are created.
