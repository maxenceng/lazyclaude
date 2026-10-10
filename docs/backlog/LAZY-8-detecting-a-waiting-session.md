---
id: LAZY-8
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-2, LAZY-13]
---

# How does lazyclaude learn, promptly, that a session is waiting for the user — and stop saying so once it is not?

## Question

How does lazyclaude learn, promptly, that a session is waiting for the user — and stop saying so once it is not?

By reading what Claude Code already writes, or through `Notification`/`Stop` hooks writing somewhere lazyclaude watches? If hooks: who installs them (the user, a plugin, lazyclaude on first run), into which settings file, and what do they write?

## Kind

grilling

## Resolution

**lazyclaude reads the session registry. It installs no hooks.**

**Source.**
- lazyclaude watches `~/.claude/sessions/*.json` with `WatchService` (inotify on Linux). Claude Code rewrites a record on every change, and LAZY-13 measured `waiting` within 20 ms of the dialog and back within 35 ms of the answer.
- A sweep every ~2 s re-checks liveness (`pidDomain`, `/proc/<pid>`, `procStart`, per LAZY-2). A crashed process never rewrites its record, so the sweep is what drops it.
- `claude agents --json` sits behind the same port. It's the fallback adapter, and a contract test that the registry still agrees with it.
- No `Notification`/`Stop` hooks.

**States.**
- `waiting` + `permission prompt` → *waiting for permission*.
- `waiting` with any other reason → *waiting for input*.
- Both of those count as **waiting**.
- `idle` → *your turn*: shown, but quieter, and not waiting.
- `busy`/`shell` → busy.

**Which sessions.** Every live session on the machine, except `entrypoint: sdk-cli` (`-p` and SDK runs), which has no terminal to answer in. That field exists only in the registry.

**Grouping.**
- Sessions group by project: the main worktree's root, from `git rev-parse --git-common-dir` run in the session's `cwd`.
- Ticket worktrees therefore sit under their project.
- A `cwd` outside git groups under itself.

**Desktop notification.**
- `notify-send -p` fires once on each transition into waiting. The title is the project and the session name; the body is the `waitingFor` reason.
- lazyclaude keeps the returned id. When the session leaves waiting, or dies, it closes that notification or replaces it with `-r`.
- Nothing fires for *your turn*.
- Nothing fires while you're already looking at that session: its hosted pane has focus and the terminal has focus (focus reporting, `?1004`).

**Marker.**
- Each session shows one of three markers: waiting for permission, waiting for input, or your turn.
- A ticket on the board shows the strongest marker among its sessions, and the strongest marker overall shows in a status line.
- Glyphs, colours and placement are LAZY-10's.

**Blind spot.** A session on the workspace trust dialog has no record (LAZY-13). That is LAZY-15's to settle.
