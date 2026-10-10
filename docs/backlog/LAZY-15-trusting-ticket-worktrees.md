---
id: LAZY-15
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-7, LAZY-13]
---

# What happens with Claude Code's workspace trust dialog in a fresh ticket worktree?

## Question

What happens with Claude Code's workspace trust dialog in a fresh ticket worktree?

LAZY-7 runs every ticket session in a new worktree, `../<repo>.worktrees/<id>-<slug>`. LAZY-13 found that `claude` opens such a folder on the "Is this a project you trust?" dialog. Trust isn't inherited from a parent folder, and while that dialog is open the session has no registry record, so lazyclaude can't see its state.

The options:
- **Let the user answer it** in the embedded pane. The cost is one extra dialog per ticket, and a session lazyclaude can't track until it's answered.
- **Have lazyclaude mark the worktree trusted** before launching. That means writing Claude Code's internal state (`~/.claude.json`) on the user's behalf.
- **Place worktrees** where trust does carry over, if any such place exists.
- **Something documented**: a setting or flag, if Claude Code offers one.

## Kind

grilling

## Resolution

Grilled on 2026-10-10, against Claude Code 2.1.294.

**The user answers it themselves, once per project. lazyclaude does nothing ahead of it and never writes trust.**

- **The premise was wrong.** A fresh ticket worktree is not untrusted on its own:
  - Claude Code keys trust on the git repository root, and "in a worktree, it uses the main checkout's root" (code.claude.com/docs/en/permissions).
  - Probed on a pty: a plain `git worktree add` of the trusted claude-tools repo, outside it in a scratch folder, opened straight on the prompt. `claude -w` worktrees behaved the same.
  - LAZY-13's dialog came from `lazyclaude` itself, a repo nested in the trusted `~/projects`. A folder's trust doesn't cover a git repository nested in it, and `lazyclaude` has no trust entry of its own.
  - So the dialog shows at most once per project: on the first session in an untrusted project, in whichever worktree. Answering it trusts the main checkout and every ticket worktree with it.
- **No documented way to skip it** for interactive sessions:
  - `--dangerously-skip-permissions` still shows it;
  - only `-p` or a non-TTY stdout skip it;
  - the docs' one other route is editing `~/.claude.json` by hand.
- **lazyclaude never writes `~/.claude.json`.** It is internal, every running `claude` rewrites the whole file (a race), and trusting a repo runs its hooks, which is the user's call.
- **Starting.** A hosted session with no registry record yet is *starting*: no marker, no notification. That covers the trust dialog and the first moments of every hosted session. Added to `docs/glossary.md`.
- **Nothing ahead of the dialog.** There's no startup check of `~/.claude.json` and no hint. Ticket worktrees stay at `../<repo>.worktrees/` (LAZY-7); Claude Code's `.claude/worktrees/` layout gains nothing.
