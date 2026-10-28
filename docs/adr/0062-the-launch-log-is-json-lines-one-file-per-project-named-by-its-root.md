# 62. The launch log is JSON Lines, one file per project named by its root

Date: 2026-10-28

## Status

Accepted. Settles the file shape ADR 0047 leaves open.

## Context

ADR 0047 keeps one launch log per project under `$XDG_STATE_HOME/lazyclaude/` and refuses a log that does not parse
rather than overwriting it. A refusal is only useful if it says where the log went wrong, and a single JSON document
reports a position in a stream, not the entry a person would fix by hand. Two projects can share a directory name.

## Decision

- **The log is JSON Lines**: one entry per line, so a refusal names the `file:line` of the entry it could not use.
- **A file is named `launch-log-<root directory name>-<8 hex of the root path's hash>.jsonl`**: the name says which
  project it is, the hash keeps two projects that share that name apart.
- The whole log is written to a temporary file and renamed over the old one, as the transcript cache is.
- No format version line: nothing older exists to read.
- **The log's own adapter warns once, not the managers**: `LaunchLogRepository` tells the fallback each call carries, on its first failure since it was built (an unreadable, unparseable or unwritable log, with `file:line` when it did not parse), and the domain raises that one warning.

## Consequences

- Moving or renaming a project's root starts it an empty log; its past sessions stay in the old file, unread.
- A change to an entry's shape that an older lazyclaude cannot read needs a version, added with that change.
