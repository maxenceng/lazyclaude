# Linking a Claude Code session to a backlog ticket

Research for [LAZY-3](../backlog/LAZY-3-session-ticket-link.md), 2026-10-09, Claude Code 2.1.294,
ddd-workflow plugin 2.2.2.

Question: how does lazyclaude know which ticket a session belongs to without the user
recording anything? Candidates were the transcript's `gitBranch`, the session `cwd` in a
worktree, and something `/ticket` could write.

**Short answer.** Use the `/ticket <verb> <ID>` invocation that is already in the transcript
first, then the session's `pr-link` entries, then `gitBranch`. Don't use `cwd`. Claude Code
records all three signals automatically, so `/ticket` doesn't need to write anything new.

## Sources

- **[S1]** Claude Code docs, *Manage sessions*:
  https://code.claude.com/docs/en/sessions ("Where transcripts are stored", "Use the
  session picker", "Where the session picker looks", `--from-pr`).
- **[S2]** Claude Code docs, *Run parallel sessions with worktrees*:
  https://code.claude.com/docs/en/worktrees.
- **[S3]** Claude Code docs, *Common workflows*, "Create pull requests":
  https://code.claude.com/docs/en/common-workflows.
- **[S4]** The `/ticket` command:
  `~/.claude/plugins/cache/maxence-tools/ddd-workflow/2.2.2/commands/ticket.md` and its
  `ticket-steps/{start,refine,done}.md`.
- **[S5]** This repo's `docs/backlog/README.md`.
- **[O]** What I saw on this machine, read-only: the 295 top-level transcripts in
  `~/.claude/projects/-home-max-projects-next-suggestions/`, their `subagents/`
  sub-transcripts, `git worktree list` and the branches of `~/projects/next-suggestions`,
  and `gh pr list --state all` for that repo.

## Documented facts

- Transcripts live at `~/.claude/projects/<project>/<session-id>.jsonl`, where `<project>`
  is the working directory path with every non-alphanumeric character replaced by `-`.
  Names longer than 200 characters are truncated and a hash is appended. **"The entry
  format is internal to Claude Code and changes between versions"** [S1]. Every field
  below is undocumented except the storage path.
- So a worktree at `/home/max/projects/next-suggestions-SHARED-8` slugs to
  `-home-max-projects-next-suggestions-SHARED-8`. That is a separate directory from the
  main checkout's `-home-max-projects-next-suggestions` [S1, by the stated rule].
- The `/resume` picker shows each session's **git branch**. `Ctrl+B` filters to the current
  branch and `Ctrl+W` widens the list to all worktrees of the repository [S1]. Claude Code
  itself treats branch as a session attribute.
- Claude Code **links a session to a PR** when Claude creates it with `gh pr create` or
  works on an existing PR. `claude --from-pr <n>` filters the picker to those sessions
  [S1, S3].
- When Claude enters or exits a worktree it created with `EnterWorktree`, the transcript
  moves to the new working directory's project storage [S2]. `claude --worktree <name>`
  creates `.claude/worktrees/<name>/` on a branch named `worktree-<name>` [S2]. That naming
  says nothing about the ticket.
- Hook input carries a `cwd` that follows Claude into a worktree and through `cd` [S2].
- `/ticket start` creates **a branch named `<id>-<slug>`, matching the filename**. It says
  "use a worktree if the current one has uncommitted work worth keeping" but gives no
  name or location for that worktree [S4 start.md]. `refine` notes the id ends up in the
  filename and the branch name [S4 refine.md]. `done` makes a separate close-out branch
  from `origin/main` but does not name it [S4 done.md]. The backlog README says `id`
  "matches the filename and the branch name" [S5].
- `/ticket` takes the ticket id as an argument: `refine|start|design|review|respond|done
  <ID>` [S4].

## Observed behaviour

All of these come from [O]. They describe this machine's history, not a guarantee.

### `gitBranch` is the checkout's HEAD at each message, and it moves

- No transcript was missing `gitBranch`. Within a session it changes as soon as the
  checkout changes. Of 295 sessions, 60 record more than one branch, for example
  `main → CONTENT-20-refine-the-ticket → main → CONTENT-20-domain-managers-never-nest-another-manager`.
