---
id: LAZY-44
status: in-progress
context: cross-context
type: chore
tracker_id: 108
created: 2026-10-27
blocked_by: [LAZY-43, BOARD-8]
---

# lazyclaude's backlog moves to GitHub Issues

## Acceptance criteria

- [ ] With no ticket branch open, the migration script's dry run on lazyclaude's `main` refuses nothing, and its run
      leaves `verify` passing (ADR 0055).
- [ ] The `LAZY-24` map, `done` on `main` since its decisions were resolved, is a closed issue whose decisions are its
      sub-issues, and `/wayfinder`'s frontier on it lists the same tickets the markdown did: none.
- [ ] The two links into `docs/backlog/` in `docs/adr/` and `docs/research/` point at their issues; bare old ids stay.
- [ ] `docs/backlog/` is deleted in a PR quoting `verify`'s output, and `backlog-archive` tags the last commit holding it.
- [ ] Before `docs/backlog/` is deleted, the unbuilt steps of `ROADMAP-v1.1.md` are `docs/roadmap/v1.1.md`, in the
      same order, each step linking its issue; `docs/roadmap/` is part of the record (ADR 0068).
- [ ] lazyclaude is on plugin 3.0.0, its board lists the migrated tickets, and the migration script is deleted from
      claude-tools.

## Model decision

No code: the board already reads Issues once BOARD-8 lands; this runs the script and removes the files.

## Design direction

n/a

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-33](LAZY-33-migrating-the-backlog.md). The map's first test case on Issues, as its *Notes* foresaw.
