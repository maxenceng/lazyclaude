---
id: LAZY-42
status: in-review
context: cross-context
type: feature
tracker_id: 104
created: 2026-10-27
blocked_by: [LAZY-31]
---

# The claude-tools harness works on GitHub Issues

## Acceptance criteria

- [ ] `ddd-workflow` ships `scripts/ticket-tracker`, Python standard library over `gh`, and every `/ticket` step,
      `/wayfinder` and `/debt` reaches GitHub only through it; no step file carries a `gh issue` or `gh api` snippet.
- [ ] It reads a ticket by ADR 0052's rules: no status label is `draft`; from `todo` on, a ticket without exactly one
      `context:` and one `type:` label, with two status labels, or closed as not planned, is unreadable, and every verb
      refuses it with the reason.
- [ ] A status move is one `gh issue edit` adding the new label and removing the old.
- [ ] A section write replaces only that `##` section and refuses, changing nothing, when the issue's `updatedAt` moved
      since it was read.
- [ ] A missing `status:`/`type:` label is created as it is applied; a `context:` label not named in
      `docs/context-map.md` is refused, except `context:cross-context`, which every project accepts (ADR 0053).
- [ ] `new` captures one issue per line from `.github/ISSUE_TEMPLATE/ticket.md`; the template also ships `map.md` and
      `decision.md`. `refine`, `start` and `design` keep today's refusals, read from labels and sections; `refine` no
      longer calls context the last cheap moment.
- [ ] `start` branches with `gh issue develop`; `review` writes `Closes #<n>` in the PR body; `done`, after the merge,
      writes *Notes* to the issue, removes leftover labels, closes an issue left open by a missed keyword as completed
      and says so, and opens a PR only when it changed files, closing nothing.
- [ ] `/wayfinder`'s frontier is the map's open `status:todo` sub-issues whose dependencies are all closed; claiming
      adds `status:in-progress`; a decision ticket closes as completed, a ruled-out one too; graduated feature tickets
      are not sub-issues; a map's file changes go on one PR that `Refs` it, and the map closes by hand.
- [ ] Without the network every verb refuses with that reason and writes nothing; `/debt` still sweeps code markers and
      ADRs and reports its ticket source skipped. `make verify` in the template needs no network.
- [ ] The template's `check-ticket-references.py` fails on any `#n` or issue URL outside `docs/adr/`, `docs/research/`,
      `docs/endpoints/` and `scripts/`, open or closed, without calling GitHub; a hex colour in a stylesheet or a
      frontend source does not trip it; a fixture trips it (template ADR 0004).
- [ ] Commit subjects carry no ticket id; `/trace-endpoint`'s `tickets:` takes issue numbers.
- [ ] memory-sync indexes issues, open and closed, through `gh` instead of `docs/backlog/*.md`.
- [ ] Every Vikunja section, `tracker_id`, the Obsidian board and `docs/backlog/` go from the plugin, HOWTO, README and
      template; the plugin is 3.0.0.
- [ ] The script's tests run under `unittest` against a fake `gh` on `PATH` and show each refusal above failing on its
      case.

## Model decision

No lazyclaude code: the change is in `~/projects/claude-tools`, decided in LAZY-31 (ADR 0053). Two template ADRs land
with it — the harness on Issues through one script, and the superseding of template ADR 0016.

## Design direction

n/a

## Glossary impact

None in lazyclaude; the glossary's `Ticket branch`, `Branch copy`, `Root copy` and `Status` change with the board's
Issues read (ADR 0052), not here.

## Notes

Lands before any project upgrades; a project takes 3.0.0 only once LAZY-33 has migrated it, lazyclaude only once its
board reads Issues.
