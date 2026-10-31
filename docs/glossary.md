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
| Project | The root of a git repository's main worktree. lazyclaude starts only inside one, and refuses to start outside git; a session belongs to the one its `cwd` belongs to, so a ticket worktree's sessions sit under its project. | `shared` |
| Working directory | The directory lazyclaude was started in, or a session's `cwd`; the project is located from it. | `shared` |
| Warning | A degraded mode lazyclaude carries on in (the docs watch fell back to polling, the board on screen kept because it could not be read again, the sessions watch left to its sweep), or a thread that stopped on a failure nobody caught. Each degraded mode is named once, by its context's manager, however often it is reported; it shows on the status line until quit, or until its cause goes away and the manager withdraws it (the board's failed read, withdrawn by the next good one, ADR 0066), and is never printed over the screen. | `shared` |

## shell

| Term | Meaning | Where it lives |
|---|---|---|
| Status line | The top row of the screen. On the left, how many of the project's live sessions carry each marker (`! 1  ? 2  • 1`, only those present); cut with `…` before the warning. On the right, the newest warning (`⚠ <text>`, or `⚠ 2 · <text>` once there are two), cut with `…` rather than wrapped. | `shell` |
| Main view | The right column's top pane, which shows the selection of whichever left panel last had focus; for a ticket, its facts line, labels and body rendered as markdown, for a document, its markdown rendered. | `shell` |
| Dock | The bottom of the right column, holding every hosted session as a tab. With the main view hidden it fills the column. | `shell` |
| Dock tab | One hosted session's pane in the dock, labelled with its marker and name, or `starting`. | `shell` |
| Refresh | `R`: reads the board, the docs tree, the open and merged pull requests, and the sessions and transcripts again at once, off the render thread. It shows only as the rows that changed; nothing flashes and nothing says "refreshed". | `shell` |
| Zoom | `z` on the Board or the main view, or `alt+z` from those and from a hosted session: on the Board, the status columns; on the main view, that pane over the panels with the dock strip under it; on the dock, the dock alone between the status line and the key bar. Any other panel has no zoom. Moving focus ends it; in a review, `esc` ends the zoom before the review. | `shell` |
| Status columns | The Board zoomed: one column per status across the screen, the finished one narrow with only its header and count, then one for the unreadable tickets when there are any; when the issues could not be read, one saying why in place of them all. Their cursor is the Board's selection, never a second one, and skips the narrow column. | `shell` |
| Dock strip | The dock folded to one line of its tabs and markers, under the status columns or the zoomed main view. | `shell` |
| Ticket detail | What the main view shows for a ticket: its facts line (`#<number>`, status, context, type), a `labels` row for its other labels, a rule, then the issue body's `##` sections rendered as markdown. It scrolls as one unit. | `shell` |
| Review panel | The review screen's third left panel, under Files and Comments: the summary's first line, the pending count, and the moved-head warning. | `shell`, read from `review` |
| Second click | A press on the row that was already selected, in the pane that already had focus: it does what `enter` does there. A first click only focuses and selects; there is no timed double-click. | `shell` |
| Mouse setting | `mouse` in `config.yml`: `false` turns mouse capture off, for a terminal that answers in X10 and would type a click as keys. Off by default when `TERM` is `linux`. | `session` (the setting), `shell` (the capture) |

## sidecar

| Term | Meaning | Where it lives |
|---|---|---|
| Backend | The contexts' application layer and everything beneath it, whichever frontend drives it. | `sidecar`, `shell` (expected) |
| Frontend | What drives a backend and draws it: the TUI, which calls it in-process, or the desktop frontend, which drives a sidecar. Each emulates its own terminal (ADR 0049). | `shell`, `sidecar` (expected) |
| Sidecar | The backend run as a child process of the desktop frontend, `lazyclaude --sidecar`, in the project's directory. It ends when its stdin closes, and its hosted sessions end with it (ADR 0048). | `sidecar` (expected) |
| Protocol | The JSON-RPC 2.0 messages, one per line, that a frontend and its sidecar exchange: one method per `*Query`/`*Command` operation, `*/changed` and `pty/*` notifications, `ready` first (ADR 0050). | `sidecar` (expected) |
| Desktop frontend | The Tauri window, React over xterm.js, that drives a sidecar: the Board, the hosted session pane, Docs and Review, the TUI's screens and keys with the mouse. Not the *desktop notification*, which either frontend may fire. | `frontend/` (expected) |
| Relay | The desktop frontend's Rust half: it spawns the sidecar, carries its lines both ways in batches and keeps `ready` for a reloaded page. It knows none of the protocol's methods. | `frontend/src-tauri/` (expected) |
| Bundle | The desktop frontend as installed, a deb or an AppImage: the relay with the runtime, the jar and pty4j's natives beside it, built by `make bundle` (ADR 0059). Not `make package`'s jar. | `frontend/` (expected) |
| Runtime | The jlink image of Temurin 25 the bundle starts the sidecar on. | `frontend/` (expected) |

