---
id: LAZY-35
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-34]
---

# How are a project's docs laid out and built on MkDocs?

## Question

How are a project's docs laid out and built on MkDocs?

The tool and theme, the nav (ADRs, glossary, context map, research, endpoint maps from `/trace-endpoint`), where generated output lives and how it is refreshed without touching hand-written pages, the `make` targets and CI checks, publishing, what the claude-tools template ships, and what lazyclaude's `docs` context reads (sources, nav order, a missing or stale generated page).

## Kind

grilling

## Resolution

Settled by grilling on 2026-10-27, on Zensical as LAZY-34 chose it.

- **One tree.** `docs_dir: docs`: ADRs, glossary, context map, research and endpoint maps are
  all site pages; `docs/backlog/` is kept out by `exclude_docs` until phase 3 deletes it. No
  `nav:` in `mkdocs.yml` — the filesystem is the one source for the site and the Docs panel.
- **The panel orders as the site does.** At each level the folder's `README.md` first, then the
  other files by name, then the subfolders by name, segment by segment as now; `.html` stays
  unlisted. A superseding ADR for 0018 lands with that change.
- **Index pages are `README.md`** — `docs/README.md` hand-written as the landing page,
  `docs/adr/README.md`, `docs/endpoints/README.md` — so GitHub shows them too. If Zensical does
  not take `README.md` as a section index, fall back to `index.md` and move `/trace-endpoint`
  with it.
- **The ADR index is generated and checked**: one line per ADR in number order, linked title
  from the H1 and its status line. `make adr-index` writes it; `adr-check` fails on a stale
  index (a fixture proves it) and exempts it from the number rule. No check ever edits a file.
- **Generated pages** stay as `/trace-endpoint` writes them: committed, in `docs/endpoints/`,
  regenerated on demand, `.html` copied as-is. No staleness check — it would need Claude and
  archify; `/ticket done` already prompts a re-trace. lazyclaude shows them as ordinary
  documents and reads sources, never the built site.
- **Build**: `uvx zensical==<pin>` from one Makefile variable, `make doctor` checks `uv`;
  `make docs` builds strict into a gitignored `site/`, `make docs-serve` serves; the strict
  build runs in `make verify` after `make ci`, so a broken link or anchor fails CI.
- **Publishing**: a GitHub Pages workflow, deploying from `main` only when the repo variable
  `DOCS_PAGES` is `true`; lazyclaude turns it on, a private project need not.
- **The template**: claude-tools' spring-ddd template ships the build and the ADR index;
  existing projects adopt by hand, one ticket each — no command for two projects.

Leads to [DOCS-2](DOCS-2-docs-build-on-zensical.md), [DOCS-3](DOCS-3-the-adr-index-is-generated-and-checked.md)
and [DOCS-4](DOCS-4-the-panel-orders-as-the-site-does.md), independent of each other; the
template's share is a claude-tools ticket.
