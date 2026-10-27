---
id: DESKTOP-1
status: todo
context: desktop
type: feature
tracker_id: 112
created: 2026-10-27
---

# The desktop frontend starts its sidecar and shows the Board

## Acceptance criteria

- [ ] `frontend/` holds a Tauri 2 app, `src-tauri/` the relay and `src/` React and TypeScript, built with pnpm.
- [ ] The relay spawns `java -jar <jar> --sidecar` in the project directory — the first argument, else the launch
      directory — the jar being the one `make` builds unless `LAZYCLAUDE_SIDECAR` names another.
- [ ] The relay hands the page every waiting stdout line in one `Channel` message, writes the lines one `invoke` command
      gives it to stdin, and keeps the `ready` line for a page that asks after it went by; it parses no method.
- [ ] A Rust test covers a line split across two reads, several lines in one read, and `ready` answered after a reload.
- [ ] `src/protocol/` holds the TypeScript and a `method → {params, result}` map generated from `docs/protocol/`, and
      one client matching responses by `id` and routing notifications by method; nothing outside `src/protocol/`
      imports the generated files, `architecture.test.ts` failing otherwise.
- [ ] A `ready` whose `protocol` is not the one the client was generated for shows a refusal naming both, and sends
      nothing else.
- [ ] The Board shows `board/watch`'s View and each `board/changed` after it, through one store read with
      `useSyncExternalStore`; loading, empty, refused or unavailable, and populated each render and have a test.
- [ ] A JSON-RPC `error` answer shows an `Alert` with its message and a Retry.
- [ ] When the sidecar exits unasked, the window says so, shows stderr's last lines and offers a restart; it never
      restarts by itself.
- [ ] Closing the window closes the sidecar's stdin.
- [ ] `make frontend` runs pnpm install, regenerates the TypeScript and fails on a diff, then `tsc`, ESLint, Vitest,
      `cargo clippy` and `cargo test`; `make verify` and CI run it, CI with WebKitGTK's development packages;
      `make doctor` reports node, pnpm and cargo.
- [ ] CLAUDE.md lists `make frontend`, places `frontend/` in the structure, and says which parts of `ddd-frontend`
      apply (ADR 0057).
- [ ] Outside a session pane, one window-level `keydown` handler takes every key the TUI binds, the reserved chords
      from `ConfigurationQuery`'s method included, matched on `KeyboardEvent.key` and its modifiers. It calls
      `preventDefault` on each key it binds, and the webview's zoom hotkeys are off (ADR 0058).
- [ ] In a packaged build, going through the TUI's key table shows each key doing its TUI action and nothing of the
      webview's (no focus move, scroll, caret browsing, zoom or inspector).

## Model decision

No backend change. The frontend's folders mirror the contexts (`board` first); `src/protocol/` replaces
`ddd-frontend`'s `src/api/` (ADR 0057).

## Design direction

The Board as the TUI lays it out, its keys where the webview lets them through, the mouse by ADR 0051. Design tokens
and the primitives the Board needs start the design system.

## Glossary impact

*Desktop frontend* and *Relay* drop their `(expected)` marker.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md), recorded in ADR 0057. Blocked by
[SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md) and
[SIDECAR-2](SIDECAR-2-the-protocol-schema-is-generated-and-checked.md). Run under `tauri dev`; NVIDIA on Wayland needs
`WEBKIT_DISABLE_DMABUF_RENDERER=1` ([LAZY-36](LAZY-36-pty-over-a-sidecar.md)).
