---
id: LAZY-33
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-30, LAZY-31]
---

# How is every project's `docs/backlog/` migrated to Issues?

## Question

How is every project's `docs/backlog/` migrated to Issues?

Which projects, a repeatable script or a one-off, how ids and the links between tickets, ADRs and docs survive, how a lost or duplicated ticket is caught, maps and decisions included, and where the archival copy stays. The harness cuts over in one major version (LAZY-31), so a project is migrated before
it upgrades, and lazyclaude's board reads Issues before lazyclaude itself does.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0055](../adr/0055-a-backlog-moves-to-issues-once-from-main-by-a-resumable-script-and-a-tag-keeps-the-files.md).

- **Projects**: lazyclaude and next-suggestions; the template's `docs/backlog/` goes with LAZY-42.
- **What moves**: every ticket — done ones closed as completed, maps, decisions and drafts too.
- **Mechanics**: one standalone stdlib Python script in `claude-tools/scripts/`, run from a 2.x project, `--dry-run`,
  resumable by skipping old ids already migrated, deleted in the PR migrating the last project.
- **Old id and date**: a closing body line, `Migrated from LAZY-33, created 2026-10-26.`, the resume and verify key; the
  `# H1` is the title, `tracker_id` dropped.
- **`cross-context`**: accepted as a `context:` label in every project, an exception in `ticket-tracker` (ADR 0053,
  LAZY-42).
- **When**: from `main` only, no ticket branch open; the dry run refuses `in-progress`/`in-review` except maps. Order:
  3.0.0, next-suggestions, then lazyclaude after its board reads Issues.
- **Links**: two passes — create all, then rewrite links and bare ids of migrated tickets to `#n` outside code spans and
  repository links to blob URLs on `main`. In the record only links into `docs/backlog/` change.
- **Maps**: a decision is a sub-issue of the map it reaches by `blocked_by`; edges onto a map are dropped, the rest are
  native dependencies; a feature loses its map edge; two maps or none is refused.
- **Verification**: `verify` fails on an old id with no issue or several, a state or label mismatch, a missing or extra
  dependency or sub-issue, a marked issue with no file; the deleting PR quotes it.
- **Archive**: `docs/backlog/` deleted, README, template and `ROADMAP.md` included; a `backlog-archive` tag keeps it.
- **Glossary**: no new term.

Graduated: [LAZY-43](LAZY-43-migrating-a-backlog-to-issues.md), [LAZY-44](LAZY-44-lazyclaude-backlog-on-issues.md).
