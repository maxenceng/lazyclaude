---
id: SESSION-9
status: in-review
context: session
type: feature
tracker_id: 86
created: 2026-10-10
---

# The configuration file: the reserved chords and `notifications.desktop`, plus the keybindings warning

## Acceptance criteria

- [ ] With no `config.yml` (under `$XDG_CONFIG_HOME/lazyclaude/`, or `~/.config/lazyclaude/` when unset), the seven reserved chords are ADR 0008's defaults and desktop notifications are on.
- [ ] A file naming one entry rebinds that action alone: the new chord acts inside a hosted session and outside one, and the old chord now reaches `claude`.
- [ ] The key bar shows the bound chords — the compact `alt+h/j/k/l` form while the four focus chords share their modifiers and are each one character, every chord in full joined by `/` otherwise.
- [ ] `notifications.desktop: false` stops `notify-send`; markers and the status line still show waiting sessions.
- [ ] An unknown key, an invalid value, a chord with no modifier or with `shift` alone, a modifier other than `ctrl`/`alt`/`shift` (`meta`, `cmd`, …), or a multi-keystroke sequence stops startup: `lazyclaude: <file>:<line>: <reason>` on stderr, exit code 1, no frame drawn.
- [ ] Two actions on one chord stop startup the same way, reported at the second occurrence with the first one's line in the reason.
- [ ] A `~/.claude/keybindings.json` binding, in any context, whose first keystroke is a reserved chord — Claude Code's `meta`/`opt`/`option` read as `alt`, case ignored — raises one status-line warning listing every clash, and startup continues. A `null` binding is not a clash.
- [ ] A missing `keybindings.json` warns nothing; an unreadable one raises a warning that the clash check was skipped, and startup continues.
- [ ] `make contract` fetches Claude Code's keybindings documentation and fails when a default binding uses a reserved chord.
- [ ] `nextWaiting` rebinds `alt+w` as it behaves today: leaving the zoom.

## Model decision

New behaviour in `session`, no new aggregate. LAZY-20 gives `session` the configuration: hosting and notifications are its words.

- Domain: a `Chord` value object (modifiers and one key) that refuses a bare key or `shift` alone; `ReservedChords`, the seven actions to their chords with ADR 0008's defaults, refusing two actions on one chord; a `Configuration` holding them and the desktop-notification setting. A `ConfigurationPort` reads it, a `ClaudeKeybindingsPort` reads Claude Code's bindings, and the clash comparison is in the domain.
- Secondary: a repository walks `config.yml` node by node with SnakeYAML Engine so every error carries a line, finding the file the way `KittyConfigRepository` does; a Jackson repository reads `keybindings.json` and normalises Claude Code's modifier synonyms and case.
- Primary: a `ConfigurationQuery`, asked once in `LazyClaude.main` before the other reads; its `DomainException` takes the existing stderr-and-exit-1 path. Clashes and an unreadable `keybindings.json` go through `WarningPort` (ADR 0022).
- Published: the reserved chords. `shell` replaces its seven hard-coded characters with them, both to match keys and to draw the key bar.
- With `desktop: false`, `DesktopNotificationManager` never calls its port; markers and the status line are untouched. This is the switch ADR 0033's consequences deferred — point that consequence here.
- No new ADR, per LAZY-20.

## Design direction

Key bar, rebinding only `focusLeft` to `ctrl+alt+h`:

```
ctrl+alt+h/alt+j/alt+k/alt+l focus   alt+m main view   alt+z zoom
```

Defaults keep today's text (`alt+h/j/k/l`). A refused file, before any frame:

```
lazyclaude: ~/.config/lazyclaude/config.yml:3: unknown key 'focusLeftt'
```

## Glossary impact

*Reserved chord* moves from `session (expected)` to `session`. Nothing else.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-20](LAZY-20-configuration.md).

Refined 2026-10-26. `alt+w` only leaves the zoom today; going to the next waiting session is owned by no ticket (SESSION-7 left it out) and is not this one's.
Claude Code's `keybindings.json` shape — contexts, `null` unbinding, `meta`/`opt`/`option` for `alt`, space-separated sequences — is from code.claude.com/docs/en/keybindings; no such file exists on the development machine.
`Chord`, `ReservedChords` and `ReservedAction` sit in `session.published`, not `session.domain`: the shell matches keys with `Chord` and draws the key bar from it, and a domain copy would have meant a `ChordView` and a second matcher in the shell (ADR 0012, ADR 0029).
