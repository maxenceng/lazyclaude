---
id: LAZY-31
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-30]
---

# How do `/ticket`, `/wayfinder` and `/debt` work on GitHub Issues?

## Question

How do `/ticket`, `/wayfinder` and `/debt` work on GitHub Issues?

In `~/projects/claude-tools`: each `/ticket` verb's reads and writes, the refusals it enforces today from frontmatter, `/wayfinder`'s frontier query and claiming, `/debt`'s collection of ticket notes, the `ticket-check`-style guards that grep `docs/backlog/`, the project template, and deleting the Vikunja push. What still has to work offline.

## Kind

grilling

## Resolution

Grilled 2026-10-27; lazyclaude's side in [ADR 0053](../adr/0053-the-harness-works-tickets-on-issues-through-one-script-and-no-issue-is-named-outside-the-record.md),
the harness's in two template ADRs that land with [LAZY-42](LAZY-42-harness-on-github-issues.md).

- **Mechanics**: one Python standard-library script in the plugin, `ticket-tracker`, wrapping `gh`; every step of
  `/ticket`, `/wayfinder` and `/debt` calls it. Tested with `unittest` against a fake `gh` on `PATH`.
- **Writes**: a status move is one label swap; a body write splices only the step's `##` section and refuses if
  `updatedAt` moved since the read; a missing label is created as it is applied, a `context:` label only if
  `docs/context-map.md` names it.
- **Refusals**: today's, read from labels and sections; every verb refuses an unreadable ticket (ADR 0052). Context is a
  label changeable any time — `refine`'s "last cheap moment" goes.
- **Verbs**: captures from `.github/ISSUE_TEMPLATE/` (ticket, map, decision), which `new` reads too; `start` branches
  with `gh issue develop`; `review` always writes `Closes #n`; `done` runs after merge, writes *Notes* to the issue,
  clears leftover labels, closes the issue itself if the keyword was missed, and opens a PR only for files it changed
  (atlas, ADR), closing nothing.
- **Wayfinder**: the frontier is a map's open `status:todo` sub-issues whose dependencies are all closed; claiming is the
  label, the race accepted. A graduated feature ticket is not linked to its map, only `blocked_by` where true. A map
  keeps one PR for its files (`Refs #n`) and is closed by hand when its destination is reached.
- **Offline**: no verb works offline, nothing is cached; `make verify` never needs the network; `/debt` skips its ticket
  source offline and reads every issue's *Notes* online.
- **Guards and naming**: `ticket-check` bans any `#n` or issue URL outside the record, open or closed, no lookup
  (supersedes ADR 0005 and template ADR 0016); commit subjects in words, no id prefix; an endpoint's `tickets:` holds
  issue numbers.
- **Rollout**: one cut-over, plugin 3.0.0, no dual mode; a project upgrades once LAZY-33 has migrated it, lazyclaude
  only once its board reads Issues. memory-sync indexes issues through `gh`. Vikunja and the Obsidian board go.

Graduated: [LAZY-42](LAZY-42-harness-on-github-issues.md).
