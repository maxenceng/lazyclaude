---
id: LAZY-1
status: in-progress
context: cross-context
type: map
created: 2026-10-09
---

# v1 of lazyclaude: the board, its sessions, the docs and PR review in one TUI

## Destination

Every decision v1 depends on is settled, so v1 can be written as refined `todo` feature
tickets under real context prefixes, ready for `/ticket start`.

v1, as scoped on 2026-10-09:

- **Board** — the current project's `docs/backlog/` (lazyclaude is run from a project root,
  like lazygit), read-only. Modelled on lazygit and lazydocker.
- **Ticket detail** — one ticket's frontmatter and body.
- **Sessions linked to a ticket** — go into a live one, resume a past one, or start a new
  one on the next `/ticket` verb: the valid verbs for its status offered, the likeliest
  preselected. Sessions lazyclaude starts or resumes run embedded in a pane, each ticket's
  in its own worktree; a session tied to no ticket can be started too.
- **Waiting sessions** — a marker in the TUI and a `notify-send` desktop notification while
  lazyclaude runs, for every session on the machine, grouped by project.
- **Docs** — a tree of `docs/**/*.md`, rendered read-only.
- **PR review** — reached from the ticket (ticket → branch → open PR): a unified diff by
  default, line comments plus a summary submitted as one *Comment* review, then an offer
  to start a `respond` session.

## Notes

- Map and decision tickets carry `LAZY-n` and `context: cross-context`. The v1 feature
  tickets they lead to take the context prefixes `BOARD`, `SESSION`, `REVIEW` and `DOCS`
  (LAZY-12), and use `docs/glossary.md`'s words.
- Grilling sessions use `mattpocock-skills:grilling` with `ddd-workflow:domain-modeling`;
  research uses `mattpocock-skills:research`, findings under `docs/research/`.
- Facts gathered while charting (2026-10-09, Claude Code 2.1.294):
  - live sessions are listed in `~/.claude/sessions/<pid>.json` (`sessionId`, `cwd`,
    `name`, `status`, `messagingSocketPath`, `peerFeatures` incl. `notify_idle`) — an
    internal, undocumented format;
  - transcripts are `~/.claude/projects/<cwd-slug>/<sessionId>.jsonl`; each message
    carries `cwd` and `gitBranch`, and `ai-title` lines name the session;
  - Claude Code's documented `Notification` and `Stop` hooks fire when a session waits or
    finishes;
  - the backlog README makes the ticket id the branch name;
  - TamboUI publishes a `tamboui-markdown` module;
  - GitHub refuses *Approve* and *Request changes* on your own PR, and every PR here is
    opened under the user's account — a review is always a *Comment* review.
- The user works in kitty on Linux, with `claude` and `gh` on the PATH.

## Decisions so far

