---
id: REVIEW-1
status: done
context: review
type: feature
tracker_id: 87
created: 2026-10-10
---

# PR and CI glyphs on the board and in the ticket detail, from the open-PR fetch on its schedule

## Acceptance criteria

- [ ] A ticket whose ticket branch is the head of an open pull request, draft PR included,
      shows a right-aligned glyph on its board row, whatever its status: `✓` green when the
      CI result is passed, `✗` red failed, `◌` yellow pending. A dim `⊙` marks a draft PR
      (beside its CI glyph) or an open PR with no checks.
- [ ] The match is the ticket branch (the ticket file's name without `.md`) equal to
      `headRefName`, exactly. A PR whose head only shares the id prefix (`REVIEW-10-…` for
      `REVIEW-1`), or is the bare id, gives no glyph; nor does a merged or closed PR, a PR
      from a fork (`isCrossRepository`), or a ticket with no open PR.
- [ ] The CI result is failed if any check is `FAILURE`, `TIMED_OUT`, `CANCELLED`,
      `ACTION_REQUIRED`, `STARTUP_FAILURE` or `ERROR`; otherwise pending if any is not
      complete (`QUEUED`, `IN_PROGRESS`, `PENDING`, `EXPECTED`); otherwise passed. A PR with
      no checks has no CI result and no CI glyph.
- [ ] The CI result is not a marker: it never changes the session marker, the status
      line's strongest marker, `w`/`alt+w` or notifications.
- [ ] The ticket detail shows, under the frontmatter rows, a `PR` row with the number, the
      title and `(draft PR)` when it is one, then one row per check (glyph and name),
      failed first, then pending, then passed, by name within each group.
- [ ] With no open PR for the ticket the detail shows a dim `PR none`; before the first
      fetch has answered, a dim `PR checking…`, and no glyphs on the board.
- [ ] When GitHub cannot be read — `gh` not on the PATH, not authenticated, offline, a
      remote that is not GitHub, or a run killed after 30 s — the board shows no PR glyph
      and the detail shows `PR unknown (<reason>)`. No warning reaches the status line,
      nothing blocks, and lazyclaude still starts.
- [ ] A fetch that fails after one succeeded clears the glyphs and shows the reason; the
      last good result is never shown as current.
- [ ] The fetch is one `gh pr list --state open --json
      number,title,headRefName,isDraft,isCrossRepository,statusCheckRollup` call at startup, then every 60 s;
      a PR opened or a check finishing shows within one interval. Two runs never overlap.

## Model decision

**The first ticket into `review`**, which the context map already lists as *expected*, so
it creates the package, its `package-info` and its glossary section — not a new context to
raise. It follows `session`'s shape: a query, an application service, a manager, a read
port and a watch port.

- **Domain.** `PullRequest` (number, title, head branch, draft or not, `Checks`). `Checks`
  list worst first by `CheckOutcome`, so the CI result is the first check's outcome, none
  without checks — no separate `CiResult` type, which would only restate `CheckOutcome`
  (settled on the PR's review). A fetch's outcome is sealed too:
  `CheckingPullRequests | OpenPullRequests | UnavailablePullRequests(reason)`, as `session`
  does with `UnavailableSessions`.
- **Ports.** `OpenPullRequestPort` reads the open pull requests; a watch port calls back on
  its own thread, as `DocsWatchPort` and `session`'s watch port do.
- **Adapters.** A `gh` repository runs the one `gh pr list` call, parses it with Jackson
  (ADR 0020), maps each check's `status`/`conclusion`/`state` to the domain, and turns any
  failure — exit code, missing binary, 30 s patience exceeded then `destroyForcibly`, as
  `ClaudeAgentsRepository` does — into the unavailable reason. The watch adapter runs it at
  startup and every 60 s on one daemon thread, which is what keeps two runs from
  overlapping.
- **Board.** `TicketBranch`, the ticket file's name without `.md`, joins `board.published`
  and `TicketView`; `BoardManager` stops dropping it. Board still knows nothing of pull
  requests. `docs/backlog/README.md`'s "matches the filename and the branch name" is
  corrected to say the branch is the filename stem.
- **Shell.** It asks `review`'s query for the open pull requests and joins each
  `TicketBranch` to a `headRefName`, as it joins the session marker. `RowLine` gains a
  right-aligned column of two cells; `TicketText.head` gains the PR rows between the
  frontmatter rows and the rule.
- **Not here.** The merged-PR fetch moves to BOARD-3, its first reader, since nothing in
  this ticket would show it. LAZY-21's push trigger and `R` arrive with BOARD-4's ref
  watchers and key. Running only while the terminal has focus waits for `?1004` decoding,
  which TamboUI does not offer; the timer carries a `// deferred:` comment naming the first
  ticket that decodes it (SESSION-7 needs it). No ADR: LAZY-19 and LAZY-21 found all of it
  cheap to reverse.

## Design direction

```
╭ Board ──────────────────────────────────────╮
│ in-review                                    │
│ • REVIEW-1 PR and CI glyphs on the board  ⊙◌ │
│   BOARD-3  Status comes from the branch…   ✓ │
╰──────────────────────────────────────────────╯
```

- Two cells at the right edge of a ticket row: the `⊙` slot, then the CI slot. The title
  is truncated before them; the session marker keeps the left gutter. Header rows have no
  column.
- Ticket detail, between the frontmatter rows and the rule, in the same key column:

```
PR       #23 PR and CI glyphs on the board (draft PR)
checks   ✗ build
         ◌ contract
         ✓ lint
```

- `PR       none`, `PR       checking…` and `PR       unknown (gh: not logged in)` are
  dimmed. No new key.

## Glossary impact

- *Pull request*, *Draft pull request*, *Checks* and *CI result* drop their *(expected)*
  marker, in `review`.
- *Ticket branch* drops its *(expected)* marker, in `board`.
- Nothing new; nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-19](LAZY-19-pr-and-ci-on-the-board.md), [LAZY-21](LAZY-21-keeping-the-screen-current.md). The merged state also feeds BOARD-3, SESSION-5 and SESSION-8.

Refined 2026-10-13: the merged-PR fetch moved to BOARD-3, so SESSION-5 and SESSION-8 get the merged state from there. The 30 s patience is longer than `claude agents --json`'s 10 s because a `gh pr list` with checks was measured at 3.4 s on 100 PRs.

Closed after [#21](https://github.com/maxenceng/lazyclaude/pull/21). The Model decision above was corrected during review and matches the code. What the code will not show:

- A `CiResult` type (`Passed | Failed | Pending | NoCiResult`, then an enum) was built and removed in review. It restated `CheckOutcome`, so the CI result is now the worst check's outcome, or none when there are no checks.
- The domain had its own copy of the branch-to-head match, and only its own test ran it. Review removed it, so the shell's join is now the one match. The context map's `review → board` edge was dropped because nothing implements it: `review` never reads `board`.
- Fork PRs (`isCrossRepository`) are left out of the fetch. Otherwise a fork pushing a branch with the same name as a ticket branch would shadow this repository's PR. SESSION-5's PR link inherits this.
- The criterion that the CI result is not a marker is only pinned for the status line. `w`/`alt+w` and notifications do not exist yet (`w` is only reserved). SESSION-7, which builds them, has to read session markers only and leave the CI result out.