- **It is often stale.** The branch is whatever the checkout happened to be on, not what
  the session worked on. Fifteen sessions recorded only `IDENTITY-8-close-the-ticket` while
  doing unrelated work, with titles such as "Wayfinder linking IMPORT-1 and IMPORT-24
  tickets", "IMPORT-1 map completion" and "Account deletion feature declined". Eleven
  sessions recorded branches of two different tickets, for example
  `IDENTITY-8-close-the-ticket → LIBRARY-1-rename-rating-to-library`. In 60 of the 124
  single-ticket `/ticket` sessions, the *first* recorded branch was not that ticket.
- **Branch names on this machine** start with the ticket id, but not always the canonical
  `<ID>-<slug>`:
  - close-out branches: `<ID>-close`, `-close-ticket`, `-close-the-ticket`, `-done`;
  - refine branches: `content-16-refine-the-ticket`, `shared-4-refine-the-ticket-v2`;
  - **lowercase ids**: `content-17-…`, `shared-4-…`;
  - multi-ticket branches: `SYNC-2-3-4-capture`, `SHARED-10-SHARED-12-capture-tickets`;
  - non-ticket branches: `endpoint-atlas-identity-and-library`, `main` (66 sessions touched
    `main`, 22 never left it).

  A case-insensitive prefix `^([A-Za-z]+-\d+)` recovers the id from every ticket branch.
  Multi-ticket branches yield only their first id.

### `cwd` adds nothing; the SHARED-8 worktree was never a session `cwd`

- `~/projects/next-suggestions-SHARED-8` is a `git worktree add` checkout on
  `SHARED-8-exceptions-say-what-happened`. **No transcript directory exists for its slug.**
  Every session that touched it ran from the main checkout:
  - a `start SHARED-8` session sent `backend-ddd` subagents into the worktree by absolute
    path;
  - the `review` and `respond` sessions used `cd /home/max/projects/next-suggestions-SHARED-8; …`.

  All of them, subagents included, recorded `cwd: /home/max/projects/next-suggestions` and
  `gitBranch: main`.
- The reason is that Bash `cd` outside the project is reset: the transcripts contain 746
  `Shell cwd was reset to /home/max/projects/next-suggestions` results. `cd` *within* the
  project does move `cwd` to subdirectories such as `…/docs/backlog`. That happened in 24
  sessions, and the branch didn't change.
- So a worktree sitting next to the main checkout gives no signal in a session started from
  the main checkout. A session *started inside* a worktree would record that worktree's
  `cwd` and its branch. In that case `gitBranch` already carries the ticket, so `cwd` adds
  nothing. The worktree directory name is the user's own choice: `/ticket` doesn't specify
  it, and `--worktree` uses `worktree-<name>`. Parsing it would be guesswork.
- Subagent transcripts live at `<sessionId>/subagents/agent-*.jsonl`. They carry the
  **parent's** `sessionId` and have a `.meta.json` with `agentType` and `description`.
  They are not sessions of their own.

### The `/ticket` invocation is already recorded

- 132 of 295 sessions contain a user message with
  `<command-name>/ddd-workflow:ticket</command-name><command-args>start SHARED-8</command-args>`.
  The verb counts were respond 36, done 30, start 27, review 29, refine 22 and new 5. Some
  `/ticket` runs were model-initiated: those appear as a `Skill` tool_use with
  `{"skill":"ddd-workflow:ticket","args":"review SHARED-7"}`, and the skill name can also be
  plain `"ticket"`.
- I took the 124 sessions whose `/ticket` arguments name exactly one id as ground truth and
  checked the other signals against them:

  | Signal | Sessions linked to the right ticket |
  |---|---|
  | `/ticket` args | 124 / 124 (by construction) |
  | any `gitBranch` id | 103 / 124 |
  | `pr-link` → PR head branch id | 77 / 124 |
  | `gitBranch` or `pr-link` | 105 / 124 |
  | neither | 19: 17 `refine` sessions on `main`, `start SHARED-8` (worktree), 1 `new` |

  By verb, the branch misses almost every `refine`, because there is no branch yet. It
  catches `start`, `review`, `respond` and `done` unless the work went into a worktree.

### `pr-link` entries

- 84 sessions contain
  `{"type":"pr-link","prNumber":96,"prUrl":…,"prRepository":"maxenceng/next-suggestions"}`.
  This is the documented session↔PR link [S1, S3]. 19 sessions link to more than one PR,
  and 21 of 82 PRs are linked to more than one session.
