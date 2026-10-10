---
id: LAZY-20
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-8, LAZY-10]
---

# What is configurable in v1, and where does the configuration live?

## Question

What is configurable in v1, and where does the configuration live?

The candidates so far:
- keys, including the `alt+h/j/k/l` (+ `alt+m/z/w`) that ADR 0008 reserves inside hosted sessions, for when they clash with something;
- opting out of desktop notifications (LAZY-8).

Is v1 configurable at all, or fixed? If it is: a file under `$XDG_CONFIG_HOME/lazyclaude/` (lazygit's habit), per-project overrides or not, and what happens with an unknown or invalid key?

## Kind

grilling

## Resolution

**v1 has one small, strict configuration file. It covers the reserved chords and desktop notifications, nothing else.**

**Where.**
- `$XDG_CONFIG_HOME/lazyclaude/config.yml`, or `~/.config/lazyclaude/config.yml` when the variable is unset.
- No per-project override: both settings are about Claude Code and the desktop, not the project.
- A missing file means defaults. lazyclaude never writes the file.

**Shape.** Entries left out keep their default, so a one-line file is valid.

```yaml
keybinding:
  focusLeft: alt+h      # also steps to the previous dock tab
  focusDown: alt+j
  focusUp: alt+k
  focusRight: alt+l     # also steps to the next dock tab
  hideMainView: alt+m
  zoom: alt+z
  nextWaiting: alt+w
notifications:
  desktop: true
```

- Each of the seven reserved chords (ADR 0008) rebinds on its own. Every other key, outside or inside a pane, is fixed in v1.
- Chords are spelled as in ADR 0008 and Claude Code's `keybindings.json`: `alt+h`, `ctrl+alt+h`, with modifiers `ctrl`, `alt` and `shift`.
- Every chord needs a modifier; a bare key would steal what you type into `claude`.
- `notifications.desktop: false` stops only `notify-send`. Markers and the status line stay (LAZY-8).

**Validation.**
- An unknown key, an invalid value or two actions on one chord all stop startup. lazyclaude prints the file, the line and the reason, and exits before the first frame.
- Warn-and-default was rejected: a typo would quietly bring back the clashing default the file exists to escape.
- The file is read once at startup. There is no live reload; a restart is cheap, since hosted sessions resume from their transcripts (LAZY-7).

**Clashes with Claude Code.**
- At startup, lazyclaude warns, without refusing, when `~/.claude/keybindings.json` binds a reserved chord. That file is Claude Code's, not lazyclaude's.
- A contract test in the build checks the reserved chords against Claude Code's documented default bindings.

**Implementation.**
- SnakeYAML Engine (YAML 1.2, so `no` stays a string), walked node by node so errors carry a line. No object binding.
- `session` owns the configuration: hosting and notifications are its words (LAZY-12). One port reads `config.yml` and `keybindings.json` in `session.infrastructure.secondary`.
- `session.published` exposes the reserved chords. `shell` uses them to move focus outside a pane; the hosted pane uses them to decide which keys not to forward to `claude`.
- No ADR: with one user every choice here is cheap to reverse, and ADR 0008 already anticipated the key set moving into configuration.
- Glossary: *Reserved chord* added under `session`.
