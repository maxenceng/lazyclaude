---
id: LAZY-34
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# What is the state of MkDocs and its ecosystem in 2026?

## Question

What is the state of MkDocs and its ecosystem in 2026?

- MkDocs core and Material for MkDocs: maintenance status, and the successor work announced by Material's maintainers — what a new site should start on;
- plugins or patterns for ADR indexes, mermaid, generated pages kept apart from hand-written ones, link checking and strict builds;
- publishing to GitHub Pages from Actions;
- whether `mkdocs.yml`'s nav is a reasonable source for lazyclaude's docs tree order (ADR 0018).

## Kind

research

## Resolution

Start a new site on Zensical, not MkDocs. MkDocs 1.x is stalled (1.6.1, 2024-08-30; outside
contributions closed 2026-10-02), MkDocs 2.0 is a plugin-less rewrite still in `2.0.dev`, and
Material is in maintenance mode, end of life 2027-05-05. Its maintainers' successor, Zensical
(MIT, Rust core), reaches 0.1.0 on 2026-11-05. Keep `mkdocs.yml` to what both tools read, and
pin the Zensical version, because it silently ignores unknown plugins and has no `gen-files`.
Mermaid, strict link checks and Pages deploys are all built in. ADRs need an index page, since
no ADR plugin is maintained. Generated pages are committed to their own folder, and Archify
HTML is copied as-is. `mkdocs.yml`'s nav is a poor source for the docs tree order (ADR 0018);
the default file-name nav is close but puts `index` first and files before folders. Details
and the unverified points: [`docs/research/mkdocs-ecosystem.md`](../research/mkdocs-ecosystem.md).
