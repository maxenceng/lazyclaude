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
- **Behaviour that keeps state is a class with private methods**, not a factory returning closures over local variables:
  `Hosting`, `AppModel`, `KeyRouter`, `SessionsController`, `BoardController`, `ConfigurationStore`, `WindowFocus`,
  `Heartbeat`, `WarningStore`, `StorageModeMemory`, `ModeTracker`, `WatchingReporter`. A named helper is declared at
  module level, not inside another function. A callback passed inline (a listener, a `map`) stays an inline arrow.

## Consequences

- `createStore`, `createBoardStore`, `createSessionStore`, `createSelection` and `installKeys` predate this and are still
  closures; converting them is a follow-up ticket.
- A component's own event handler (`BoardPane`'s `press`) stays inside it: it closes over props, and a class would not
  read better.
