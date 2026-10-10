# Session discovery: which Claude Code sessions exist, which are live, what state each is in

Researched 2026-10-09 against Claude Code 2.1.294 on Linux (CachyOS, kernel 7.2.9).

Each claim is tagged:

- **[doc]**: stated in the official Claude Code docs, with the page linked.
- **[obs]**: observed on this machine, in files under `~/.claude` (read-only) or in strings
  from the installed binary `/opt/claude-code/bin/claude` (2.1.294). Observed behaviour can
  change in any release.

## Answer in brief

- **Live sessions and their state.** Every running Claude Code process writes
  `~/.claude/sessions/<pid>.json`. The file's `status` is `busy`, `shell`, `idle` or
  `waiting`. When the status is `waiting`, `waitingFor` gives the reason, for example
  `permission prompt` or `input needed`. The docs say this directory exists but do not
  document its format.
  - The same data is available through a documented interface: **`claude agents --json`**
    returns `pid`, `status` (`busy | waiting | idle`) and `waitingFor` for every live session,
    interactive and background. In 2.1.294 that command reads the same registry files.
- **Past sessions.** These are the transcripts, `~/.claude/projects/<slug>/<sessionId>.jsonl`.
  - The docs name the path and the retention rule (30 days by default), but not the line
    format.
  - `<slug>` comes from the directory the session was **launched** from, or the directory it
    last moved to with `/cd` or a worktree. It is not necessarily the project the session
    worked on.
- **Hooks.** Hooks are documented and stable, and they fire on transitions: `Notification`
  for `permission_prompt` and `idle_prompt`, and `Stop`, `SessionStart`, `SessionEnd`,
  `UserPromptSubmit` and others. But they only report sessions started after the hook was
  installed, they need the user's settings changed, and a hook only records a transition when
  it happens, so lazyclaude would have to be running then to see it. They are not needed for
  v1.
- **Recommendation.** Read the registry directly for liveness and state. Check each record
  against `/proc` so a crashed session's leftover file is not taken as live. Keep
  `claude agents --json` as the documented fallback and as the contract to test against. Use
  the transcripts for history, resume and linking a session to a ticket. Leave hooks out of
  v1.

## 1. The registry: `~/.claude/sessions/<pid>.json`

### What the docs say

