# 47. Only the sessions lazyclaude started count, and a launch log lists them

Date: 2026-10-26

## Status

Accepted. Supersedes ADR 0035, ADR 0036 and ADR 0037. Amends ADR 0021 (no liveness check, no fallback), ADR 0023 (the
launch log replaces discovery), ADR 0024 (no branch link) and ADR 0033 (hosted sessions only).

## Context

v1 shows every Claude Code session on the machine: this project's live sessions wherever they run, the Elsewhere panel's
for every other place, a read-only transcript view of the ones lazyclaude did not start, and a jump to their kitty window
or tmux pane. Knowing about sessions lazyclaude did not start is most of `session`'s code: the registry liveness check and
its `claude agents --json` fallback, the residence read from `/proc/<pid>/environ`, the `kitty.conf` offer, a scan of
every transcript in `~/.claude/projects` located through git, and links guessed from branch names.

The desktop evolution ports `session` behind a sidecar and gives it a second frontend. Carrying all of that across only
to serve sessions started elsewhere costs more than it gives a developer who starts their sessions from the board.

## Decision

- **A session counts only if lazyclaude started it.** Outside live sessions, the `o` jump, the Elsewhere panel, the
  kitty and tmux residence, the `kitty.conf` offer and the transcript view all go. A session started in a plain terminal
  never shows, live or past.
- **lazyclaude names the session at launch.** A new or `/ticket` launch passes `claude --session-id <uuid>`, and a resume
  already names its id, so a hosted session is known by its `SessionId` from the moment it spawns. `HostedSessionId`
  goes: it existed only because the id was learned later.
- **A hosted session's state is read from its own registry record**, `~/.claude/sessions/<pid>.json`, for the pids
  lazyclaude hosts. The liveness check goes, since lazyclaude owns the process and sees it exit; the `claude agents
  --json` fallback goes too, and an unreadable record shows the session as `Unknown` with one warning. `make contract`
  checks the record's shape instead of comparing two sources. Rejected: hooks injected through `--settings`, which merge
  with the user's own hooks and are skipped under `--bare`.
- **The launch log lists the sessions lazyclaude started**: one entry per launch (session id, directory, launch command,
  started at), one file per project under `$XDG_STATE_HOME/lazyclaude/`. A past session is a launch-log entry with no
  live process. Transcripts are read only for what a row shows, and only for logged ids, through ADR 0023's cache scoped
  to them; the scan of every transcript and the path rule for a removed worktree go, since the log holds the directory.
  Rejected: transcripts as today, which would keep outside past sessions resumable against the first rule.
- **An entry whose transcript is gone is dropped** on the next read, since `claude --resume` cannot open it. One whose
  process is live is kept while its transcript is not yet written. Sessions started before the launch log existed are in
  none, and are not imported.
- **A link is known at launch, then follows `/ticket` args.** A `/ticket` launch links its session to its tickets with
  its verb from the first frame; a `/ticket <verb> <ids>` its transcript records later adds a link or moves the last
  verb, matched against the ids the shell hands over as ADR 0024 has it. The branch rule and the weak link go: they
  guessed at sessions lazyclaude knew nothing about.
- **Notifications cover hosted sessions only.** ADR 0033's rule is otherwise unchanged.

## Consequences

- The session panel lists this project's hosted sessions and its launch log, nothing else; the main view no longer has a
  transcript to show.
- `/proc` is read for nothing in `session`, which removes one of the Linux-only pieces LAZY-39 counted; `notify-send`
  and `gdbus` remain.
- The left column loses the Elsewhere panel, and CLAUDE.md's hard rule 4 no longer names `tmux`. The glossary,
  the context map and CLAUDE.md change with the code that goes, not before it.
- A session started in a plain terminal and later wanted in lazyclaude has to be resumed by hand; lazyclaude will not
  find it.
- A `claude` started in another terminal no longer blocks removing its ticket's worktree, and its `/ticket` run no longer
  preselects a verb: the worktree check and the preselection see hosted sessions only. git still refuses to remove a
  worktree with uncommitted changes.
- Whether a hosted session outlives lazyclaude, and so whether the next launch offers to resume what quitting ended, is
  LAZY-26's.