- [What can kitty's remote control do for lazyclaude?](LAZY-5-kitty-remote-control.md): it can focus a live session's window (id from the `claude` process's `/proc/<pid>/environ`) and open tabs in a cwd, but needs `allow_remote_control socket-only` + `listen_on unix:@kitty-{kitty_pid}` and a restart — unset today; fall back to tmux under `$TMUX`, else run `claude` in lazyclaude's own terminal.
- [Which `gh` calls does the PR review need?](LAZY-6-github-pr-review-api.md): `gh pr list --head <branch>` + `gh pr diff`; threads and `isResolved` only via GraphQL; the review is one `POST /pulls/{n}/reviews` (`event: COMMENT`, `commit_id` = head SHA, `comments[]` of `path`/`line`/`side`), lines must sit in a hunk (RIGHT new, LEFT old); merged = `state == MERGED`. Writes are superseded by LAZY-11: a GitHub pending review, through GraphQL.
- [How is a session tied to a ticket?](LAZY-3-session-ticket-link.md): by the `/ticket <verb> <ID>` already recorded in the transcript first, then its `pr-link` → the PR's head branch, then a `gitBranch` id prefix as a weak signal; `cwd` only locates transcripts (across `git worktree list`), never attributes; `/ticket` writes nothing new. Several sessions per ticket is the norm (28 of 30 tickets in next-suggestions).
- [How do we know which sessions exist and their state?](LAZY-2-session-discovery.md): live sessions and state from the internal `~/.claude/sessions/<pid>.json` registry (`status` busy/shell/idle/waiting + `waitingFor`), trusted only after a `pidDomain` + `/proc/<pid>` + `procStart` check (5 of 6 records here are stale); documented `claude agents --json` as fallback and contract test; past sessions from transcripts in every `~/.claude/projects/*` folder, matched on each line's `cwd`; hooks only report transitions — recommended against for v1, LAZY-8 decides.
- [What can TamboUI 0.5.0 do for v1?](LAZY-4-tamboui-capabilities.md): covers v1 — markdown via `tamboui-toolkit-markdown`, `textArea` (submit needs a chord like Ctrl+S), focusable `list`/`tree`/`panel`, `runOnRenderThread` for background work — but a huge diff or transcript needs a custom viewport `Element`, and there is no suspend API (upstream #323): handing the terminal to `claude`/`$EDITOR` means closing the runner, running the child with `inheritIO()`, and starting a new runner, with app state kept outside it.
- [How does lazyclaude hand the user to a session?](LAZY-7-handing-over-to-a-session.md): it hosts new and resumed sessions itself, the real `claude` on a pty in a pane (feasibility: LAZY-14), dying with lazyclaude; an outside live session gets a read-only transcript view plus a kitty jump, offering to append the remote-control lines to `kitty.conf`; each ticket's sessions share one worktree `../<repo>.worktrees/<id>-<slug>`, unlinked sessions run in the project root; no `e`/`$EDITOR` anywhere.
- [Can the real `claude` TUI be embedded on a pty in a TamboUI pane?](LAZY-14-embedding-claude-on-a-pty.md): yes — pty4j 0.14.0 + jediterm-core 3.76 (JetBrains repo, not Central) drew `claude`'s fullscreen UI and took resize, Shift+Tab, paste and Ctrl+C; a custom grid `Element` costs <1 ms/frame; TamboUI's input is the weak side (Tab/Shift+Tab taken for focus, bracketed paste and mouse off by default, Ctrl+J and modified Enter lost); `--resume <id>` works from any cwd but runs in the launch cwd, so launch it in the worktree.
- [What does `claude agents --json` cost and reveal?](LAZY-13-claude-agents-spike.md): no daemon, no writes, ~115 ms a call, stale records filtered; `-p` and `--bare` sessions register too (`-p` as `entrypoint: sdk-cli`); `waiting`/`permission prompt` lands within 20 ms of the dialog and clears within 35 ms of the answer; a session on the workspace-trust dialog has no record at all (that a fresh worktree is untrusted was wrong — LAZY-15).
- [How does lazyclaude learn a session is waiting?](LAZY-8-detecting-a-waiting-session.md): `WatchService` on the registry plus a ~2 s liveness sweep, `agents --json` as fallback/contract test, no hooks; waiting = `waiting` (permission or input), `idle` is a quieter "your turn"; `sdk-cli` sessions excluded; grouped by main-worktree root; `notify-send -p` on entering waiting, closed when it clears, suppressed while you're in that pane.
- [Which `/ticket` verb is preselected when starting a session from a ticket?](LAZY-9-likeliest-next-verb.md): only the verbs `/ticket` accepts for the status are offered. Status is read from the ticket's branch copy when a branch exists (board too), else the root — once its PR is `MERGED` the branch no longer counts (LAZY-17). `in-progress` → `review`, or `design` when the branch touches `frontend/**` and no design session ran yet. `in-review` → `done` only on a confirmed `MERGED`, else `respond`. The picker opens on the status default and updates when `git`/`gh` answer, unless the cursor moved. A `done` ticket offers only an unlinked session in the project root.
- [What do the screens look like and how does one move between them?](LAZY-10-screen-layout-and-keys.md): one screen — Board/Sessions/Docs/Elsewhere panels left, a main view following the selection over a dock of hosted sessions (one tab each) right; `m` hides the main view, `z` zooms (Board → status columns); lazygit keys outside a pane, `alt+h/j/k/l` (+ `alt+m/z/w`) to move focus from anywhere, a session included; no `[ ]`/`ctrl+]` (AZERTY); markers `!` red permission, `?` yellow input, `•` cyan your turn; ADR 0008.
- [How does the PR review screen work?](LAZY-11-pr-review-interaction.md): ADR 0008's frame with Files/Comments/Review panels and the whole unified diff in the main view; threads and pending comments inline under their line, and `c` writes inline too (`v` range within one hunk, `ctrl+s` saves, `S` summary); every saved comment goes to your GitHub pending review, so leaving or quitting loses nothing, an existing one is adopted, a moved head only warns (ADR 0009); after submit, offer `/ticket respond`.
- [What are lazyclaude's bounded contexts, and their words?](LAZY-12-bounded-contexts-and-glossary.md): `board` (tickets, status from the branch copy, allowed verbs), `session` (state, link rule, preselected verb, ticket worktrees, hosting, notifications), `review` (pull request, pending review), `docs`, plus `shared` with `Project`; edges `session → board`, `session → review`, `review → board`; sessions are hosted/outside/past × linked/unlinked, states busy/your turn/waiting; words in `docs/glossary.md`, marked *expected* until code lands.
- [What happens with the workspace trust dialog in a fresh ticket worktree?](LAZY-15-trusting-ticket-worktrees.md): nothing to do — a worktree takes its main checkout's trust, so the dialog shows once per untrusted project and the user answers it in the pane; lazyclaude never writes `~/.claude.json`, checks nothing ahead, and shows the session as *starting* until its registry record appears.
- [What does lazyclaude pay at startup to read every past session, and can it be cheaper?](LAZY-16-startup-cost-of-transcripts.md): a blocking full scan is already 0.2–0.5 s for 375 transcripts (205 MB) and grows with bytes to seconds at thousands; cache each transcript's fields on path + size + mtime (~1.5 ms stat pass, ~365 B a session), draw the first frame from the registry and the cache, and rescan only new or changed files in the background; early stop barely helps because any-`gitBranch` linking needs the whole file.
- [Does the board show each ticket's PR and CI status in v1?](LAZY-19-pr-and-ci-on-the-board.md): yes — one background `gh pr list --state open --json …,statusCheckRollup` call, matched exactly on the ticket branch; a right-aligned `✓`/`✗`/`◌` CI column (failed > pending > passed, dim `⊙` for a draft PR or no checks) that is not a marker; the ticket detail lists the PR and each check; unreadable GitHub shows no glyphs and `PR: unknown`; `review` owns the CI result, `shell` joins it.
- [When is a ticket worktree removed, by whom, and what if it holds uncommitted work?](LAZY-17-removing-a-ticket-worktree.md): lazyclaude, on a y/n — offered when a hosted `/ticket done` session exits, else `d` on a `done` ticket; never on `MERGED` (the `done` session needs it); refused while dirty, with commits beyond the merged PR's head, or with a live session in it; `git worktree remove` only, branch kept; amends LAZY-9 — a branch whose PR is `MERGED` stops being a status source.
- [In what order are a ticket's several sessions listed?](LAZY-18-ordering-a-tickets-sessions.md): live (start time, newest first) before past (last activity, newest first); rows never reorder on state change; all linked sessions shown; unlinked shows live + 5 newest past + `… N older`; weak links inline with a dim `(branch)`; `enter` lands on the strongest marker, else on `n new`.
- [What is configurable in v1, and where does the configuration live?](LAZY-20-configuration.md): only the seven reserved chords (each rebindable, modifier required, no duplicates, Claude Code's `alt+h` spelling) and `notifications.desktop`, in `$XDG_CONFIG_HOME/lazyclaude/config.yml` (SnakeYAML Engine), no per-project override, read once at startup; anything invalid refuses to start with file and line; a startup warning for reserved chords bound in `~/.claude/keybindings.json` plus a contract test against Claude Code's defaults; `session` owns it and publishes the chords.
- [How do the board, the docs tree, the sessions and each branch copy stay current?](LAZY-21-keeping-the-screen-current.md): watched, never polled — `WatchService` on `docs/**`, each ticket worktree's backlog and `.git/refs/heads`+`packed-refs`; transcripts watched and parsed from the cached offset (~1 s debounce); `gh` open-PR list plus a second `--state merged` call at startup, every 60 s while focused, on refocus and on a push to a ticket branch; `R` re-reads all; a watch that can't be set up falls back to 5 s polling with a warning; selection follows ids; `shell` hands `board` the merged branches as names.

## Not yet specified

_Nothing left in the fog._

## Out of scope

- Several projects in one lazyclaude — one project per instance, like lazygit.
- Editing tickets inside the TUI, and creating them — `/ticket` and `$EDITOR` own that.
- Any editing at all, and handing the terminal to `$EDITOR` — every view is read-only
  ([LAZY-7](LAZY-7-handing-over-to-a-session.md)).
- Hosted sessions outliving lazyclaude — they die when it quits, and are resumed from their
  transcripts ([LAZY-7](LAZY-7-handing-over-to-a-session.md)).
- Git operations — lazygit exists.
- A diff view of a session's own changes, as opposed to its PR.
- Cost and token usage per session.
- GitLab, and any platform but Linux.
- Approving or requesting changes on a PR — GitHub forbids both on your own PR.
- Notifications while lazyclaude is closed — that is a Claude Code hook, not this tool.
- Search across the docs.
