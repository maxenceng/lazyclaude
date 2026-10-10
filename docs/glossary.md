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
| Project | The root of a git repository's main worktree. lazyclaude runs in one; sessions group under the one their `cwd` belongs to, so a ticket worktree's sessions sit under its project. A `cwd` outside git is its own project. | `shared` (expected) |

## board

| Term | Meaning | Where it lives |
|---|---|---|
| Ticket | One `docs/backlog/*.md` file: frontmatter plus body. | `board` (expected) |
| Ticket branch | The branch `<id>-<slug>` a ticket's work happens on, named from the ticket. | `board` (expected) |
| Branch copy | The ticket's file as its ticket branch has it: the ticket worktree's file, whatever branch the worktree is on, else `git show <branch>:<file>` while the branch's pull request is not merged. | `board` (expected) |
| Status | A ticket's `status`, read from its branch copy whenever there is one, and from the root checkout otherwise: no ticket branch, or a ticket branch whose pull request is merged and no ticket worktree. There is no separate root status. | `board` (expected) |
| Verb | A `/ticket` verb. A ticket's allowed verbs are the ones `/ticket` accepts for its status; `design` also needs the ticket branch to touch `frontend/**`. | `board` (expected) |

## session

| Term | Meaning | Where it lives |
|---|---|---|
| Hosted session | A live session lazyclaude spawned: the real `claude` on a pty, drawn in a dock tab. It dies with lazyclaude. | `session` (expected) |
| Outside session | A live session started anywhere else. lazyclaude shows it read-only and can jump to its terminal window. | `session` (expected) |
| Past session | A session with no live process, only a transcript. It can be resumed, which makes it hosted. A hosted session whose `claude` exited is past. | `session` (expected) |
| Linked session | A session attributed to a ticket, by the first link rule that matches: `/ticket` args, `pr-link`, or branch. | `session` (expected) |
| Weak link | A link found only through the branch name; shown as less certain. | `session` (expected) |
| Unlinked session | A session attributed to no ticket. Started from lazyclaude, it runs in the project root. | `session` (expected) |
| Starting | A hosted session with no registry record yet: just spawned, or still on Claude Code's trust dialog. No marker, never notified. | `session` (expected) |
| Busy | The session is working; nothing is asked of you. | `session` (expected) |
| Your turn | The session finished and waits for your next prompt, without blocking on anything. Shown, never notified. | `session` (expected) |
| Ticket worktree | The one worktree `../<repo>.worktrees/<ticket branch>` all of a ticket's sessions run in. Created before its first session, recreated if gone. Removed only on the user's yes, once the ticket is `done` and the worktree has no uncommitted work, no commit beyond the merged pull request, and no live session. | `session` (expected) |
| Preselected verb | The allowed verb the new-session picker opens on, from status, past sessions and the pull request's state. | `session` (expected) |
| Waiting | The session is blocked on you: *waiting for permission* (a permission prompt) or *waiting for input* (any other question). The only state that notifies. | `session` (expected) |
| Reserved chord | A chord lazyclaude takes before `claude` sees it, inside every hosted session: the seven focus, hide, zoom and next-waiting chords, `alt+h/j/k/l/m/z/w` unless `config.yml` rebinds them. | `session` (expected) |

## review

| Term | Meaning | Where it lives |
|---|---|---|
| Pull request | The open GitHub pull request whose head is a ticket branch. "PR" in UI text, `PullRequest` in code. | `review` (expected) |
| Draft pull request | A pull request GitHub marks as draft. Always written in full ("draft PR" in UI text), never a bare "draft", which stays the ticket status. | `review` (expected) |
| Checks | The check runs and commit statuses on a pull request's head. | `review` (expected) |
| CI result | The checks collapsed into *passed*, *failed* or *pending*: failed wins over pending, pending over passed. A pull request with no checks has none. | `review` (expected) |
| Pending review | Your one unsubmitted review on a pull request, kept on GitHub. An existing one is adopted, wherever it was started. | `review` (expected) |
| Pending comment | A comment in your pending review, on one line or a range of lines inside one hunk. | `review` (expected) |
| Thread | Comments already on the pull request; *resolved* or *outdated* ones are shown differently. | `review` (expected) |
| Summary | The pending review's body. | `review` (expected) |
| Submit | Send the pending review as a *Comment* review — the only kind GitHub allows on your own pull request. | `review` (expected) |
| Moved head | The pull request's head is no longer the commit the pending review was written on. Warned about, never blocking. | `review` (expected) |

## docs

| Term | Meaning | Where it lives |
|---|---|---|
| Document | One markdown file under the project's `docs/`, outside `docs/backlog/`, shown read-only. | `docs` (expected) |

## Words we deliberately avoid

| Avoid | Use instead | Why |
|---|---|---|
| Helper, Util, Processor, Manager | A name describing what it does | These names hide missing concepts. |
| Issue, card, task | Ticket | The backlog's own word; `/ticket` and the files already use it. |
| Chat, conversation | Session | What Claude Code calls it, and what `claude --resume` resumes. |
| Idle (of a session) | Your turn | Claude Code's registry word; lazyclaude's states are its own. |
| Project (for `~/.claude/projects/*`) | Transcript folder | Those folders are keyed by `cwd`, so one project has one per worktree. |
| Next verb, likeliest verb | Preselected verb | One name for the picker's default. |
| Draft (of a PR review) | Pending review, pending comment | GitHub's own state name (`PENDING`); lazyclaude keeps unsent comments there, not locally. |
