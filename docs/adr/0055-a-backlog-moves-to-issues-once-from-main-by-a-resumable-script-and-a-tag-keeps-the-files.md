# 55. A backlog moves to Issues once, from main, by a resumable script, and a tag keeps the files

Date: 2026-10-27

## Status

Accepted. Amended by ADR 0064, which also takes a decision's map from the map body that links it.

## Context

ADR 0052 moves a ticket onto a GitHub issue and ADR 0053 cuts the harness over in one major version, with no dual mode:
a project upgrades only once its `docs/backlog/` is on Issues. Two projects have a backlog — lazyclaude (77 files) and
next-suggestions (89) — most of them `done`, with maps, decisions and drafts among them. Issue numbers cannot be chosen
and `createdAt` cannot be backdated (LAZY-29). Ticket files differ between `main` and the open ticket branches and
worktrees. Under ADR 0053's wayfinder, a decision is on the frontier only when every dependency is closed, while today a
decision belongs to its map by `blocked_by` the open map.

## Decision

- **Every ticket moves, done ones included.** A `done` ticket becomes an issue closed as completed; maps, decisions and
  drafts move too. Rejected: moving only open tickets, which drops done tickets' *Notes* from `/debt`, memory-sync and
  the board's `done` group.
- **One standalone, standard-library Python script in claude-tools, run once per project, then deleted.** It runs from a
  2.x project, has a `--dry-run` listing what it would create, and resumes: an old id that already has an issue is
  skipped. Rejected: a `migrate` verb on 3.0.0's `ticket-tracker`, which would run against a project not yet on 3.0.0;
  a one-off script per project.
- **The old id and date survive as one closing body line**, `Migrated from LAZY-33, created 2026-10-26.` — the key the
  script resumes and verifies by, and what a GitHub search for an old id finds. The `# H1` becomes the title;
  `tracker_id` is dropped. Rejected: a `migrated` label; the old id in the title.
- **It reads `main` only, once no ticket branch is open.** The dry run refuses an `in-progress` or `in-review` ticket
  other than a map, since `gh issue develop` never linked its branch. Rejected: merging branch copies by ADR 0028.
- **Order**: the 3.0.0 harness first, then next-suggestions migrates and upgrades, then lazyclaude builds its Issues
  board on 2.x, migrates and upgrades.
- **Two passes.** All issues are created first; then bodies are rewritten — links and bare ids of migrated tickets become
  `#n` outside code spans, links into the repository become blob URLs on `main` — dependencies and sub-issues are wired,
  and done issues closed.
- **A decision becomes a sub-issue of the map it reaches through `blocked_by`, and edges onto a map are dropped.** Every
  other edge is a native dependency; a feature ticket depending on a map loses that edge and is not a sub-issue. A
  decision reaching two maps or none is refused by the dry run.
- **A `verify` pass catches a lost or duplicated ticket.** It fails, naming the ticket, on an old id with no issue or
  several, an issue whose state or `status:`/`type:`/`context:` label disagrees with its file, a dependency or sub-issue
  missing or extra, or a marked issue with no file. The pull request deleting `docs/backlog/` quotes its output.
- **`docs/backlog/` is deleted, and a `backlog-archive` tag on the last commit holding it is the archive** — tickets,
  `README.md`, `_template.md` and v1's `ROADMAP.md` alike. Rejected: a frozen copy under `docs/archive/`, which agents
  grep as current and MkDocs publishes.
- **In the record**, only links into `docs/backlog/` are rewritten to issue URLs; bare old ids in ADRs and research stay
  as written. Rejected: rewriting every old id in the record.

## Consequences

- Issue numbers follow the repository's pull requests (lazyclaude's start after 47), not the old ids or their order.
- A ticket that is wrong in its file is wrong on its issue; the migration copies, it does not tidy.
- An old id read in an ADR is found through GitHub search, so it needs the network, as every ticket read does
  (ADR 0053).
- The `LAZY-24` map is the first map on Issues; its open decisions continue there.
- Migrating a project means pausing its work: no ticket branch open until the archive tag is cut.