## board

| Term | Meaning | Where it lives |
|---|---|---|
| Ticket | An issue of the project's GitHub remote: its number (the ticket's id), title, labels and body. Open issues and the ones closed as completed are read; the board is not cached (ADR 0052). | `board` |
| GitHub issue | What stores a ticket: its number is the ticket's id, its `status:`, `context:` and `type:` labels and its open or closed state carry the ticket's status, context and type, its body the ticket's `##` sections (ADR 0052). Read through `gh issue list`. The only meaning *issue* has here. | `board` |
| Title | A ticket's issue title; an issue always has one. | `board` |
| Board | A project's tickets grouped by status, as the Board panel shows them. | `board` |
| Unreadable ticket | An issue the board refuses as a ticket: closed as not planned (or any reason but completed), carrying two `status:` labels or one the lifecycle has no word for, or, from `todo` until `done`, not exactly one `context:` and one `type:` label; a `draft` or a `done` one may lack them, but not carry two of either. The rules are the domain's `GitHubIssue`'s (ADR 0065). Listed under its number with its reason, never dropped silently. | `board` |
| Ticket branch | The branch `<number>-<title slug>` a ticket's work happens on, as `gh issue develop` names it by default (ADR 0052). The board derives the name from the number and title rather than reading `linkedBranches` back (ADR 0065). | `board` |
| Status | A ticket's place in its lifecycle: the issue's one `status:` label while it is open (a `draft` when it has none), and `done` once it is closed as completed, whatever labels it still carries. Nothing else is a source: no copy rides on the ticket branch (ADR 0052). | `board` |
| Unavailable board | A Board that has no tickets because the issues could not be read: the project has no GitHub remote, GitHub is unreachable, or `gh` is not logged in (the reason names `gh auth login`). The Board shows the reason in place of the groups; the next read, `R` or the poll, replaces it. | `board` |
| Issues poll | The board's watch: every 30 s a conditional request for the most recently updated issue with the last answer's `ETag`; a 304 costs nothing and the board stays, anything else reads it again. A check that fails counts as a change, so the board is read and says why it cannot be. | `board` |
| Capture | `c` on the Board: a title typed into a popup becomes a GitHub issue with no labels, read as an unassigned `draft`, its body the sections of the project's ticket template, or empty without one. lazyclaude's one ticket write; every other goes through a `/ticket` verb in a session (ADR 0054). | `board` |
| Verb | A `/ticket` verb. A ticket's allowed verbs are the ones `/ticket` accepts for its status; `design` also needs the ticket branch to touch `frontend/**`. | `board` |

## session

