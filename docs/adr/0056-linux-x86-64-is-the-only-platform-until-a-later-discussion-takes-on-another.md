# 56. Linux x86_64 is the only platform until a later discussion takes on another

Date: 2026-10-27

## Status

Accepted.

## Context

lazyclaude has only ever run on Linux, but no record says so. The desktop evolution (LAZY-24) adds a sidecar, a jlink
runtime and a Tauri frontend, each of which would be built per platform. LAZY-39 priced macOS and Windows: little of our
own code is Linux-only, but TamboUI's open Windows input bugs, JLine 3.25.1 hanging on quit on macOS, signing and
notarizing a jlink runtime inside a Tauri bundle, and one runtime per OS and architecture lie outside it, and closing a
notification on macOS or Windows is blocked without native code. lazyclaude has one user, on Linux, with no Mac or
Windows machine, so nothing built for them could be checked beyond what a headless CI runner shows.

## Decision

- **Linux on x86_64 is the one platform**, for the TUI and the desktop frontend alike, across every phase of the
  evolution. macOS, Windows and Linux on aarch64 are neither built nor tested. Rejected: macOS for the TUI only (Panama
  backend, show-only notifications), macOS for both frontends, all three — each ships what cannot be tried.
- **Taking on another platform is a later, separate discussion**, starting from LAZY-39's research
  (`docs/research/cross-platform-blockers.md`).
- **Each Linux-only spot carries a `// deferred: Linux only — <what> — closes when another platform is taken on`
  comment**, so `/debt` lists them for that discussion. Code the hosted-only cut (ADR 0047) deletes — the registry
  liveness check and the `/proc/<pid>/environ` read — is left uncommented. Rejected: a `make test` job on a macOS
  runner and a build check banning `/proc`-style paths outside secondary adapters — guards against a drift nobody pays
  for until then, while the hexagonal rule already keeps side effects in adapters.
- **When the cost is equal, the desktop frontend takes Tauri's cross-platform way over a Linux-only one**; when it is
  not, Linux-only is allowed.

## Consequences

- jlink packaging builds one runtime and one Tauri target triple, `x86_64-unknown-linux-gnu`.
- Linux-only code — D-Bus notifications, `/proc` reads, the WebKitGTK workaround — needs no portable twin, only its
  comment.
- The XDG config and cache paths stay; native macOS or Windows locations are that later discussion's choice.
- A port later costs what LAZY-39 lists plus whatever Linux-only code the evolution adds, which the comments count.
