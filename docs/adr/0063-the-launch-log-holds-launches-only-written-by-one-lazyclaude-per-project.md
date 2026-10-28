# 63. The launch log holds launches only, written by one lazyclaude per project

Date: 2026-10-28

## Status

Accepted. Narrows ADR 0047's launch log and states the writer ADR 0062 assumes.

## Context

ADR 0047 logs one entry per launch, under the id lazyclaude names it with. `/clear` keeps the process and gives the
session a new id (ADR 0061), so the conversation a developer is in after a `/clear` is not the one the log names.

ADR 0062 writes the log whole, read-modify-write, and changes run one at a time within one JVM. Two lazyclaude on one
project, two terminals or the TUI beside the ADR 0048 desktop frontend, are two writers the log knows nothing about.

## Decision

- **Only a launch's own id is logged.** A session a `/clear` starts is never an entry, so once its tab closes it is no
  past session, and the `/ticket` runs typed after the `/clear` link nothing. Rejected: logging the new id when a
  registry record first names it, which makes reading a record write the log, for a conversation `claude --resume`
  already lists.
- **One lazyclaude per project.** Nothing checks it. Rejected: a file lock around each change plus an owner pid on each
  entry, a log format change and a cross-process protocol for a case one developer can avoid.

## Consequences

- The conversation after a `/clear` is resumed from `claude --resume`, not from the past block.
- A second lazyclaude on the same project can lose the other's entries, since each writes the log whole. It also reads
  the other's live sessions as interrupted, and `enter` then starts a second `claude` on the same transcript.
- Both are marked `deferred:` where the log is written.
