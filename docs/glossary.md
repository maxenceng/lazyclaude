# Ubiquitous language

The words below are the ones that appear in code. If a discussion introduces a new word
for an existing concept, either rename the concept everywhere or add the new word here —
never let two names for one thing coexist.

Keep this file short. A glossary nobody trusts is worse than none.

Add a section per bounded context, and add the term in the same change that introduces
it in code. A term settled by a decision ticket before any code exists may land early,
marked `(expected)` in *Where it lives*; the change that introduces it drops the marker. A glossary updated in a separate pass documents what survived, not what the
words mean.

## shared

| Term | Meaning | Where it lives |
|---|---|---|
| Project | The root of a git repository's main worktree. lazyclaude starts only inside one, and refuses to start outside git; sessions group under the one their `cwd` belongs to, so a ticket worktree's sessions sit under its project. A session whose `cwd` is outside git is grouped on its own, named by that directory. | `shared` |
| Working directory | The directory lazyclaude was started in, or a session's `cwd`; the project is located from it. | `shared` |
| Warning | A degraded mode lazyclaude carries on in (the docs or board watch fell back to polling, the board on screen kept because it could not be read again, the sessions watch left to its sweep, live sessions read from `claude agents`), or a thread that stopped on a failure nobody caught. Each degraded mode is named once, by its context's manager, however often it is reported; it shows on the status line until quit, and is never printed over the screen. | `shared` |

## shell

| Term | Meaning | Where it lives |
|---|---|---|
| Status line | The top row of the screen. On the left, how many of the project's live sessions carry each marker (`! 1  ? 2  • 1`, only those present), then `elsewhere:` and each other place with a waiting session, an outside-git directory included, under its strongest waiting marker; cut with `…` before the warning. On the right, the newest warning (`⚠ <text>`, or `⚠ 2 · <text>` once there are two), cut with `…` rather than wrapped. | `shell` |
| Main view | The right column's top pane, which shows the selection of whichever left panel last had focus; for a ticket, its frontmatter and its body rendered as markdown, for a document, its markdown rendered. | `shell` |
| Dock | The bottom of the right column, holding every hosted session as a tab. With the main view hidden it fills the column. | `shell` |
| Dock tab | One hosted session's pane in the dock, labelled with its marker and name, or `starting`. | `shell` |
| Refresh | `R`: reads the board, the docs tree, the open and merged pull requests, and the sessions and transcripts again at once, off the render thread. It shows only as the rows that changed; nothing flashes and nothing says "refreshed". | `shell` |
| Ticket detail | What the main view shows for a ticket: its facts line, its other frontmatter keys, a rule, then its body rendered as markdown. It scrolls as one unit. | `shell` |

## board

| Term | Meaning | Where it lives |
|---|---|---|
| Ticket | One `docs/backlog/*.md` file: frontmatter plus body. | `board` |
| Title | The first `# ` line of a ticket's body outside a code fence, stripped; a ticket without one is shown under its id. | `board` |
| Board | A project's tickets grouped by status, as the Board panel shows them. | `board` |
| Unreadable ticket | A backlog file with an `id` whose frontmatter or status is refused, or whose `id` another file already claims; listed with its reason, never dropped silently. | `board` |
| Ticket branch | The branch `<id>-<slug>` a ticket's work happens on, the ticket file's name without `.md`. | `board` |
| Branch copy | The ticket's file as its ticket branch has it: the file in a worktree checked out on the ticket branch, the root checkout included, or in the ticket worktree whatever branch it is on, uncommitted edits shown; else `git show <branch>:<file>` while the branch's pull request is not merged. | `board` |
| Root copy | The ticket's file in the root checkout, as the backlog read finds it. | `board` |
| Status | A ticket's `status`, read from its branch copy whenever there is one, and from the root checkout otherwise: no ticket branch, or a ticket branch whose pull request is merged and no worktree. A root copy reading `done` wins over any branch copy. There is no separate root status. | `board` |
| Verb | A `/ticket` verb. A ticket's allowed verbs are the ones `/ticket` accepts for its status; `design` also needs the ticket branch to touch `frontend/**`. | `board` |

## session

