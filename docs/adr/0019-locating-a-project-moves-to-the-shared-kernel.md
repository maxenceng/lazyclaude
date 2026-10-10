# 19. Locating a project moves to the shared kernel

Date: 2026-10-10

## Status

Accepted. Extends ADR 0010 and ADR 0017.

## Context

`ProjectLocatorPort`, its git adapter and the types they speak in (`WorkingDirectory`,
`ProjectNotFoundException`) lived in `board`, because `board` was the only context that located
a project, from the directory lazyclaude was started in. `session` has to answer the same
question for every live session's `cwd`: which project is this path in. A context may not
import another's domain (ADR 0011), and a second copy of the `git rev-parse --git-common-dir`
lookup would drift from the first, so a session in a ticket worktree could land in a different
project than the board.

## Decision

- **`ProjectLocatorPort`, `WorkingDirectory` and `ProjectNotFoundException` move to
  `shared.domain`**, beside `Project` (ADR 0010).
- **`ProjectLocatorRepository`, the git adapter, moves to `shared.infrastructure.secondary`**
  (ADR 0017's pattern: an adapter more than one context uses).
- `board` and `session` each take the port in their application service and ask it; neither
  decides what "the project of this path" means. `session` asks for each distinct `cwd` and
  reads a `ProjectNotFoundException` as "this path belongs to no project here".
- **A lookup that failed is not an answer.** When `git` cannot be run or the wait for it is
  interrupted, the port throws `ProjectUnavailableException`, not `ProjectNotFoundException`.
  `session` shows *sessions unavailable* for that read instead of dropping the session as
  nobody's, and the board's start-up reports the failure as itself. A `git` that runs and
  exits non-zero still means "not in a repository".
- The architecture rules stay as they are: the domain rule already admits `shared.domain`, and
  the adapter sits where the side-effect rule wants `git` to run.

## Consequences

- The kernel grows by two domain types, a port, two exceptions and one adapter; every context
  that locates a path is coupled to them.
- A change to how a project is located (a submodule, a bare repository) is made once and
  reaches the board and the sessions together.
- `GitFixture`, the test helper that builds real repositories, moves with the adapter.
