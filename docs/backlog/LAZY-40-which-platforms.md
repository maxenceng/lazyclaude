---
id: LAZY-40
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-25, LAZY-39]
---

# Which platforms does this evolution support?

## Question

Which platforms does this evolution support?

Go or no-go for macOS and Windows, per phase, against LAZY-39's findings; Linux alone if the cost is high for now, and what then stays portable so the door is not closed.

## Kind

grilling

## Resolution

Linux on x86_64 only, for every phase and both frontends; macOS, Windows and Linux aarch64 are neither built nor
tested, since there is no machine to try them on, and taking one on is a later, separate discussion starting from
LAZY-39's research. The door stays open by a `// deferred: Linux only — … — closes when another platform is taken on`
comment at each Linux-only spot, for `/debt` to list; no macOS CI job and no build check. The desktop frontend takes
Tauri's cross-platform way when it costs the same, a Linux-only one otherwise. jlink packaging targets one runtime and
one target triple, `x86_64-unknown-linux-gnu`; the XDG config and cache paths stay (ADR 0056).