| Term | Meaning | Where it lives |
|---|---|---|
| Live session | A session whose process runs, as the registry lists it after the liveness check, or `claude agents --json` when the registry is unusable. `sdk-cli` sessions are not listed from the registry. | `session` |
| Registry | Claude Code's `~/.claude/sessions/<pid>.json` records, one per running process; internal and undocumented, so never trusted without the liveness check. | `session` |
| Sweep | Re-reading the live sessions on a timer (every 2 s), always, watch or not: a session whose process died never rewrites its record, so only the sweep notices. Without a watch, the sweep is all there is. | `session` |
| Hosted session | A live session lazyclaude spawned: the real `claude` on a pty, drawn in a dock tab. Started by `N` or by resuming an unlinked past session; its tab closes when `claude` exits, and it dies with lazyclaude. | `session` |
| Outside session | A live session started anywhere else. lazyclaude shows it read-only and can jump to its terminal window. Its row is labelled `tmux` when its process carries `TMUX_PANE`, else `kitty` when it carries `KITTY_WINDOW_ID`, else its age; a session lazyclaude hosts is never outside, under Elsewhere either. | `session` |
| Transcript view | The main view's read-only, tailing render of the transcript of the outside or past session selected in the Sessions panel: your prompts behind `›`, Claude's replies, and one dim `⏺ <Tool>  <argument>` line per tool call; tool results and thinking are not shown. It follows new entries while scrolled to the bottom and holds its place when scrolled up. A session with no transcript yet says so, and fills in once it appears. | `session` (the read), `shell` (the view) |
| Jump | `o` on an outside session in the Sessions or Elsewhere panel: brings its kitty window or tmux pane forward. In tmux only when lazyclaude runs in the same tmux server. A jump that cannot be made warns on the status line why. When kitty refuses because remote control is off and lazyclaude runs in kitty, a popup offers the `kitty.conf` lines. | `session` (the jump), `shell` (the key and popup) |
| Elsewhere | Every live session outside this project, grouped by place; places and the sessions in them run strongest marker first, then by name. The Elsewhere panel lists them under always-expanded headers the cursor skips; `enter` does nothing. | `session` (the groups), `shell` (the panel) |
| Place | Where a live session belongs: its project, ticket worktrees included, removed ones by their `../<repo>.worktrees/` path, or, when its `cwd` is outside git, that directory. Named by the project's or the directory's name; Elsewhere and the status line group by it. | `session` |
| Watched session | The session whose dock tab is selected while the dock and the terminal both have focus. It is never notified, and becoming watched closes its notification. The terminal counts as focused until it reports a focus-out. | `session` (the rule), `shell` (what is watched) |
| Desktop notification | The `notify-send` notification a session's transition into waiting fires, titled `<project> · <session name>` with the registry's waiting text as body. Replaced in place when the session moves between waiting for permission and waiting for input; closed, over D-Bus, when it stops waiting or becomes watched. | `session` |
| Past session | A session with no live process, only a transcript. It can be resumed, which makes it hosted. A hosted session whose `claude` exited is past. | `session` |
| Transcript | Claude Code's `~/.claude/projects/<folder>/<sessionId>.jsonl`, one per session; internal format, append-only by observation. A `subagents/` file is not one. | `session` |
| Last activity | The timestamp of a transcript's last entry. It orders past sessions. | `session` |
| Linked session | A session attributed to a ticket, by the first link rule that matches: `/ticket` args, or branch. A third rule, `pr-link`, waits for `review` to publish head branches. | `session` |
| Strong link | A link made by the session's own `/ticket` args; it carries the last verb the session ran on that ticket. | `session` |
| Weak link | A link found only through the branch name; shown as less certain, with a dim `(branch)` and no verb. | `session` |
| Unlinked session | A session attributed to no ticket. Started from lazyclaude, it runs in the project root. | `session` |
| Marker | What a live session asks of you, in the board gutter and on the status line: `!` waiting for permission, `?` waiting for input, `•` your turn; the strongest is `!` > `?` > `•`. Busy, unknown and past are not markers. | `session` |
| Starting | A hosted session with no registry record yet: just spawned, or still on Claude Code's trust dialog. No marker, never notified. | `session` |
| Unread | A hosted session while the registry cannot be read: whether it is starting, busy or waiting is unknown. Its tab reads `starting`, and it counts as busy, so `q` asks first. | `session` |
| Busy | The session is working; nothing is asked of you. The registry's `busy` and `shell` both show as busy. | `session` |
| Your turn | The session finished and waits for your next prompt, without blocking on anything. Shown, never notified. | `session` |
| Ticket worktree | The one worktree `../<repo>.worktrees/<ticket branch>` all of a ticket's sessions run in. Created before its first session, recreated if gone. Removed only on the user's yes, once the ticket is `done` and the worktree has no uncommitted work, no commit beyond the merged pull request, and no live session. | `shared` (its path), `session` (creates it) |
| Verb picker | The popup `n` opens on a ticket, listing its allowed verbs with the preselected one under the cursor. | `shell` |
| Preselected verb | The allowed verb the verb picker opens on, from status, past sessions and the pull request's state. | `session` |
| Waiting | The session is blocked on you: *waiting for permission* (a permission prompt) or *waiting for input* (any other question). The only state that notifies. | `session` |
| Unknown (state) | The session reports a `status` lazyclaude does not know, or none, as a newer Claude Code may. Still listed, with a grey `·`; never notified. | `session` |
| Reserved chord | A chord lazyclaude takes before `claude` sees it, inside every hosted session: the seven focus, hide, zoom and next-waiting chords, `alt+h/j/k/l/m/z/w` unless `config.yml` rebinds them. | `session` (expected) |

