# 53. The harness works tickets on Issues through one script, and no issue is named outside the record

Date: 2026-10-27

## Status

Accepted. Supersedes ADR 0005 once the backlog moves to GitHub Issues (ADR 0052); until then it stands.

## Context

The claude-tools harness — `/ticket`, `/wayfinder`, `/debt`, the template's guards — reads and writes `docs/backlog/`
today, and mirrors status to Vikunja when it is configured. ADR 0052 moves a ticket onto a GitHub issue: the number is
its id, labels carry status, context and type, and the merge's closing keyword makes it `done`. GitHub offers no
conditional write, only an `updatedAt` to compare before writing (LAZY-29).

ADR 0005's check fails on a done ticket's id named outside the record. On Issues the merge itself closes the ticket, so a
pull request that names `#42` in `src/` is green when it merges and fails the next build of `main`, and the check would
need the network and a token to know which issues are closed.

## Decision

The harness side is decided in claude-tools, with its own ADRs in the template; lazyclaude inherits it as follows.

- **One script carries every GitHub operation.** The plugin ships a Python, standard-library `ticket-tracker` wrapping
  `gh`, and every step calls it: reading a ticket under ADR 0052's readability rules, moving a status label in one edit,
  writing one `##` section, creating a missing label (a `context:` label only if `docs/context-map.md` names it, or `context:cross-context`, the harness's own),
  the wayfinder frontier. Rejected: `gh` snippets in each step, which drift across nine files; an MCP server.
- **A body write replaces only its own section, and refuses if the issue changed since it was read.** It compares
  `updatedAt`; a human's edit from the GitHub UI is never overwritten. Rejected: step output as comments, which splits
  the ticket's record.
- **No ticket verb works offline, and nothing is cached.** A verb that cannot reach GitHub refuses with that reason. The
  build never needs the network. Rejected: a read-only cache, a second copy of status that ADR 0052 just removed.
- **No issue is named outside the record.** `make ticket-check` fails on any `#n` or issue URL outside the record —
  `docs/adr/`, `docs/research/`, `docs/endpoints/`, `scripts/` — open or closed, with no lookup; a hex colour is told
  apart by file type.
  Rejected: keeping "done only" through `gh` in CI, which needs the network and turns `main` red after a green merge; and
  dropping the check.
- **A commit subject says the change in words.** The pull request body carries `Closes #42`; the squash adds the pull
  request's number. Rejected: an id prefix, which repeats the link GitHub keeps.
- **`/ticket done` runs after the merge**, writes *Notes* into the issue, clears leftover labels, closes the issue as
  completed if the closing keyword was missed, and opens a pull request only for the files it changed.
- **One cut-over.** The harness speaks Issues only from a major version; lazyclaude takes it once its own backlog is
  migrated and its board reads Issues. Rejected: detecting `docs/backlog/` per project and keeping both.

## Consequences

- A pointer to an open ticket from code — "the type #42 will share" — is no longer allowed; a deliberate limit says what
  closes it in a `deferred:` comment instead.
- Two sessions claiming the same frontier ticket in the same second both win; accepted for one developer.
- Without the network, `/ticket` and `/wayfinder` do nothing and `/debt` reports its ticket source as skipped.
- Vikunja, `tracker_id` and the Obsidian board go from the harness and from `docs/backlog/README.md`.