| Term | Meaning | Where it lives |
|---|---|---|
| Live session | A hosted session whose registry record reports a status, with the state that record reports, or `Unknown` with a warning when the record is there and cannot be read. Only the sessions lazyclaude hosts are listed at all; a `claude` in another terminal shows nowhere. | `session` |
| Registry | Claude Code's `~/.claude/sessions/<pid>.json` records, one per running process; internal and undocumented, so a record that does not parse or lacks what a session needs is refused for its own session, with a warning naming why, and the others keep their state. | `session` |
| Sweep | Re-reading the live sessions on a timer (every 2 s), always, watch or not: a session whose process died never rewrites its record, so only the sweep notices. Without a watch, the sweep is all there is. | `session` |
| Hosted session | A session lazyclaude spawned: the real `claude` on a pty, drawn in a dock tab, named by its session id from launch (`claude --session-id <uuid>` for `N` and a `/ticket` launch, the id it resumes for a resume), so its row, tab and notification carry that id before Claude Code has written anything. Its state, and the session it runs once `/clear` has replaced the one it launched, are read from the registry record of its own pid; the launch id stays its tab's. Its tab closes when `claude` exits, and it dies with lazyclaude. | `session` |
| Pty bytes | What crosses a hosted session's pty, both ways: output as `claude` wrote it, input already encoded by the frontend's own emulator. `session`'s backend never emulates. | `session` |
| Watched session | The session whose dock tab is selected while the dock and the terminal both have focus. It is never notified, and becoming watched closes its notification. The terminal counts as focused until it reports a focus-out. In the desktop frontend, the selected session pane's session while that pane and the window both have focus. | `session` (the rule), `shell` and the desktop frontend (what is watched) |
| Desktop notification | The `notify-send` notification a session's transition into waiting fires, under either frontend, titled `<project> · <session name>` with the registry's waiting text as body. Replaced in place when the session moves between waiting for permission and waiting for input; closed, over D-Bus, when it stops waiting or becomes watched. | `session` |
| Session card | The main view's facts for the past session selected in the Sessions panel: its title, what it was first asked behind `›` (left out when the title is that prompt), the tickets it is linked to with their last verb, its directory, and when it started and was last active. A transcript that cannot be read now keeps what an earlier read said, adds a line with its reason, and is headed by the short id only when nothing was ever read; `enter` resumes the session either way. Nothing tails the transcript. | `session` (the facts), `shell` (the card) |
| Launch log | lazyclaude's own list of the sessions it started in a project, one entry per launch (session id, directory, launch command, started at, and an end when its `claude` exited by itself), kept under `$XDG_STATE_HOME/lazyclaude/` (`~/.local/state/lazyclaude/` when that is unset or not absolute). A resume of a logged id adds no second entry. An entry whose transcript is gone is dropped on the next read, unless its process is live. A log that cannot be read or written warns once and hosted sessions still run; one that does not parse warns with its `file:line`, lists no past sessions and is never overwritten. | `session` |
| Past session | A launch-log entry with no live process, resumable while its transcript lasts, which makes it hosted. A session started in a plain terminal is in no log, so it is never past. A hosted session whose `claude` exited is past. | `session` |
| Interrupted session | A past session lazyclaude cut off rather than one whose `claude` exited by itself: its launch-log entry has no end, after a quit, a crash or a kill. Listed first among past sessions, with the `↺` glyph in place of the circle (not a marker), until resumed, which clears the end. | `session` |
| Transcript | Claude Code's `~/.claude/projects/<folder>/<sessionId>.jsonl`, one per session; internal format, append-only by observation. A `subagents/` file is not one. Only the transcripts of logged sessions are read. | `session` |
| Last activity | The timestamp of a transcript's last entry. It orders past sessions. | `session` |
| Linked session | A session attributed to a ticket by its `/ticket` args: the launch that began it links it from the first frame, and a `/ticket <verb> <ids>` its transcript records later adds a link or moves the last verb. The link carries the last verb the session ran on that ticket. A branch name links nothing. A second rule, `pr-link`, waits for `review` to publish head branches. | `session` |
| Unlinked session | A session attributed to no ticket. Started from lazyclaude, it runs in the project root. | `session` |
| Marker | What a live session asks of you, in the board gutter and on the status line: `!` waiting for permission, `?` waiting for input, `•` your turn; the strongest is `!` > `?` > `•`. Busy, unknown and past are not markers. | `session` |
| Starting | A hosted session with no registry record yet, or only one an earlier process of its pid left: just spawned, or still on Claude Code's trust dialog. No marker, never notified. | `session` |
| Unread | A hosted session whose registry record is there and cannot be used, or reports a status lazyclaude does not know: whether it is busy or waiting is unknown. It shows as `Unknown`, and it counts as busy, so `q` asks first. | `session` |
| Busy | The session is working; nothing is asked of you. The registry's `busy` and `shell` both show as busy. | `session` |
| Your turn | The session finished and waits for your next prompt, without blocking on anything. Shown, never notified. | `session` |
| Ticket worktree | The one worktree `../<repo>.worktrees/<ticket branch>` all of a ticket's sessions run in. Created before its first session, recreated if gone. Removed only on the user's yes, once the ticket is `done` and the worktree has no uncommitted work, no commit beyond the merged pull request, and no live session (its *Worktree state*). Removal is `git worktree remove` only; the local ticket branch stays. | `shared` (its path), `session` (creates and removes it) |
| Worktree state | A ticket worktree's dirty paths (`git status --porcelain`, untracked files included), its commits beyond the merged pull request's head that no remote ref has, and the live sessions whose `cwd` is in it; clear when all three are empty, or *no merged pull request* when the merge cannot be read, or *no sessions read* when the sessions cannot be. The detail shows it and removal is refused by it, from the one value. | `session` |
| Verb picker | The popup `n` opens on a ticket, listing its allowed verbs with the preselected one under the cursor. | `shell` |
| Preselected verb | The allowed verb the verb picker opens on, from status, past sessions and the pull request's state. | `session` |
| Waiting | The session is blocked on you: *waiting for permission* (a permission prompt) or *waiting for input* (any other question). The only state that notifies. | `session` |
| Unknown (state) | What an unread session shows: its record cannot be used, or reports a `status` lazyclaude does not know, as a newer Claude Code may. Still listed, with a grey `·` and one warning; never notified. | `session` |
| Reserved chord | A chord lazyclaude takes before `claude` sees it, inside every hosted session: the seven focus, hide, zoom and next-waiting chords, `alt+h/j/k/l/m/z/w` unless `config.yml` rebinds them. The same file binds them in the desktop frontend. | `session` |
| Fixed chord | A chord lazyclaude keeps for itself and `config.yml` cannot move — `ctrl+c` quit, `ctrl+d`/`ctrl+u` half page — so no reserved chord may be rebound onto one. | `session` |

