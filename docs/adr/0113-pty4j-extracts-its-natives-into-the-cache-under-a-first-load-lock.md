# 113. pty4j extracts its natives into the cache, under a first-load lock

Date: 2026-11-14

## Status

Accepted. Leaves ADR 0059's bundle as it is.

## Context

pty4j extracts `libpty.so` on the first spawn of a process. With nothing configured it extracts into a new
`java.io.tmpdir/pty4j-linux-x86-64-*` folder and marks it `deleteOnExit`, which cannot remove a folder that still holds a
file. Every run of the jar that started a session left one behind: 26 were found in `/tmp` (#128's research,
`docs/research/jlink-sidecar-bundle.md`), and a single `make test` run added five. The bundle does not have the problem, because
its relay starts the sidecar with `-Dpty4j.preferred.native.folder` pointing at natives extracted at build time (ADR 0059).

pty4j also reads `pty4j.tmpdir`, a fixed parent folder it reuses: it checks an existing extraction against its own and
rewrites it on a mismatch. Two limits come with it. pty4j falls back to a temp folder, saying nothing, when that parent does not
exist. It also assumes no two processes share the folder: two lazyclaude starting their first session together race on
the extraction, and in 13 of 30 such pairs one of them failed with `Cannot extract pty4j native linux/x86-64`.

## Decision

- **The jar points `pty4j.tmpdir` at `$XDG_CACHE_HOME/lazyclaude/pty4j`** (`~/.cache/lazyclaude/pty4j` otherwise), resolved
  like the other cache files. lazyclaude creates that folder before the first spawn, so pty4j does not fall back.
- **Each process holds an exclusive file lock on `extract.lock` in that folder around its spawns until one has returned.**
  That spawn is when pty4j extracts. A later process waits for the extraction, then finds it up to date and reuses it.
  A spawn that throws leaves the next one locked, since it may not have extracted.
- **When `pty4j.preferred.native.folder` is already set, `pty4j.tmpdir` is left unset** and nothing is locked. The bundle's
  pre-extracted `natives/` and its `-D` flags stay with ADR 0059.
- **A folder that cannot be created leaves `pty4j.tmpdir` unset and raises a warning** naming the folder and the reason.
  Sessions still start, and the temp-folder leak comes back for that run. lazyclaude takes the lock once while preparing the
  folder, so a lock that cannot be taken (a cache on NFS without lock support) takes the same path. A folder removed while
  running makes the spawn run unlocked, unwarned: pty4j then extracts into a temp folder of its own, so nothing is shared. A folder that
  cannot be used should not stop sessions from starting.
- **The composition root hands the adapter the JVM's properties, and a test a fresh table**, so building contexts in a test
  never repoints the whole test JVM's pty4j at a folder JUnit then deletes.
- **lazyclaude deletes none of the `pty4j-*` folders already in `/tmp`.** It did not create them all, and other pty4j users
  (JetBrains IDEs among them) share the prefix.

Rejected: extracting the natives at build time for the jar too, as the bundle does. That needs a second packaging step for
`make run` and the dev relay, and duplicates ADR 0059. Also rejected: a shutdown hook deleting the temp folder. It does not run
on a kill or a crash, and while one process deletes the folder another may have the library mapped.

## Consequences

- The test JVM points pty4j at `target/pty4j` through the same class, so `make test` leaves nothing in `/tmp` either.
- Updating pty4j rewrites the cached natives once, on the first run after the update. A process still running the older
  version keeps the library it already mapped.
- The lock is advisory and held only by lazyclaude. Another program pointing `pty4j.tmpdir` at the same folder is not
  guarded against.
