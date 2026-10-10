---
id: LAZY-3
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1]
---

# How is a session attributed to a ticket without the user recording anything?

## Question

How is a session attributed to a ticket without the user recording anything?

Candidates: the transcript's `gitBranch` (the ticket id is the branch name), the session's `cwd` when it runs in a worktree such as `next-suggestions-SHARED-8`, or something `/ticket` could write. What breaks each: a branch renamed or deleted, a session that switched branches mid-way, `main`, two sessions on one ticket?

## Kind

research

## Resolution

Claude Code already records everything needed, so `/ticket` writes nothing new. Sources
and numbers are in [the research note](../research/session-ticket-link.md).

**Facts.**
- Transcripts live at `~/.claude/projects/<cwd with non-alphanumerics → '-'>/<id>.jsonl`.
  Their entry format is documented as internal and changing.
- `gitBranch` is the checkout's HEAD at each message. It changes mid-session, and it is
  often stale: 15 next-suggestions sessions sat on `IDENTITY-8-close-the-ticket` while
  doing unrelated work.
- Ticket branches start with the id but vary: `-close`, `-done`, `-refine-the-ticket`,
  lowercase ids, multi-id `SYNC-2-3-4-capture`.
- `cwd` never showed the SHARED-8 worktree. That work was driven from the main checkout
  by absolute path and `cd`, and Claude Code resets a `cd` outside the project. Every
  session recorded `cwd` = main checkout and `gitBranch: main`.
- Every `/ticket <verb> <ID>` is in the transcript as `<command-args>`, or as a `Skill`
  tool_use. That was 132 of 295 sessions.
- Sessions carry `pr-link` entries. This is the documented session↔PR link.
- Measured against the 124 single-id `/ticket` sessions: `gitBranch` attributes 103 and
  adds 2 with `pr-link`. It misses `refine` on `main` and worktree-driven `start`.

**Link rule.** Link a session to an existing ticket id by the first rule that matches,
and tag the link with that rule:
1. **`/ticket` args.** This also gives the verb. A session with `/ticket` args is linked
   only to the ids it named; its branch is ignored.
2. **`pr-link`.** Map the PR to its head branch through `gh`, then match the id prefix.
3. **Any `gitBranch`.** Match `^([A-Za-z]+-\d+)` case-insensitively. This is a weak link.

Never attribute by `cwd`. Use it only to find transcripts: scan the slug of the project
root and of every `git worktree list` path, and skip `<session>/subagents/`.

Branch renames or deletions don't matter, because the transcript keeps the string.
Several sessions on one ticket is the norm (28 of 30 tickets); list them newest first,
labelled by verb.
