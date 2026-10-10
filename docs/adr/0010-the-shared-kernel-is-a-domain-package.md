# 10. The shared kernel's types live in `shared.domain`

Date: 2026-10-10

## Status

Accepted

## Context

`Project`, the main worktree's root, is used by `board` and will be by `session` and `review`,
so it sits in the shared kernel. `ArchitectureTest` lets a domain depend only on `java..`,
other `..domain..` packages and `..published..`. A `Project` directly under `shared` matches
none of them, so no context's domain could use it. `shared` is not a context, so `published`
does not apply to it.

## Decision

Types a context's domain needs from the shared kernel live in `shared.domain`: plain Java, no
ports, no infrastructure. The architecture rules stay as they are.

## Consequences

- Anything in `shared.domain` is held to the domain rule: nothing outside `java..`.
- A shared type that needs a framework, a process or a file is not a kernel type; it belongs in
  a context, behind that context's port.
- The kernel stays small on purpose: every addition couples every context to it.
