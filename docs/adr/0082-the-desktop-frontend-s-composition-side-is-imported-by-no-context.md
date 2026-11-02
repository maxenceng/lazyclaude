# 82. The desktop frontend's composition side is imported by no context

Date: 2026-11-02

## Status

Accepted. Builds on ADR 0057. The frontend's twin of the backend's rule that nothing depends on the shell.

## Context

`keys/` holds the TUI's key table and `connection/` the screen shown while the sidecar is not there; both play the
shell's part, composed by `App.tsx`. `architecture.test.ts` listed them as shared folders, so any context could import
them, and the boundary the backend enforces with its sixth hard rule did not hold in the frontend.

## Decision

- `architecture.test.ts` lists `keys` and `connection` as the composition side. No context and no shared folder may
  import one; `App.tsx`, `app.ts` and `main.tsx` may.
- A folder that only composes contexts joins that list; a folder a context reads stays shared.

## Consequences

- A context that needs a key's meaning gets it through what the composition side hands it — a prop or a callback —
  never by importing `keys/`.
