# 21. Sessions come from the registry after a liveness check, with `claude agents --json` as fallback

Date: 2026-10-10

## Status

Accepted. Extends ADR 0016 for sessions.

## Context

Claude Code writes one record per running process to `~/.claude/sessions/<pid>.json`. The
registry is internal and undocumented, and a record is rewritten on a state change but never
removed when its process is killed. `claude agents --json` is documented and already leaves out
dead processes, but it reports no version and no `entrypoint`, and costs a process per read.

## Decision

- **The registry is read first, on every read, and trusted only after a liveness check**: the
  record's `pidDomain` is this machine's, `/proc/<pid>` exists, and field 22 of
  `/proc/<pid>/stat` equals `procStart`. The last check is what rejects a dead pid reused by
  another process.
- **`pidDomain` is `linux:<machine id>:<pid namespace>`**, where the machine id is the content
  of `/etc/machine-id` and the pid namespace is what `/proc/self/ns/pid` links to
  (`pid:[4026531836]`). The domain is computed on each read, not cached. This was read off the
  real records: two machines' worth of records sit in one registry, and only the ones whose
  domain matches name processes in this `/proc`.
- **Field 22 is read after the last `)` of `stat`**, because the command name between the
  parentheses may hold spaces and parentheses.
- **`claude agents --json` is the fallback**, chosen by the adapter side
  (`LiveSessionRepository`), not the domain. It takes over when the registry directory is
  missing or unreadable, a record whose pid has a process does not parse, a live record lacks a
  field the domain needs, or the host's identity cannot be read. One warning names why. The
  registry is tried again on every read, so it is used again as soon as it works.
- **Both sources sit behind `LiveSessionPort`.** When neither answers, the domain returns
  `UnavailableSessions` with both reasons and lazyclaude starts anyway.
- **`session` owns its watcher** (`SessionWatchRepository`, behind `SessionWatchPort`), as `docs`
  owns its own (ADR 0016): a `WatchService` on `~/.claude/sessions/` reports a rewritten `.json`
  record at once, and a sweep every 2 s carries the rest, because a dead process never rewrites
  its record. One thread carries both. A `.key` file is ignored. A watch that cannot be set up
  leaves the sweep alone, with one warning.
- **A sweep is a re-read, not a diff**: every tick hands the shell fresh sessions, which also
  keeps ages current. The shell replaces rows cheaply and keeps the selection by session id.
- **`sdk-cli` sessions are excluded by the manager** from what the registry reports. The
  fallback has no `entrypoint`, so it lists them; that is a known difference between the two
  sources.
- **`make contract` compares the two adapters** over the live sessions on pid, session id, `cwd`,
  name and state. It is outside `make test` and CI because it needs a live `claude`; run it by
  hand after a Claude Code upgrade.

## Consequences

- A registry written by a Claude Code version this code does not understand degrades to the
  fallback with a warning, rather than showing wrong sessions.
- The fallback costs one `claude` process every 2 s while it is in use.
- Each refresh runs `git` once per distinct `cwd` to find its project, about every 2 s; the
  `deferred:` note in `SessionManager` records what closes that.
- Linux only: `/proc` and `/etc/machine-id` are read directly.
- A record whose process lives but whose JSON no longer parses switches to the fallback even
  though the other records are fine; the registry is all or nothing.
