---
id: LAZY-41
status: done
context: cross-context
type: decision
created: 2026-10-27
blocked_by: [LAZY-38]
---

# How is the desktop frontend laid out and built?

## Question

How is the desktop frontend laid out and built?

Where it lives (a `frontend/` here or its own repo) and how its Tauri app finds the sidecar jar in development; how far
`ddd-frontend` applies without an OpenAPI client, given TypeScript types generated from `docs/protocol/`'s schema
(ADR 0050); the typed client over a Tauri `Channel`; and the order its screens reach parity with the TUI. Also how a
webview reload repaints its panes by ADR 0049's SIGWINCH nudge, which waits on `claude`'s full redraw on SIGWINCH being
checked.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0057](../adr/0057-the-desktop-frontend-lives-in-frontend-a-dumb-relay-and-a-typescript-protocol-client.md),
the repaint's checked facts in [ADR 0049](../adr/0049-a-hosted-pty-carries-bytes-and-each-frontend-emulates-its-own.md).

- **Words**: *Desktop frontend* (replacing "desktop window") and *Relay*, in the glossary's `sidecar` section; the
  *desktop notification* keeps its name.
- **Where**: `frontend/` in this repository, `src-tauri/` and `src/` side by side. Rejected: a repository of its own.
- **Development**: the relay runs `java -jar` on the jar `make` builds, `LAZYCLAUDE_SIDECAR` overriding it; the project
  is the argument, else the launch directory; no picker.
- **`ddd-frontend`**: context folders, design system, lint, `architecture.test.ts`, the four states and untouched
  generated code kept; `src/protocol/` replaces `src/api/`, refusals render by `kind`, a JSON-RPC `error` is an `Alert`
  with Retry; i18n and MSW dropped, Vitest mocking `src/protocol/`. Server state is one store per watch over
  `useSyncExternalStore`. Rejected: TanStack Query.
- **Relay**: spawns, batches stdout lines into a `Channel`, writes stdin lines from one `invoke`, keeps `ready`; no
  method known. TypeScript owns JSON-RPC. Rejected: typed Tauri commands. A small Rust test covers batching across a
  partial line and the `ready` cache.
- **Parity**: the TUI's screens and keys, the mouse by ADR 0051, in order Board with crash restart, hosted session pane,
  Docs, Review, capture; tickets carry `DESKTOP`. Rejected: a desktop layout of its own.
- **Build**: `make frontend` in `make verify` — pnpm, TypeScript generated and drift-checked, `tsc`, ESLint, Vitest,
  `cargo clippy`, `cargo test`; CI installs WebKitGTK's development packages; `make doctor` reports node, pnpm, cargo.
- **Reload**: checked on Claude Code 2.1.310 — a one-row shrink and restore at least 10 ms apart repaints the whole
  alternate screen, identical to a full stream; no gap repaints nothing; the alternate screen, bracketed paste, focus
  reports, `?2031` and the kitty keyboard mode are not re-sent. So the page keeps each session's modes in
  `sessionStorage` and writes them back into a reloaded pane, then asks `session/repaint`, which nudges with a 50 ms gap.
  Rejected: the sidecar scanning the stream for modes. A packaged build swallows F5 and Ctrl+R and has no context menu;
  the repaint stays for `tauri dev` and a hung page.

Graduated into [DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md),
[DESKTOP-2](DESKTOP-2-hosted-sessions-in-the-desktop-frontend.md), [DESKTOP-3](DESKTOP-3-docs-in-the-desktop-frontend.md),
[DESKTOP-4](DESKTOP-4-review-in-the-desktop-frontend.md), [DESKTOP-5](DESKTOP-5-capture-in-the-desktop-frontend.md) and
[LAZY-45](LAZY-45-notifications-and-configuration-on-the-desktop.md); `session/repaint` joins
[SIDECAR-3](SIDECAR-3-hosted-sessions-over-the-sidecar.md).