- `gh` maps a PR to its `headRefName`. That rescued the SHARED-8 `review` and `respond`
  sessions, both on `main`: PR #96 has head `SHARED-8-exceptions-say-what-happened`.
- Other entry types seen: `ai-title`, `continued-in` (`continuedInSessionId`, which chains
  one session into another), `agent-name`, `cost-state`, `last-prompt`.

### Branches disappear after merge

`git ls-remote --heads origin 'SHARED-8*'` is empty, so remote branches are deleted on
merge. Locally, `SHARED-8-close` and the worktree branch remain. This does not matter: the
transcript keeps the branch *string*, and attribution only parses that string.

## Failure cases

| Case | `gitBranch` | `cwd` | `/ticket` args | `pr-link` |
|---|---|---|---|---|
| Branch renamed or deleted afterwards | Unaffected: the old string stays in the transcript. A rename mid-session shows as a new value. | n/a | Unaffected | Unaffected (`headRefName` is kept on the PR) |
| Session switches branch mid-way | Records both: 11 sessions span two tickets, and a stale first branch is common | Doesn't move outside the project | Records each verb invoked | Records each PR |
| Session on `main` (`refine`, worktree work driven from main) | Gives nothing | Gives nothing | Works | Works once a PR exists |
| Checkout left on another ticket's branch | **Links wrongly** (the 15 IDENTITY-8 sessions) | n/a | Correct | Correct |
| Two sessions on one ticket | Normal: 28 of 30 tickets have several sessions, since each `/ticket` verb is meant to be a fresh session [S4] | — | Gives each one's verb, so they can be ordered and labelled | — |
| Work in a worktree next to the main checkout | Main checkout's branch | Main checkout | Works | Works |
| Session started inside a worktree | Works (that checkout's branch) | Stored under its own slug: you must scan for it | Works | Works |
| Multi-ticket branch (`SYNC-2-3-4-capture`) | First id only | — | Works if `/ticket` named it | — |
| Lowercase id in branch (`content-17-…`) | Works only if matched case-insensitively | — | — | — |

## Recommended link rule

A session is linked to ticket `T` (an id that exists as a file in `docs/backlog/`) by the
first of these that holds. Tag the link with the rule that made it.

1. **`/ticket` invocation.** A user message whose `<command-name>` ends in `ticket`, or a
   `Skill` tool_use whose `skill` is `ticket` or ends in `:ticket`, has args naming `T`.
   The verb comes free and labels the session ("start", "review", …). A session that
   invoked `/ticket` is linked **only** to the ids it named; its `gitBranch` is ignored.
   That removes the stale-branch false positives.
2. **`pr-link`.** The session has a `pr-link` whose PR's `headRefName` starts with `T`.
   This needs `gh`, which the PR review screen already uses. Cache it per PR number.
3. **`gitBranch`.** Any message's `gitBranch` matches `^([A-Za-z]+-\d+)` case-insensitively
   to `T`. This is the only signal for sessions not driven by `/ticket`. Show it as weaker
   ("on branch").

Do not use `cwd` for attribution. Use it only to find transcripts: scan
`~/.claude/projects/<slug>` for the project root *and* every path in `git worktree list`,
slugged by the documented rule. A worktree that has since been removed leaves its
transcripts under a slug nothing points to any more. They can be found by reading every
project directory's first `cwd`, or accepted as lost. Skip `<sessionId>/subagents/`.

"Two sessions on one ticket" is the normal case, not an edge case. The ticket detail lists
every linked session, newest first, labelled with its `/ticket` verb. Which one "go into"
picks belongs to the map's open question on several sessions per ticket.

### What `/ticket` could write

Nothing is needed. The invocation is already in the transcript, and that beats anything
the command could add. Two optional improvements to `/ticket`, both outside lazyclaude:

- `start` could name its worktree in a fixed way, such as `../<repo>-<ID>`, as the user
  already does by hand. It could also `EnterWorktree` into it, so the transcript and
  `gitBranch` follow [S2]. That is less important now that rule 1 exists.
- `done` could name its close-out branch `<ID>-close` consistently. Rule 3 already
  tolerates the variants.

Risk: every signal here comes from the internal transcript format [S1]. Parse it
defensively, ignoring unknown entry types and missing fields, and keep the parsing behind
one adapter.
