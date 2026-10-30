# 69. The docs build on a pinned Zensical from a mkdocs.yml both tools read, and deploy only when enabled

Date: 2026-10-30

## Status

Accepted.

## Context

`docs/` holds the glossary, the context map, the ADRs, research and, later, endpoint maps. Read on GitHub or in the Docs
panel they are fine, but they are not a site, and nothing checks that one page's link still reaches another. Research
(`docs/research/mkdocs-ecosystem.md`) found MkDocs 1.x stalled and Material for MkDocs in maintenance mode until
2027-05-05, with Zensical, from Material's maintainers, as the successor. Zensical is 0.x, changes every few days, and
silently ignores a plugin it does not know.

## Decision

- **The site is built by Zensical, pinned in one Makefile variable.** `make docs` runs `uvx zensical==$(ZENSICAL_VERSION)`,
  currently 0.0.73, the newest release on PyPI when this was written. A bump is an edit to that variable, made on purpose
  and checked by the strict build. Rejected: an unpinned `uvx zensical`, which changes behaviour on days nobody touched
  the pipeline; MkDocs with Material, which is end-of-life in 2027 and would be a migration owed later.
- **`mkdocs.yml` holds only what both Zensical and MkDocs 1.x with Material read.** Zensical reads `mkdocs.yml` as it is,
  so the file also builds with the older pair as a fallback while Zensical is 0.x. That means settings only (`docs_dir`,
  `exclude_docs`, `strict`, `validation`, the `mermaid` fence through `pymdownx.superfences`) and no plugin, because an
  unlisted plugin is dropped without an error. There is no `nav:`: the filesystem is the one source, for the site and for
  the Docs panel. Rejected: `zensical.toml`, which MkDocs cannot read.
- **The build is strict.** `strict: true` turns a link to a missing page, or to a missing anchor, into a failed build.
  `make verify` runs `make docs` after `make ci`, and CI runs the same step. Links to a directory are not checked, so the
  landing page links pages.
- **`docs/README.md` is the home page.** Zensical takes a `README.md` as a section index, so GitHub shows the same file.
- **Deploying to GitHub Pages is a separate workflow that runs on a push to `main` only when the repository variable
  `DOCS_PAGES` is `true`.** Without it the site is built and checked on every pull request and published nowhere. This
  keeps a repository that has not enabled Pages from failing on every merge. Rejected: deploying unconditionally.
- **`site/` is build output: gitignored, never read by lazyclaude.** The Docs panel keeps reading `docs/**/*.md`.

## Consequences

- `uv` is a build prerequisite, reported by `make doctor`. Python is already needed for the `scripts/check-*` guards.
- External links are not checked.
- Zensical's caches are not cached in CI; its docs advise against it while the cache format changes.
- A plugin added to `mkdocs.yml` later must be checked against Zensical's supported list by hand.
