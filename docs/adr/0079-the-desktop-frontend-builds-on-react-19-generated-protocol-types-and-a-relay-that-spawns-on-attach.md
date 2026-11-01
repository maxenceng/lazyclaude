# 79. The desktop frontend builds on React 19, generated protocol types and a relay that spawns on attach

Date: 2026-11-01

## Status

Accepted. Builds on ADR 0057 (the desktop frontend in `frontend/`), ADR 0058 (its keys) and ADR 0078 (its chords).

## Context

ADR 0057 fixes the desktop frontend's shape — a Tauri window, a relay that knows no method, a TypeScript client generated
from `docs/protocol/` — but not the libraries, how the generated names reach the screens, or what the relay does beyond
its three behaviours. The first Board made each of these choices; each could be undone without knowing why.

## Decision

- **React 19, tested with Vitest, jsdom and Testing Library; TypeScript held at 6** because typescript-eslint does not
  accept 7 yet. Fonts are bundled through `@fontsource` (Barlow, Saira Stencil One, JetBrains Mono), not linked from
  Google Fonts as in the sketch: the window works offline.
- **Types come from `json-schema-to-typescript`** with a script writing the `method → {params, result}` map, into
  `src/protocol/generated/`, checked in. The generator prefixes a context (`BoardBoardView`); `src/protocol/index.ts`
  re-exports the short names, and nothing else imports the generated file (`architecture.test.ts`).
- **The front-end architecture rules** in `architecture.test.ts`: `board` and `session` are contexts and never import
  each other; `protocol`, `design-system`, `shared`, `keys` and `connection` are shared; `App.tsx` composes the contexts,
  handing the Board a session lamp lookup.
- **The relay spawns the sidecar on the page's first attach**, not at window creation, so the page's `Channel` exists
  before the first line. It runs `java --enable-native-access=ALL-UNNAMED -jar`, as `make run` does; the default jar is
  the repository's `target/lazyclaude.jar`, compiled in, until the bundle (ADR 0059) carries its own.
- **The sidecar's exit travels in the same `Channel`** as its lines, `{kind: "exited", code, stderr}`, with the last 20
  stderr lines. It is the relay's only message of its own; it is not a protocol method.
- **Lamps come from `session/watch`**, since the Board View carries no session state, joined to tickets by id.
- **No reserved chord is taken until `session/configuration` answers** (ADR 0078). The Board's fixed keys work from the
  first keypress; a default `alt+h` bound before the answer would be wrong for a rebound `config.yml`. A reserved chord
  is matched before the fixed keys, so a chord rebound onto one of them wins.
- **`mouse: false` leaves the window's clicks alone.** ADR 0051's setting turns a terminal's mouse capture off; a
  window's click is not captured from a terminal, so the desktop frontend reads the flag and ignores it.
- **The in-progress status heads the Board**, before the View's order, because the Board answers "what am I on". Every
  other status keeps its heading and count even when empty, the finished one folded, as in the TUI.
- **The focus edge is blue**, apart from the amber input lamp the sketch shared it with: lamp colours mean session state
  and nothing else. Below 760px the lamp tiles fold to marker and count.

## Consequences

- A TypeScript 7 upgrade waits for typescript-eslint.
- A webview reload gets `ready` back but not the lines sent while no page was attached; the page reads afresh.
- Packaging must embed the built page (`tauri build`, or `cargo build --features custom-protocol`); a plain debug build
  loads the dev server's URL.