## review

| Term | Meaning | Where it lives |
|---|---|---|
| Pull request | An open GitHub pull request on this repository, not from a fork; a ticket's pull request is the one whose head is its ticket branch. "PR" in UI text, `PullRequest` in code. | `review` |
| Merged pull request | A pull request on this repository, not from a fork, that GitHub reports merged. | `review` |
| Draft pull request | A pull request GitHub marks as draft. Always written in full ("draft PR" in UI text), never a bare "draft", which stays the ticket status. | `review` |
| Checks | The check runs and commit statuses on a pull request's head. | `review` |
| CI result | The checks collapsed into *passed*, *failed* or *pending*: failed wins over pending, pending over passed. A pull request with no checks has none. | `review` |
| Pending review | Your one unsubmitted review on a pull request, kept on GitHub. An existing one is adopted, wherever it was started. | `review` (expected) |
| Pending comment | A comment in your pending review, on one line or a range of lines inside one hunk. | `review` (expected) |
| Thread | Comments already on the pull request; *resolved* or *outdated* ones are shown differently. | `review` (expected) |
| Summary | The pending review's body. | `review` (expected) |
| Submit | Send the pending review as a *Comment* review — the only kind GitHub allows on your own pull request. | `review` (expected) |
| Moved head | The pull request's head is no longer the commit the pending review was written on. Warned about, never blocking. | `review` (expected) |

## docs

| Term | Meaning | Where it lives |
|---|---|---|
| Document | One markdown file under the project's `docs/`, outside `docs/backlog/`, shown read-only. | `docs` |
| Docs tree | The project's documents grouped by folder, as the Docs panel shows them: the files directly under `docs/` first, then one header per folder with the count of documents beneath it, each level sorted by name. | `docs` |
| Polling | Checking the docs, the transcripts, or the backlog, the worktrees' backlogs and the refs the board is read from, on a timer (every 5 s) in place of the watch, only when the watch cannot be set up or kept; each tree that cannot be watched is polled and warned of by itself. Not a sweep: none has polling while its watch holds. | `docs`, `session`, `board` |

## Words we deliberately avoid

| Avoid | Use instead | Why |
|---|---|---|
| Helper, Util, Processor | A name describing what it does | These names hide missing concepts. *Manager* is reserved for a context's domain service (ADR 0011). |
| Issue, card, task | Ticket | The backlog's own word; `/ticket` and the files already use it. |
| Chat, conversation | Session | What Claude Code calls it, and what `claude --resume` resumes. |
| Idle (of a session) | Your turn | Claude Code's registry word; lazyclaude's states are its own. |
| Project (for `~/.claude/projects/*`) | Transcript folder | Those folders are keyed by `cwd`, so one project has one per worktree. |
| Next verb, likeliest verb | Preselected verb | One name for the picker's default. |
| Draft (of a PR review) | Pending review, pending comment | GitHub's own state name (`PENDING`); lazyclaude keeps unsent comments there, not locally. |