## review

| Term | Meaning | Where it lives |
|---|---|---|
| Pull request | An open GitHub pull request on this repository, not from a fork; a ticket's pull request is the one whose head is its ticket branch. "PR" in UI text, `PullRequest` in code. | `review` |
| Merged pull request | A pull request on this repository, not from a fork, that GitHub reports merged. | `review` |
| Merged head | The head commit (`headRefOid`) of a branch's merged pull request, or none when no merged pull request is known for the branch. Published so `session` can tell what the merge covered. | `review` |
| Draft pull request | A pull request GitHub marks as draft. Always written in full ("draft PR" in UI text), never a bare "draft", which stays the ticket status. | `review` |
| Checks | The check runs and commit statuses on a pull request's head. | `review` |
| CI result | The checks collapsed into *passed*, *failed* or *pending*: failed wins over pending, pending over passed. A pull request with no checks has none. | `review` |
| Pending review | Your one unsubmitted review on a pull request, kept on GitHub. An existing one is adopted, wherever it was started. | `review` |
| Pending comment | A comment in your pending review, on one line or a range of lines inside one hunk. | `review` |
| Review screen | The screen `p` opens on a ticket's pull request: its diff, with Files and Comments beside it. Reading it is not GitHub's *review*; in code "review" is kept for the pending review and submitting. | `shell`, read from `review` |
| Diff | The pull request's unified diff, every file in one scroll. | `review` |
| Hunk | One `@@` section of a file's diff. | `review` |
| Diff line | A `+`, `-` or context line in a hunk; the only rows a cursor or a comment lands on. | `review` |
| Thread | Comments already on the pull request; *resolved* or *outdated* ones are shown differently. `ReviewThread` in code, beside `java.lang.Thread`. | `review` |
| Thread's line | The diff line a placed thread shows under: its last line, on the new side for `RIGHT` and the old side for `LEFT`. An outdated thread, or one whose line the diff does not hold, has none. Threads in diff order follow these lines; the ones with none come last. | `review` |
| Summary | What the review says as a whole: typed on the review screen, or an adopted pending review's body, and sent as its body by the submit (ADR 0042). | `review` |
| Submit | Send the pending review as a *Comment* review — the only kind GitHub allows on your own pull request. | `review` |
| Moved head | The pull request's head is no longer the commit the pending review was written on. Warned about, never blocking. | `review` |
| Reply | A pending comment answering an existing thread, sent with the pending review rather than on its own. | `review` |

## docs

| Term | Meaning | Where it lives |
|---|---|---|
| Document | One markdown file under the project's `docs/`, outside `docs/backlog/`, shown read-only. | `docs` |
| Docs tree | The project's documents grouped by folder, as the Docs panel shows them: the files directly under `docs/` first, then one header per folder with the count of documents beneath it, each level in the site's order: the folder's `README.md` first, then its other files by name, then its subfolders by name. | `docs` |
| Site | lazyclaude's own docs built into HTML by Zensical from `docs/`, `docs/backlog/` left out. lazyclaude never reads it: the Docs panel reads the markdown sources. `make docs` builds it into `site/`, and a push to `main` deploys it to GitHub Pages when the repository variable `DOCS_PAGES` is `true`. | `docs` |
| Polling | Checking the docs or the transcripts on a timer (every 5 s) in place of the watch, only when the watch cannot be set up or kept; each tree that cannot be watched is polled and warned of by itself. Not a sweep: none has polling while its watch holds. The board's *Issues poll* is its watch, not a fallback. | `docs`, `session` |

## Words we deliberately avoid

| Avoid | Use instead | Why |
|---|---|---|
| Helper, Util, Processor | A name describing what it does | These names hide missing concepts. *Manager* is reserved for a context's domain service (ADR 0011). |
| Issue, card, task | Ticket | The backlog's own word; `/ticket` and the code already use it. *Issue* names only the GitHub issue that stores a ticket. |
| Chat, conversation | Session | What Claude Code calls it, and what `claude --resume` resumes. |
| Idle (of a session) | Your turn | Claude Code's registry word; lazyclaude's states are its own. |
| Project (for `~/.claude/projects/*`) | Transcript folder | Those folders are keyed by `cwd`, so one project has one per worktree. |
| Next verb, likeliest verb | Preselected verb | One name for the picker's default. |
| Draft (of a PR review) | Pending review, pending comment | GitHub's own state name (`PENDING`); lazyclaude keeps unsent comments there, not locally. |
