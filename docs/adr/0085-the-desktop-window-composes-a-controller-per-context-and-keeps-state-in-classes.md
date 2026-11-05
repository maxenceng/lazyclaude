# 85. The desktop window composes a controller per context, and keeps behaviour with state in classes

Date: 2026-11-05

## Status

Accepted. Amends ADR 0081 (functions are arrows) and ADR 0082 (the composition side). Builds on ADR 0057, which keeps
the hand-written stores over a state or routing library.

## Context

The hosted panes made `app.ts` the place every feature went: the sessions' cursor, the verb picker, what each row opens and
every key's routing sat in one 330-line closure, and `App.tsx` listed each pane's key bar copy. The review asked why not a
router and a store library, and to avoid nested functions. The window has one screen and no URLs, so a router would only
route keys; and every store is already one `useSyncExternalStore` read, which a library would wrap without removing.

## Decision

- **A context owns a controller**: `board/BoardController` (the Board's read and cursor) and
  `session/SessionsController` (the selected ticket's rows, their cursor, the verb picker, and what opening each row or
  pressing `n`/`N` starts). A controller takes what it needs from another context as a callback, never by import.
- **A context owns its key bar copy**: `BoardKeyBar`, `SessionsKeyBar` and `DockKeyBar`. `App.tsx` picks the one for the
  focused pane.
- **The composition side wires and routes**: `app.ts` is `AppModel`, which builds the contexts and the layout and loads
  them when the sidecar is ready. `keyRouting.ts` is `KeyRouter`, which sends a key to the picker, the focused pane or the
  layout. `keys/ConfigurationStore` holds the configuration read.
- **Behaviour that keeps state is a class with private methods**, not a factory returning closures over local variables.
  Every stateful piece of the frontend is one: `Store`, `ProtocolClient`, `BoardStore`, `SessionStore`, `Selection`,
  `Hosting`, `AppModel`, `KeyRouter`, the controllers, `ConfigurationStore`, `WindowFocus`, `Heartbeat`, `WarningStore`,
  `StorageModeMemory`, `ModeTracker`, `WatchingReporter`, `WindowKeys`, `BrowserPlatform`, and in tests `FakeClient` and
  `TestPlatform`. A named helper is declared at module level, not inside another function. A callback passed inline (a
  listener, a `map`, a JSX handler) stays an inline arrow.
- **ESLint enforces it**, in source and tests alike: `no-restricted-syntax` refuses a function assigned to a variable
  inside a function, and a function returning an object whose properties are functions. The rule was watched to fail on
  `main`'s `createStore` and on the first `createHosting`.

## Consequences

- `useStore` hands `useSyncExternalStore` stable arrows over the store's methods, since a method passed unbound loses its
  `this`; anything else taking a method as a value does the same.
- A component's handler is written inline in the JSX it serves; one too long for that is a sign the component should split.
