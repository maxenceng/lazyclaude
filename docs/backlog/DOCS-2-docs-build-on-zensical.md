---
id: DOCS-2
status: todo
context: docs
type: feature
tracker_id: 109
created: 2026-10-27
---

# lazyclaude's docs build into a site on Zensical

## Acceptance criteria

- [ ] `mkdocs.yml` at the root: `docs_dir: docs`, `docs/backlog/` excluded by `exclude_docs`, no `nav:`, a
      `mermaid` fence through `pymdownx.superfences`, `strict: true` with link and anchor validation, only settings
      and plugins both Zensical and MkDocs 1.x + Material read.
- [ ] `make docs` builds with `uvx zensical==<pin>`, the pin in one Makefile variable, into a gitignored `site/`;
      `make docs-serve` serves it; `make doctor` reports `uv`.
- [ ] The strict build runs in `make verify` after `make ci`, and in CI; a broken link to a page or an anchor fails it,
      watched to fail once before trusted.
- [ ] `docs/README.md` is the site's landing page, a few lines linking the glossary, the context map, the ADRs,
      research and endpoint maps; the build shows it as the home page. If Zensical does not take a `README.md` as a
      section index, `index.md` is used instead and the ticket's Notes say so, for `/trace-endpoint` to follow.
- [ ] The context map's mermaid diagram renders on the site.
- [ ] A GitHub Pages workflow deploys `site/` on a push to `main` only when the repo variable `DOCS_PAGES` is `true`;
      lazyclaude sets it.
- [ ] Every page builds today: links the strict build refuses are fixed, not set to `warn`.

## Model decision

No Java change — a build and a CI step. The Docs panel keeps reading `docs/**/*.md` from disk, never `site/`.

## Glossary impact

*Site* gains a row under `docs`.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-35](LAZY-35-docs-on-mkdocs.md), on the tool [LAZY-34](LAZY-34-mkdocs-ecosystem.md) chose. A decision someone
could reverse without knowing why — the pin, the `mkdocs.yml` both tools read, the gated deploy — gets its ADR here.
