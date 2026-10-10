---
id: LAZY-13
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-2]
---

# What does `claude agents --json` cost and reveal when actually run?

## Question

What does `claude agents --json` cost and reveal when actually run?

LAZY-2 read its documentation and its code path but did not run it, since it might start a
supervisor daemon and write under `~/.claude`. Run it, with the user's go-ahead, and record:
whether it starts a daemon or writes anything; its startup latency (it would be polled);
whether `-p` and `--bare` sessions appear in it and in the registry; and how quickly a
session's `status` turns `waiting` once a permission prompt opens, and back once answered.

## Kind

task

## Resolution

Run on 2026-10-10 against Claude Code 2.1.294, with the user's go-ahead. The probe scripts live outside the repo.

**`claude agents --json` is safe and cheap to poll, and the registry it reads is prompt.**

- **No daemon, no writes.**
  - Three runs left `~/.claude` (outside `projects/`) byte-identical and started no process.
  - Each run takes 110–117 ms wall time, about the same with one live session or two. That's fine for a background poll every few seconds, too slow for every frame.
- **It filters stale records itself.**
  - With six records in `~/.claude/sessions/` and five of them dead, it returned only the live one.
  - Its fields are `pid`, `cwd`, `kind`, `startedAt`, `sessionId`, `name`, `status`, and `waitingFor` when waiting.
  - It has no `entrypoint`.
- **`-p` sessions appear.**
  - Both in the registry and in `agents --json`, as `kind: interactive`.
  - Only the registry file tells them apart: `entrypoint: sdk-cli`.
- **`--bare` sessions write a registry record too.**
  - They can't run at all on this machine: `--bare` skips the OAuth login and wants an API key ("Not logged in").
- **Waiting is reported at once.** An interactive `claude` on a pty in a trusted folder, polled every 20 ms:
  - `idle` → `busy` 60 ms after the prompt was sent;
  - `waiting` / `permission prompt` within the same 20 ms poll as the permission dialog being drawn;
  - back to `busy` 33 ms after the dialog was answered;
  - `idle` once the turn ended.
  - A finished turn shows as `idle`, not `waiting`/`input needed`.
- **The workspace trust dialog is invisible.**
  - In a folder Claude Code hasn't been told to trust, `claude` opens on the "Is this a project you trust?" dialog.
  - While that dialog is open there is **no registry record at all**, so neither the registry nor `agents --json` sees the session.
  - Trust isn't inherited: `/home/max/projects` is trusted, but `/home/max/projects/lazyclaude` asked.
  - Every fresh ticket worktree is such a folder.
  - *Corrected by [LAZY-15](LAZY-15-trusting-ticket-worktrees.md):* a worktree takes its main checkout's trust, so only an untrusted project asks, once. `lazyclaude` asked because a nested git repository doesn't inherit its parent folder's trust.

Side effects: the probe sessions left three short transcripts. One `-p` session sits under the scratchpad's project folder. Two interactive ones sit under `~/.claude/projects/-home-max-projects/`. No folder was marked trusted.
