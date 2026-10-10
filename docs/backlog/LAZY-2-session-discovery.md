---
id: LAZY-2
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1]
---

# How can lazyclaude know which Claude Code sessions exist, which are live, and what state each is in (busy, waiting for input, waiting for permission, idle)?

## Question

How can lazyclaude know which Claude Code sessions exist, which are live, and what state each is in (busy, waiting for input, waiting for permission, idle)?

Candidates: the `~/.claude/sessions/<pid>.json` registry, the transcripts under `~/.claude/projects/`, the documented hooks, or a combination. For each: is it documented, how stable across Claude Code versions, what does it cost to read, and what does it miss (a crashed session, a session in a worktree, a session in another project)?

## Kind

research

## Resolution

**Live sessions and their state: read the `~/.claude/sessions/<pid>.json` registry, and check every record against `/proc`.**

- Every running Claude Code process writes one record. The docs say this directory exists, but its format is internal.
  - `status` is `busy | shell | idle | waiting`. When it is `waiting`, `waitingFor` gives the reason (`permission prompt`, `input needed`, `dialog open`, …).
  - Claude Code rewrites the record on every change, and reading all of them takes well under a millisecond.
- Crashed sessions leave stale records. Five dead ones here still say `busy`.
  - Accept a record only when its `pidDomain` matches this machine, `/proc/<pid>` exists and `/proc/<pid>/stat` field 22 equals `procStart`.
- Map the record to the domain states:
  - `busy` or `shell` → busy;
  - `waiting` with `permission prompt` → waiting for permission;
  - `waiting` with any other reason → waiting for input;
  - `idle` → idle.
- The registry covers every project and worktree on the machine. It misses other machines and containers.

**Documented fallback: `claude agents --json`.**

- It is documented, in research preview. It returns the same `pid`, `status` and `waitingFor` for every live session, interactive and background.
- In 2.1.294 it reads the same registry.
- Use it as a second adapter behind the same port, and as a contract test.
- Before relying on it, a spike has to check whether it starts the supervisor daemon and how long it takes to start.

**Past sessions: the transcripts `~/.claude/projects/<slug>/<sessionId>.jsonl`.**

- The path and the 30-day retention are documented; the line format is not.
- Use them for history, resume, the session title (`ai-title`) and linking a session to a ticket through `gitBranch`.
- They are no use for live state: they lag, and nothing in them marks a permission wait.
- The slug is the launch directory, or the directory the session last moved to with `/cd` or a worktree, not necessarily the project. So scan every project directory and match on each line's `cwd` and `gitBranch`.

**Hooks: none in v1.**

- `Notification` (`permission_prompt` after ~6 s, `idle_prompt` after ~60 s), `Stop`, `SessionStart` and `SessionEnd` are documented and stable.
- But they need the user's settings changed or a plugin, and they only see sessions started after they are installed.
- They report transitions, not state, so lazyclaude would have to be running to see one. `SessionEnd` does not fire on a crash.

Findings and sources: [docs/research/session-discovery.md](../research/session-discovery.md).
