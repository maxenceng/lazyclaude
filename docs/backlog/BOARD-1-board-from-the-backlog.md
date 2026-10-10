---
id: BOARD-1
status: done
context: board
type: feature
tracker_id: 72
created: 2026-10-10
---

# lazyclaude starts in a project root and shows the board: the tickets in `docs/backlog/` listed by status, in ADR 0008's one-screen frame

## Acceptance criteria

- [x] Started in a project root, a subdirectory of it, or one of its ticket worktrees, lazyclaude shows the main worktree's `docs/backlog/`.
- [x] Started outside a git repository, or where the main worktree has no `docs/backlog/`, it prints why and exits non-zero before the first frame.
- [x] The Board panel groups tickets by status in lifecycle order — draft, todo, in-progress, in-review, done — each header with its count (`▾ draft (5)`); an empty group still shows, at `(0)`.
- [x] Within a group, tickets are ordered by id numerically: BOARD-2 before BOARD-10.
- [x] `done` starts folded; `enter` on a group header folds or unfolds it.
- [x] Files whose name starts with `_`, and files with no frontmatter `id` (README.md, ROADMAP.md), never appear.
- [x] A file with an `id` but broken frontmatter or a status outside the five is listed last, under `unreadable (n)`, with its reason — and every other ticket still loads.
- [x] A ticket with no `# ` title line shows its id as its title; it is not unreadable.
- [x] The main view shows the selected ticket's id, title, status, context and type; on a group header it is empty.
- [x] Sessions, Docs and Elsewhere are titled and empty, the dock says "no hosted session", and the status line is empty.
- [x] `j`/`k` move the Board selection; `j` on the last row and `k` on the first stay put rather than wrap.
- [x] `h`/`l`, and `alt+h/j/k/l`, move focus between panels, main view and dock.
- [x] `q` quits, with no confirmation.
- [x] The key bar lists the keys for whatever has focus.

## Model decision

- **`shared.Project`** — the main worktree's root, a plain value. Finding it from the
  current directory runs `git`, so it sits behind a port `ProjectLocator` with a git adapter
  in `board.infrastructure.secondary`. `board` is the first context to need it; the port
  moves when `session` needs it too.
- **`board` domain** (new context, as the context map planned): `Ticket` (id, title,
  status, context, type), `TicketId` ordering numerically, `Status` as the five statuses in
  lifecycle order and refusing any other value, and `Board` — tickets grouped by status,
  plus `UnreadableTicket` (file and reason). Port `Backlog` reads a project's tickets.
- **`board` infrastructure** — `MarkdownBacklog` reads `docs/backlog/*.md`, parsing
  frontmatter with SnakeYAML Engine (LAZY-20's pick). It skips `_`-prefixed files and files
  with no `id`; a file with an `id` whose frontmatter or status is refused becomes an
  `UnreadableTicket`.
- **`board` application** — one use case, `ReadBoard(Project) → Board`. No verbs, no
  branch copy: those arrive with BOARD-3 and the session tickets.
- **`shell`** — the ADR 0008 frame; selection (by ticket id) and folded groups are shell
  state. `LazyClaude.main` locates the project, wires `ReadBoard`, and exits with the
  reason before starting the shell when there is none.

## Design direction

```
┌ status line ─────────────────────────────────────────────┐
┌ Board ──────────────┐┌ main view ────────────────────────┐
│▾ draft (5)          ││ BOARD-1                           │
│  BOARD-1 lazyclau…  ││ lazyclaude starts in a project …  │
│▾ todo (0)           ││ status draft · board · feature    │
│▾ in-progress (0)    ││                                   │
│▾ in-review (0)      │└───────────────────────────────────┘
│▸ done (4)           │┌ dock ─────────────────────────────┐
│▾ unreadable (1)     ││ no hosted session                 │
├ Sessions ───────────┤│                                   │
├ Docs ───────────────┤│                                   │
├ Elsewhere ──────────┤│                                   │
└─────────────────────┘└───────────────────────────────────┘
 j/k move  h/l panel  enter fold  q quit
```

- Groups in lifecycle order, `done` folded at start, `unreadable` last and only when non-empty.
- Main view: id, title, status, context, type of the selected ticket; empty on a header.
- Keys: `j`/`k`, `h`/`l`, `alt+h/j/k/l`, `enter` (fold on a header), `q`. `?`, `1`–`4`,
  `n`, `z`, `m`, `w` and `R` land with the tickets that give them something to do.

## Glossary impact

- *Project*, *Ticket* and *Status* drop their *(expected)* marker.
- Adds *Board*: a project's tickets grouped by status, as the Board panel shows them.
- Adds *Unreadable ticket*: a backlog file with an `id` whose frontmatter or status is
  refused; listed with its reason, never dropped silently.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-10](LAZY-10-screen-layout-and-keys.md) and ADR 0008. The first runnable slice: it creates `board` and `shell`.

Review reshaped *Model decision*'s names and placement, not its model; read it through the
ADRs written under review. `ReadBoard` became `BoardApplicationService` → `BoardManager`, the
`Backlog` port `BacklogPort` with `BacklogRepository`, and the project locator
`ProjectLocatorPort` with `ProjectLocatorRepository` (ADR 0011). `TicketId` and `Status` moved
to `board.published`, the shell asks through `BoardQuery`, and a ticket with no title is
`NoTitle` in the domain, shown as its id only by the shell (ADR 0012). Tests build from
fixtures and mock ports (ADR 0013).

Found while building: a second file claiming an `id` already taken is unreadable too, not
silently dropped or merged.

It took three review rounds, each pulling the code toward the conventions of the author's
other DDD project; the rules from those rounds are now in `ArchitectureTest`, so the next
context starts from them rather than rediscovering them.
