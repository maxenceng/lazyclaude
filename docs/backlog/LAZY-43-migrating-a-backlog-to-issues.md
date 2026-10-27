---
id: LAZY-43
status: todo
context: cross-context
type: chore
tracker_id: 107
created: 2026-10-27
blocked_by: [LAZY-42]
---

# A script moves a project's backlog to GitHub Issues

## Acceptance criteria

- [ ] `claude-tools/scripts/` ships a standard-library Python script that moves a 2.x project's `docs/backlog/` from
      `main` onto its GitHub remote's issues, every ticket included, done ones closed as completed (ADR 0055).
- [ ] `--dry-run` lists every issue it would create, its labels, dependencies and sub-issue parent, and writes nothing.
- [ ] It refuses, before writing, an `in-progress` or `in-review` ticket that is not a map, a decision reaching two maps
      or none, and an unreadable file, each with its file and reason.
- [ ] Each issue's title is the `# H1`; its body is the rest, ending in `Migrated from <old id>, created <date>.`;
      `status:`/`type:`/`context:` labels follow the frontmatter, no status label for a `draft`, `tracker_id` dropped.
- [ ] A second pass rewrites links and bare ids of migrated tickets to `#n` outside code spans and repository links to
      blob URLs on `main`, wires every `blocked_by` as a native dependency except edges onto a map, makes each decision
      a sub-issue of its map, and closes done issues.
- [ ] A rerun after a failure partway creates no duplicate: an old id already carried by an issue is skipped.
- [ ] `verify` fails, naming the ticket, on an old id with no issue or several, a state or label disagreeing with the
      file, a dependency or sub-issue missing or extra, and a marked issue with no file; a fixture trips each.
- [ ] next-suggestions is migrated with it: `verify` passes, its links into `docs/backlog/` in the record point at
      issues, `docs/backlog/` is deleted in a PR quoting `verify`'s output, `backlog-archive` tags the last commit
      holding it, and the project is on plugin 3.0.0.

## Model decision

No lazyclaude code: the script lives in `~/projects/claude-tools`, decided in LAZY-33 (ADR 0055).

## Design direction

n/a

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-33](LAZY-33-migrating-the-backlog.md). The script is deleted in the PR that migrates the last project
([LAZY-44](LAZY-44-lazyclaude-backlog-on-issues.md)).