- [doc] "`sessions/`: holds one small file per running session, used to detect concurrent
  sessions and crashes. It isn't part of the age-based sweep: Claude Code removes each file
  when its session exits and clears crash leftovers on the next launch."
  ([.claude directory, Application data](https://code.claude.com/docs/en/claude-directory))
- [doc] The field names and value sets are **not** documented on that page. The closest
  documented schema is `claude agents --json` (section 2), whose fields match the registry's.
- [doc] Each session binds an inbox socket for cross-session messaging (v2.1.224+). Its path
  appears in `/status` and in `CLAUDE_CODE_MESSAGING_SOCKET`. A session started in bare mode
  binds no socket and "doesn't appear in the agent list".
  ([Cross-session messaging](https://code.claude.com/docs/en/cross-session-messaging))

### What a record contains [obs]

This is the record for the session that wrote this note, as it appears on disk:

```json
{"pid":407182,"sessionId":"5ee0851c-…","cwd":"/home/max/projects","startedAt":1791577486868,
 "procStart":"594735","version":"2.1.294","peerProtocol":1,
 "peerFeatures":["notify_idle","reply_across_default_dirs","artifact_yield"],
 "kind":"interactive","entrypoint":"cli",
 "pidDomain":"linux:8627a0d9…:pid:[4026531836]",
 "messagingSocketPath":"/run/user/1000/cc-socks/407182.sock",
 "name":"projects-ac","nameSource":"derived","nameSince":1791577486868,
 "status":"busy","updatedAt":1791580407517,"statusUpdatedAt":1791580407517}
```

The binary's reader for these records (2.1.294) shows the allowed values for each field:

| Field | Values and meaning |
| - | - |
| `status` | `busy`, `shell`, `idle`, `waiting`. Anything else is read as absent. |
| `waitingFor` | Set only when `status` is `waiting`. Values found in the binary: `permission prompt`, `input needed`, `dialog open`, `worker request`, `sandbox request`. |
| `kind` | `interactive`, `bg`, `daemon`, `daemon-worker` |
| `entrypoint` | for example `cli`. The binary also knows `claude-desktop`, `claude-vscode` and `local-agent`. |
| `nameSource` | `user`, `peer`, `derived`, `collision`, `auto`, `hook` |
| `procStart` | The process start time: field 22 (`starttime`) of `/proc/<pid>/stat`. On this machine it is `594735`, matching `/proc/407182/stat`. |
| `pidDomain` | `linux:<machine-id>:pid:<pid-namespace inode>`. Matches `/etc/machine-id` and `readlink /proc/self/ns/pid`. |

The binary also reads several optional fields: `jobId`, `parkedJobId`, `bridgeSessionId`,
`hostSessionId`, `logPath`, `tmux`, `spare`, `agent`, `state`, `detail`, `tempo`
(`active`/`idle`/`blocked`) and `needs`.

How the status gets set, from the binary:

- A function derives it from the UI state. It returns `waiting` when an elicitation, a
  permission or other dialog, a worker request or a sandbox request is pending. Otherwise it
  returns `busy` when a query is loading and `idle` when not.
- For background workers there is a mapping `{running:"busy", requires_action:"waiting",
  idle:"idle"}`. In that mapping `waitingFor` is `input needed` for a dialog or question tool,
  and `permission prompt` otherwise.
- Each write sets both `updatedAt` and `statusUpdatedAt`.

How Claude Code decides a record is live: it checks `process.kill(pid, 0)` and then confirms
the recorded `procStart` matches the running process. That second check catches a PID that
has been reused by another process.

### Live versus stale, as seen here [obs]

There are six records. Only `407182` (this session) is alive. The other five, from
2026-08-27 to 2026-08-29, point to dead PIDs:

- Two of them still say `"status":"busy"`.
- All five carry another machine's `pidDomain` (machine-id `96044f50…`) and a
  `/home/maxenceng` cwd. They look like files from a migrated home directory.

So the documented promise that Claude Code "clears crash leftovers on the next launch" did
not cover these files. It apparently only reaps records from its own PID domain. **A reader
must never trust `status` without checking that the process is alive.** The check takes
three steps:

1. Compare the record's `pidDomain` with this machine's.
2. Check that `/proc/<pid>` exists.
3. Check that `/proc/<pid>/stat` field 22 equals `procStart`.

The `.key` files beside the records hold messaging auth tokens. A reader should skip them.

### Cost, stability, blind spots

- **Cost** [obs]:
  - Parsing all six files took about 0.3 ms.
  - The directory is small and flat, so it suits a `WatchService`.
  - Claude Code rewrites a record on every status change, so a watcher sees those changes
    almost immediately.
- **Stability**: internal. The field set has grown across versions [obs]: records from
  2.1.247 lack `reply_across_default_dirs` in `peerFeatures`, and one 2.1.247 record has no
  `messagingSocketPath`. Field names are shared with the documented `claude agents --json`,
  which lowers the risk somewhat.
- **What it misses**:
  - Sessions on other machines and cloud sessions [doc].
  - Sessions inside a container, which have a different PID namespace and possibly no shared
    `~/.claude` [doc and obs].
  - Bare-mode sessions, which bind no socket. Whether they also skip the registry file was
    not checked.
  - Dead sessions: they appear only as stale files or not at all.
- **Worktrees and other projects**: covered. There is one file per process across all
  projects. `cwd` is the session's working directory, and a worktree session's `cwd` is the
  worktree path [obs]. That path is under `<repo>/.claude/worktrees/<name>` by default
  [doc: [Worktrees](https://code.claude.com/docs/en/worktrees)].
- **Caveat about `cwd`**: this session's record says `cwd: /home/max/projects` although it
  is working on `lazyclaude`. `cwd` is where the session was started or last moved to, not
  every directory it touched.

## 2. `claude agents --json`: the documented view of the same data

From [Agent view](https://code.claude.com/docs/en/agent-view) [doc]:

- "`claude agents --json` prints active sessions as a JSON array and exits: every live
  session, plus background sessions that are still working or blocked even when their
  process has exited."
- `--all` adds completed background sessions.
- `--cwd <path>` limits the list to sessions started under that directory.
- The fields are:
  - always: `cwd`, `kind` (`interactive`/`background`), `startedAt`;
  - for background sessions: `id` and `state` (`working | blocked | done | failed |
    stopped`);
  - while the process is alive: `pid` and `status` (`busy | waiting | idle`);
  - when `status` is `waiting`: `waitingFor` (`permission prompt | input needed |
    sandbox request | worker request | dialog open`);
  - when set: `sessionId` and `name`.
- "Agent view is in research preview." The page also warns that `~/.claude/jobs/<id>/` "is
  not a stable interface" and recommends `claude agents --json` for scripts.

[obs] In 2.1.294 the function `printAgentsJson` builds that array as follows:

- It joins the live registry records with the background jobs under `~/.claude/jobs`.
- It maps `shell` to `busy`.
- It skips records with a `parkedJobId`.
- It sorts the result by `startedAt`.

So the command is the registry plus jobs, behind a documented contract.

The command was **not executed** here. No supervisor daemon was running, and running the
command might start one or write under `~/.claude`, which this research had to leave
untouched. The binary's `printAgentsJson` itself shows no daemon start, but that needs
confirming in a spike.

**Cost**: one process spawn of a 250 MB Bun binary per poll. `claude --version` returns in
about 6 ms, but that is a fast path. A full `agents --json` start-up was not measured.
Polling it every second for a live marker is likely heavier than watching a directory.

## 3. Transcripts: `~/.claude/projects/<slug>/<sessionId>.jsonl`

### What the docs say

- [doc] `projects/<project>/<session>.jsonl` holds the "Full conversation transcript: every
  message, tool call, and tool result". Subagent transcripts are under
  `<session>/subagents/` and spilled tool output under `<session>/tool-results/`.
  ([.claude directory](https://code.claude.com/docs/en/claude-directory))
- [doc] They are deleted after `cleanupPeriodDays`, which defaults to 30 days with a minimum
  of 1. Set-aside copies named `*.orphaned-*.jsonl` and `*.jsonl.superseded-*` don't appear
  in the session picker.
- [doc] The slug is shown by example: `/home/user/work/my-repo` becomes
  `-home-user-work-my-repo` (`claude purge --dry-run`).
- [doc] The line format is not documented.
- [doc] Hooks warn the transcript "is written asynchronously, so it may lag"
  ([Hooks](https://code.claude.com/docs/en/hooks)).
- [doc] "When Claude enters or exits a worktree that Claude Code created with git, the
  transcript follows: Claude Code records the session under the session's new working
  directory, the same way `/cd` does … A worktree created by a `WorktreeCreate` hook keeps
  its transcript at the launch directory." This needs v2.1.198 or later.
  ([Worktrees](https://code.claude.com/docs/en/worktrees))

### Observed [obs]

- There are 9 project directories, 364 top-level transcripts, 208 subagent transcripts and
  336 MB in total.
  - The median transcript is 370 KB and the largest 4.8 MB.
- Line `type`s seen:
  - `user`, `assistant`, `attachment`, `system`;
  - `ai-title` (field `aiTitle`), `last-prompt`, `mode`, `permission-mode`;
  - `file-history-snapshot`, `file-history-delta`, `atis-latch`.
- `system` subtypes seen: `stop_hook_summary`, `turn_duration`, `away_summary`.
- Message lines carry `sessionId`, `cwd`, `gitBranch`, `version`, `entrypoint` and
  `timestamp`.
- The slug replaces every non-alphanumeric character with `-`.
- **Per-line `cwd` follows the session; the directory doesn't.**
  - 29 of 364 transcripts contain more than one `cwd`.
  - Many lines under `-home-max-projects-next-suggestions` carry subdirectory `cwd`s, from
    `cd`s inside the project.
  - This session lives under `-home-max-projects` although it works on `lazyclaude`.
  - To find a project's sessions, scan every project directory and match on `cwd` and
    `gitBranch`. Listing one slug directory is not enough.
- Five project directories belong to the old `/home/maxenceng` home. They are history from
  another machine.

### Fit

- **Good for**:
  - the list of past sessions to resume;
  - the session title (`ai-title`);
  - linking a session to a ticket through `gitBranch`, since the backlog makes the ticket id
    the branch name;
  - the last activity time (file mtime).
- **Bad for live state**:
  - The transcript lags, by its own documentation.
  - It has no "waiting for permission" marker.
  - Reading the tail of a multi-MB file per session per tick costs far more than the registry.
- **Cost at scale**: listing with `stat` is cheap. Reading `cwd`, `gitBranch` and title needs
  only the first few lines and the last few lines of each file, not the whole file.
  Thousands of transcripts make reading them all at start-up costly, which matches the
  map's "startup cost" open point.
- **Stability**: the path layout and retention are documented. The JSONL schema is internal.

## 4. Hooks

From [Hooks reference](https://code.claude.com/docs/en/hooks) [doc]:

- **Input every hook gets**: `session_id`, `transcript_path`, `cwd`, `permission_mode`,
  `hook_event_name` and others. Hook types include `command`, `http` and `mcp_tool`.
- **Where hooks can be configured**:
  - `~/.claude/settings.json`
  - `.claude/settings.json` and `.claude/settings.local.json`
  - managed settings
  - a plugin's `hooks/hooks.json`
  - skill or subagent frontmatter

  Entries from all levels are merged. A command hook can set `"async": true`.
- **`Notification`** carries `notification_type`, `message` and `title`:
  - `permission_prompt` fires when a permission prompt "has waited about six seconds".
  - `idle_prompt` fires "about 60 seconds after Claude finishes responding, and only if you
    haven't typed since and no background agent … is still running".
  - Other types are `elicitation_dialog`, `agent_needs_input`, `agent_completed`,
    `auth_success` and others.
- **`Stop`** fires when "Claude finishes responding". It carries `last_assistant_message`,
  `background_tasks` and `session_crons`.
- **`SessionStart`** carries `source` (`startup | resume | clear | compact | fork`).
- **`SessionEnd`** carries `reason` (`clear | resume | logout | prompt_input_exit | other`).
  Its hooks share a 1.5 s budget.
- **`UserPromptSubmit`**, **`PermissionRequest`**, **`CwdChanged`**, `WorktreeCreate` and
  others also exist.

Assessment:

- **Stability**: high. Hooks are the documented, versioned interface.
- **Blind spots**:
  - Hooks only see sessions started after they are installed, and installing them means
    editing the user's settings. lazyclaude could ship them as a plugin instead.
  - Events are transitions, not state. If lazyclaude is not running, or misses an event, it
    has no state to recover from.
  - `SessionEnd` does not fire on a crash or `kill -9`.
  - The notification delays mean "waiting" is reported 6 to 60 s late.
  - Nothing marks "permission answered" except a later `PostToolUse` or `Stop`.
- **What hooks add over the registry**: lower latency than polling transcripts, and events
  that work without reading internal files. The registry already has the state and is
  rewritten on every change, so hooks add little for v1. The map already puts "notifications
  while lazyclaude is closed" out of scope as a hook concern.

## Comparison

| Source | Documented? | Live state | Cost | Misses |
| - | - | - | - | - |
| `~/.claude/sessions/*.json` | Existence and purpose only; format internal | Yes: `busy/shell/idle/waiting` plus `waitingFor` | Microseconds; one small file per session; can be watched | Crash leftovers (check pid, `procStart`, `pidDomain`); other machines and containers |
| `claude agents --json` | Yes (research preview) | Yes: `busy/waiting/idle` plus `waitingFor`, and background `state` | One CLI process start per poll | Same as the registry. Might start the supervisor (unverified). |
| Transcripts `projects/*/*.jsonl` | Path and retention yes; format no | No (lags; no waiting marker) | `stat` cheap; content costly at thousands of files | Directory is the launch or last `/cd` directory, not the project; deleted after 30 days |
| Hooks | Yes, stable | Transitions only, 6 to 60 s late for waits | Needs the user's settings or a plugin; event plumbing | Sessions started before install; crashes; anything while lazyclaude is down |

## Recommendation

1. **Live sessions and their state.** Read `~/.claude/sessions/*.json` behind a port.
   - Accept a record only when all of these hold:
     - its `pidDomain` matches this machine;
     - `/proc/<pid>` exists;
     - `/proc/<pid>/stat` field 22 equals `procStart`.
   - Map the record to the domain states:

     | Record | Domain state |
     | - | - |
     | `busy` or `shell` | busy |
     | `waiting` + `waitingFor = permission prompt` | waiting for permission |
     | `waiting` with any other reason | waiting for input |
     | `idle` | idle |

   - Watch the directory instead of polling it.
   - Treat unknown fields and values as absent, as Claude Code's own reader does.
2. **The documented contract.** Write a second adapter over `claude agents --json`, with
   the same port and the same mapping. Use it as a fallback when the file format drifts, and
   as a contract test: both adapters should agree on a live machine.
   - Before relying on it, confirm in a spike whether it starts the supervisor daemon, and
     measure how long it takes to start.
3. **Past sessions.** Read the transcripts by `stat`ing every
   `~/.claude/projects/*/*.jsonl`, not only the current project's slug.
   - From each file read only the first lines (`cwd`, `gitBranch`, `sessionId`) and the last
     lines (`ai-title`, last timestamp).
   - Link a session to a ticket by `gitBranch == ticket id` or by `cwd` under the project
     root.
   - Find a live session's transcript by its `sessionId`, with a glob over
     `*/<sessionId>.jsonl`.
4. **No hooks in v1.** Revisit them only if registry latency or drift becomes a problem.
   They could then ship as a lazyclaude plugin, with `async` command hooks writing to a
   lazyclaude-owned state file.

## Open questions

- Does `claude agents --json` start the supervisor when none is running, and how long does
  it take to start?
- Do bare-mode (`--bare`) and `-p` sessions write a registry file? (The docs say `-p` binds
  an inbox socket and bare mode does not.)
- How long does `status` take to flip to `waiting` when a permission prompt opens? The binary
  suggests immediately, unlike the ~6 s `Notification` delay. A live test should confirm it.
