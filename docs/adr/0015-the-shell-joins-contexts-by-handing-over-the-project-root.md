# 15. The shell joins contexts by handing over the project root

Date: 2026-10-10

## Status

Accepted. Extends ADR 0012.

## Context

`docs`, and later `session` and `review`, work in the same project as `board`. Only `board`
locates it, from the directory lazyclaude was started in. A context may not import another
(ADR 0011), so the others cannot ask `board` where the project is, and none of them should
repeat the git lookup `board` already does.

## Decision

- **The root rides the board read.** `Board` carries the `Project` it was read from, and
  `BoardView` carries it as a `ProjectRoot` in `board.published`.
- **The shell hands it on.** It takes the root from the `BoardView` and passes it in the
  payload of the other context's query (`DocsPayload(root)`). The payload's `toDomain()` makes
  the `Project` of the shared kernel from it.
- **The start-up reads in this order**: the board, then the other contexts for the root it
  returned. A project that cannot be located stops there, before any other context is asked.
- No context depends on another for this. The edge is shell to context, as ADR 0012 drew it.

## Consequences

- A new context that works in the project needs no locator of its own: its query takes a root.
- The board read is the one place that can say "there is no project", so the others never see
  a root that is not one.
- If `board` ever stops being what the shell reads first, the root moves to a type of its own
  in the shell's start-up, and this decision is what reopens.
