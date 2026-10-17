# 16. `docs` owns the `docs/**` watcher, and a watcher is a side effect behind a port

Date: 2026-10-10

## Status

Accepted. Amends the ownership list of LAZY-21, which gave `board` the root-checkout watchers. The watch loop it describes moved to the shared kernel's tree watcher (ADR 0031).

## Context

LAZY-21 settled that `docs/**` is watched with a `WatchService` on every directory, and gave
`board` the root-checkout watchers. That predates `docs` standing alone as a context. A tree
of documents that the board watches would make `board` know about a tree it does not model.

## Decision

- **`docs` owns the watcher of `docs/**`**, outside `docs/backlog/`. `board` keeps
  `docs/backlog/`, and the ticket worktree and ref watchers.
- **The domain declares it as a port**, `DocsWatchPort`, next to `DocsPort`, and
  `DocsWatchRepository` implements it in `infrastructure.secondary`. The port says only
  "run this after each batch of changes"; every `WatchService` detail stays behind it.
- **Each batch re-reads the whole tree** through `DocsPort`. The watcher reports a change and
  never what changed, so a missed event costs nothing.
- **`ArchitectureTest` lists `java.nio.file.WatchService`** among the side effects that live in
  `infrastructure.secondary`, beside `Files`, `Process` and `java.net.http`. It was watched to
  fail on a `WatchService` in the application layer before it was trusted.
- **Polling is the fallback.** When a watch cannot be set up, or fails later, the tree is
  checked every 5 s by modification time and size, with one warning naming the tree and
  `fs.inotify.max_user_watches`.
- A project with no `docs/` directory when lazyclaude starts is not watched.

## Consequences

- `board` and `docs` each hold a watcher with no shared code. If `session` and `board` grow
  the same machinery, the shared part is a kernel type of its own, and that is a new decision.
- A `docs/` that cannot be listed, at start or on a re-read, is shown as a dim "docs unavailable"
  and the reason in the Docs panel. lazyclaude starts anyway, and the tree comes back on the next
  change that can be read.
- A `docs/` created after start shows up only after a restart.
