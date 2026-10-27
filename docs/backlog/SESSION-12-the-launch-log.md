---
id: SESSION-12
status: todo
context: session
type: feature
tracker_id: 95
created: 2026-10-26
---

# The launch log lists the past sessions lazyclaude started

## Acceptance criteria

- [ ] Each launch (new, `/ticket`, resume) appends an entry to the project's launch log under
      `$XDG_STATE_HOME/lazyclaude/` (`~/.local/state/lazyclaude/` when unset or not absolute): session id, directory,
      launch command, started at. A resume does not add a second entry for an id already logged.
- [ ] The Sessions panel's past block lists the logged sessions with no live process, and nothing else: a session
      ended in a plain terminal never shows.
- [ ] Only logged ids' transcripts are read, through the transcript cache; a project with hundreds of transcripts in
      `~/.claude/projects` reads none it did not start.
- [ ] An entry whose transcript is gone is dropped from the log on the next read, silently; an entry whose process is
      live is kept while its transcript is not yet written.
- [ ] An entry records an end when its `claude` exits by itself, and only then: one lazyclaude ended on quit, or that
      a crash or kill cut off, has none. A logged entry with no live process and no end is *interrupted*: the past
      block lists it first, with a marker, and `enter` resumes it. Resuming clears the end (ADR 0048).
- [ ] A `/ticket` launch is linked to its tickets with its verb from the first frame; a later `/ticket <verb> <ids>`
      in the transcript adds a link or moves the last verb. A branch name links nothing.
- [ ] A launch log that cannot be read or written raises one warning, and hosted sessions still run; one that does not
      parse warns with its `file:line`, lists no past sessions, and is not overwritten.

## Model decision

New behaviour in `session`: the launch log is a port the domain declares, a repository writes it (ADR 0047).

- Domain: a `LaunchLog` of `LaunchEntry` (session id, `WorkingDirectory`, `LaunchCommand`, `StartedAt`, and an
  end absent until `claude` exits by itself), behind
  `LaunchLogPort`. `HostedSessionManager` appends on spawn. `SessionManager` builds the past block from the log minus the
  live ids, and asks the transcript side only for the logged ids.
- The path rule for a removed worktree, `ProjectLocatorPort` lookups per transcript, and `TranscriptLocator` go: the
  entry holds the directory.
- Links: `TicketClaude`'s ids and verb make the launch link; `LinkFacts` keep only `/ticket` runs. `WeakLink` and the
  branch pattern go, so `TicketLink` has one shape.
- The transcript cache keeps ADR 0023's key and offset reads, scoped to logged ids; drop its branch facts and bump its
  format version.
- Secondary: a Jackson repository writes the log whole to a temporary file and renames it over the old one, as the cache
  does.

## Design direction

n/a: the past block and the session card keep SESSION-10's shape; only which sessions they list changes.

## Glossary impact

Adds *Launch log*: lazyclaude's own list of the sessions it started, one entry per launch, pruned when the transcript
goes. *Past session* becomes "a launch-log entry with no live process, resumable while its transcript lasts".
Adds *Interrupted session* (ADR 0048). *Linked session* loses the branch rule; *Weak link* goes and *Strong link* folds
into *Linked session*.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map. Decided in [LAZY-25](LAZY-25-what-session-keeps.md),
recorded in ADR 0047. Blocked by [SESSION-11](SESSION-11-session-id-known-at-launch.md). Marking what quitting ended as
interrupted was decided in [LAZY-26](LAZY-26-sessions-outliving-lazyclaude.md), recorded in ADR 0048.
