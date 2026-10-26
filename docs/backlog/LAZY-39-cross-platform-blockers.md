---
id: LAZY-39
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# What stands between lazyclaude and macOS or Windows?

## Question

What stands between lazyclaude and macOS or Windows?

For each piece: TamboUI with JLine on Windows consoles and macOS terminals, pty4j (ConPTY on Windows), jediterm, `claude` and its registry and transcript paths per OS, `gh`, git worktrees, `WatchService` behaviour (macOS polls), desktop notifications without D-Bus, XDG paths for `config.yml`, jlink runtimes per OS and architecture, and Tauri's sidecar support per OS. What is free, what is work, what is blocked.

## Kind

research

## Resolution

Little stands in the way, and only one thing is truly blocked. Already portable: paths, the
transcript folder rule, `gh`, git worktrees, jediterm, the config and cache fallbacks, and the
watchers (macOS's `WatchService` polls every 2 s). Linux-only code: the registry liveness check
(`/proc`, `/etc/machine-id`), which hosted-only sessions mostly make unnecessary, and
notifications through `notify-send`/`gdbus`. Windows also needs pty4j's
`setUseWinConPty(true)` and a `claude` that resolves to an `.exe`. The real cost lies outside
our code: TamboUI's open Windows input bugs, JLine 3.25.1 hanging on quit on macOS (the Panama
backend or JLine 4 avoids it), signing and notarizing the bundled jlink runtime on macOS, and
one runtime per OS and architecture (no Temurin 25 for Windows on arm64). Blocked: closing or
replacing a notification on macOS and Windows through Tauri's plugin, which needs native code
or has to be dropped. Nothing was run on a Mac or on Windows. `CLAUDE_CONFIG_DIR` is ignored on
every OS. Details: [`docs/research/cross-platform-blockers.md`](../research/cross-platform-blockers.md).
