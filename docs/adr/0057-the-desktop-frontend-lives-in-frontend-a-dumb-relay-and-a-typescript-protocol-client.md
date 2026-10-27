# 57. The desktop frontend lives in `frontend/`, behind a dumb relay and a TypeScript protocol client

Date: 2026-10-27

## Status

Accepted. Builds on ADR 0049 and ADR 0050.

## Context

The desktop frontend is a Tauri window, React over xterm.js, that drives the backend as a sidecar (ADR 0048, ADR 0050).
LAZY-36's prototype relayed lines through a few lines of Rust and left the protocol to the page. The `ddd-frontend`
skill describes a React client generated from an OpenAPI schema and read through TanStack Query, with HTTP statuses for
failures; here the schema is ADR 0050's JSON Schema, every read is a watch pushing full Views, and a refusal is a
result, not a status. A webview reload loses the page's emulators while the relay, the sidecar and the hosted sessions
live on, and `claude` repaints only part of its terminal state on SIGWINCH (ADR 0049's amendment).

## Decision

- **`frontend/` in this repository**, Tauri's `src-tauri/` and `src/` side by side, so the schema in `docs/protocol/`,
  the TypeScript generated from it and the screens reading it change in one pull request. Rejected: a repository of its
  own, which syncs the schema across repositories for one developer.
- **The relay knows no method.** The Rust half spawns the sidecar, batches its stdout lines into one `Channel`, writes
  lines from one `invoke` command to its stdin, and keeps the `ready` line for a reloaded page. TypeScript owns
  JSON-RPC: ids, matching responses, routing notifications, base64. Rejected: typed Tauri commands in Rust, a second
  copy of the method list to drift from the schema.
- **Development runs the jar `make` builds:** the relay runs `java -jar` on it, `LAZYCLAUDE_SIDECAR` overriding the
  path. The project is the directory given as argument, else the launch directory, as the TUI takes it; no picker.
- **`ddd-frontend` applies in part.** Kept: one folder per context named like it, no context importing another, the
  design system and its tokens, the lint rules, `architecture.test.ts`, the four states, generated code never edited.
  Replaced: `src/api/` by `src/protocol/` — types and a method map generated from the schema, one client; a refused or
  unavailable View renders by its `kind`, and a JSON-RPC `error` is a fault shown as an `Alert` with Retry. Dropped:
  i18n, since lazyclaude is English only, and MSW, since there is no HTTP; tests mock `src/protocol/`. Rejected:
  TanStack Query, whose fetching, caching and retries would sit unused under push-only watches; each watch is one store
  read through `useSyncExternalStore`.
- **Parity is the TUI's screens and keys**, the mouse by ADR 0051, reached in order: the Board with a crash restart,
  the hosted session pane, Docs, Review, capture. Rejected: a desktop layout of its own over the same Views.
- **A reloaded pane restores what the repaint does not.** The page keeps, per `SessionId`, the private modes its
  emulator was asked for in `sessionStorage`, which survives a reload and not a quit; a reloaded pane writes them back
  into its new emulator, then asks for `session/repaint`. Rejected: the sidecar scanning the stream for modes, a piece
  of the headless shadow ADR 0049 rejected; accepting a reload that pastes and keys wrong.
- **A packaged build cannot reload**: F5 and Ctrl+R are swallowed and the context menu is off. The repaint path stays,
  for `tauri dev` and for a hung page's Reload.
- **`make frontend` runs in `make verify`**: pnpm, the TypeScript generated and drift-checked against `docs/protocol/`,
  `tsc`, ESLint, Vitest, `cargo clippy` and `cargo test`. CI installs WebKitGTK's development packages.

## Consequences

- CI and `make doctor` need node, pnpm and cargo besides Java 25.
- A View changed in Java fails the frontend's drift check until the TypeScript is regenerated, which is the point.
- A mode `claude` starts using later is lost on reload until the page tracks it too.
- A key the webview or xterm.js swallows cannot reach the TUI's binding; those, `config.yml`'s chords and the desktop
  notification are decided apart.
- The relay holds no protocol logic, so a protocol bug never needs a Rust change.
