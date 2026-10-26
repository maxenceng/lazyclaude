# MkDocs and its ecosystem, as of late 2026

Researched 2026-10-26 against PyPI metadata, the GitHub repositories and release notes of MkDocs,
Material for MkDocs, Zensical and ProperDocs, the Zensical documentation source
(`zensical/docs`), and the MkDocs 1.x source. Nothing was installed or built.

Each claim is tagged: **[pypi]** read from the PyPI JSON API on the date above, **[gh]** read from
the repository or its releases through the GitHub API, **[doc]** a quote or close paraphrase of
the project's own documentation, **[src]** read in the source, **[unverified]** inferred and not
tested.

## Question

What is the state of MkDocs and its ecosystem in 2026: MkDocs core and Material for MkDocs, the
successor announced by Material's maintainers, and what a new site should start on; plugins or
patterns for ADR indexes, mermaid, generated pages kept apart from hand-written ones, link
checking and strict builds; publishing to GitHub Pages from Actions; and whether `mkdocs.yml`'s
nav is a reasonable source for lazyclaude's docs tree order (ADR 0018).

## Answer in brief

- **MkDocs 1.x is over.** The last release is 1.6.1 (2024-08-30) [pypi]; `master` has had no
  commit since 2025-10-20 [gh], and the 1.x repository closed to outside contributions on
  2026-10-02 [doc: Material blog, "MkDocs 2.0"].
- **MkDocs 2.0 is a different tool.** Pre-releases `2.0.dev1`–`2.0.dev6` (2026-08-29 to
  2026-09-15) by Kim Christie [pypi]: no plugins, TOML config with no migration from
  `mkdocs.yml`, navigation handed to themes as pre-rendered HTML [doc: Material blog]. Material
  for MkDocs does not run on it and pins `mkdocs<2,>=1.6` [pypi].
- **Material for MkDocs is end-of-life.** In maintenance mode since 2025-11-05; 9.8.0
  (2026-10-24) announces end of life on **2027-05-05**, critical and security fixes only until
  then, PyPI classifier `Development Status :: 7 - Inactive` [gh, pypi].
- **Zensical is the successor** from the same team: MIT, Rust core with Python, reads an
  existing `mkdocs.yml`, ships a `classic` variant that looks like Material [doc, gh]. Still
  0.0.x (0.0.73 on 2026-10-26); **0.1.0 on 2026-11-05** starts "a dependable release line"
  [pypi, doc: zensical.org/upcoming-changes]. It does not run MkDocs plugins; it reimplements a
  list of them natively [doc].
- **ProperDocs** is a community rename-fork of MkDocs 1.6.1 (first release 1.6.5, 2026-03-15;
  latest 1.6.7, 2026-03-20) that keeps the plugin API [pypi, gh]. Material's maintainers
  explicitly do not recommend it [gh: 9.8.0 notes].
- **A new site should start on Zensical with a `mkdocs.yml`** that only uses settings and plugins
  both Zensical and MkDocs 1.x understand, so it still builds with `mkdocs` + Material during
  Zensical's 0.x period. Everything this project needs — mermaid, link and anchor validation,
  strict builds, an ADR folder, static HTML pages, a GitHub Pages workflow — is covered natively.
- **`mkdocs.yml`'s nav is not a good source for the docs panel's order.** Leave `nav` out: the
  default nav is already a name sort per directory, close to ADR 0018's order, so the
  filesystem stays the one source for both the site and the panel (see §6).

## 1. MkDocs core

- **1.x.** 1.6.1, uploaded 2024-08-30, is the last 1.x release [pypi]. The last commits on
  `mkdocs/mkdocs` `master` are dated 2025-10-20 (two small fixes); before that 2025-08-03
  [gh]. The Material team's article "MkDocs 2.0" (2026-02-18, updated since) calls 1.x
  unmaintained and dates the closing of the 1.x repository to outside contributions to
  2026-10-02 [doc: squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/].
- **2.0.** Six dev pre-releases on PyPI, `2.0.dev1` (2026-08-29) to `2.0.dev6` (2026-09-15),
  author Kim Christie; dependencies are `click`, `httpx>=1.0.dev6`, `jinja2`, `markdown`;
  Python ≥ 3.10 [pypi]. Its README describes a different model: a `docs/README.md`, GitHub
  Flavored Markdown, a `templates/base.html` for styling, `pip install mkdocs --pre` [pypi].
  Per the Material article: plugins are dropped, configuration moves to TOML with no migration
  from `mkdocs.yml`, navigation reaches themes as pre-rendered HTML (so tabs and collapsible
  sections are not possible), issues are maintainer-driven, and the licence is MIT after a
  period of being unlicensed [doc: Material blog]. No final 2.0 release date exists.
- **Consequence.** `pip install mkdocs` still resolves to 1.6.1 (pre-releases need `--pre`), but
  nothing will be fixed in it. A plain `mkdocs` dependency without an upper bound risks picking
  up 2.0 once it is final [unverified — depends on its release].

## 2. Material for MkDocs and Zensical

### Material for MkDocs

- Announcement of maintenance mode and of Zensical: 2025-11-05 ("Feature development has
  ended") [doc: squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/].
- 9.7.2 added a build warning about MkDocs 2.0; 9.7.5 (2026-03-10) limited `mkdocs` to `<2`;
  9.7.7 (2026-07-17) gave end of life as 2026-11-05 [gh releases].
- 2026-09-16: critical maintenance extended to **2027-05-05** [doc: Material blog, zensical.org
  upcoming-changes].
- 9.8.0 (2026-10-24) is "primarily a security release": Python ≥ 3.11, raised dependency minimums
  for a list of CVEs (Jinja2, Markdown, Pygments, PyMdown Extensions, Requests, Pillow,
  CairoSVG, GitPython), removed `mkdocs-rss-plugin` and `mkdocs-redirects` from the
  `recommended` extra because their recent releases depend on ProperDocs, and a CLI notice that
  "third-party ProperDocs migration notices do not represent a recommendation from the Material
  for MkDocs team". Package metadata now marks the project inactive [gh, pypi].

### Zensical

- "A modern static site generator built by the creators of Material for MkDocs" [pypi]. MIT;
  the repository is mostly Rust with a Python layer [gh]. First PyPI upload 0.0.1 on
  2025-05-17; 0.0.73 on 2026-10-26; releases arrive every few days (0.0.70 on 2026-10-12, 0.0.72
  on 2026-10-23) [pypi].
- **2026-11-05:** Zensical 0.1.0 "begins a dependable release line", staying on 0.x "while its
  module system and ecosystem take shape"; Zensical Studio (an editor/IDE product, Free, Pro and
  Team editions) and the Spark membership launch the same day [doc: zensical.org/upcoming-changes].
  The generator itself stays open source; Studio is the paid part.
- **Compatibility** [doc: zensical docs `compatibility/mkdocs/`]:
  - reads `mkdocs.yml` and "almost all settings", including `nav`, `theme`, `extra`, `plugins`;
    `zensical.toml` is the eventual format but "not required as part of adopting Zensical";
  - the only unsupported `mkdocs.yml` setting listed is `hooks`;
  - commands `build` and `serve`; no `gh-deploy`, no `get-deps`, no `--site-dir`/`--theme`
    flags;
  - Python Markdown and PyMdown Extensions as today; a Rust parser is planned [doc: Material
    blog 2025-11-05].
- **Plugins** [doc: `compatibility/mkdocs/plugins.md`]: Zensical "does not currently run
  arbitrary MkDocs plugins or expose the MkDocs plugin API"; a Module API is planned, first in
  early access through Spark. Configuration for an unlisted plugin is **silently ignored**;
  unknown settings on a listed plugin are an error. Natively supported (with the version that
  added each) include `search`, `offline`, `section-index`, `mkdocstrings`, `macros`,
  `awesome-nav` (0.0.58), `literate-nav` (0.0.58), `exclude`, `meta`, `redirects`, `tags`,
  `blog`, `rss`, `include-markdown`, `glightbox`, `minify`, `privacy`, `social`, `mike`,
  `nav-weight`. Explicitly **unsupported**: `gen-files` ("run the generation scripts separately
  from the Zensical build … Track the generated files in version control") and
  `open-in-new-tab`. Planned: the three `git-*` date/author plugins.

### ProperDocs

- `properdocs` 1.6.5 (2026-03-15) is "the first version of ProperDocs": MkDocs 1.6.1 renamed,
  reads `properdocs.yml` falling back to `mkdocs.yml`, runs both `mkdocs.*` and `properdocs.*`
  plugin and theme entry points, ships no built-in theme. Latest release 1.6.7 (2026-03-20);
  repository last pushed 2026-10-22; contributors include former MkDocs maintainers [pypi, gh].
- Some plugins now depend on it: `mkdocs-redirects` 1.2.3 requires `properdocs>=1.6.5`,
  `mkdocs-rss-plugin` 1.19.0 requires `mkdocs==1.6.1` and `properdocs<2,>=1.6.5` [pypi].
- It is the option that keeps arbitrary 1.x plugins running, but it has had no release in seven
  months and is not what Material's maintainers point to.

### What to start on

Zensical, configured through `mkdocs.yml` restricted to what Zensical supports, because:

- it is the only one of the four with an active release line and a stated successor role;
- it already covers every need in §3–§5 natively, without third-party plugins;
- keeping to `mkdocs.yml` and supported plugins lets the same file build with
  `mkdocs` 1.6.1 + Material 9.8 as a fallback while Zensical is 0.x [doc: migration guide,
  "the same project can be built with either command"].

The cost: Zensical is pre-1.0 and changes every few days, so pin its version and bump it on
purpose; and any plugin outside its list is ignored without an error, so a plugin added later
has to be checked against that list.

## 3. Patterns for this project's docs

### ADR index

There is no maintained ADR plugin: `mkdocs-material-adr` (1.2.1, 2025-04-03, 17 stars) is the
only one on PyPI and is a Material theme extension, not a Zensical-supported plugin [pypi, gh].
The pattern that needs nothing extra: zero-padded ADR file names (`0018-…md`) already sort in
number order under the default nav (§6), and `docs/adr/index.md` — hand-written, or written by
the same script that checks ADR numbers — lists them. `section-index` (supported natively) makes
that index the section's own page. `macros` could render the list at build time but adds a
Jinja layer to Markdown that is also read raw on GitHub and in lazyclaude.

### Mermaid

Native in both Material and Zensical: a `pymdownx.superfences` custom fence named `mermaid`;
Zensical "will automatically initialize the JavaScript runtime when a page includes a `mermaid`
code block", following the light/dark scheme [doc: zensical `authoring/diagrams.md`]. No
`mkdocs-mermaid2-plugin` (1.2.3, 2025-10-17, requires `mkdocs`, not in Zensical's list) [pypi].
The `mkdocs.yml` form uses a Python tag:
`format: !!python/name:pymdownx.superfences.fence_code_format`; `zensical.toml` takes the same
name as a plain string [doc].

### Generated pages apart from hand-written ones

- `gen-files` (build-time virtual pages) is unsupported in Zensical, and Zensical's own advice
  is to generate outside the build and commit the output [doc]. That is already how
  `/trace-endpoint` writes `docs/endpoints/`: committed Markdown, regenerated by a command.
- Keep generated output in its own directory (`docs/endpoints/`), so a regeneration rewrites a
  whole directory and never a hand-written page.
- Archify diagrams are standalone HTML. Any non-Markdown file under `docs_dir` is "copied
  across when generating your documentation site" unaltered [doc: MkDocs
  `writing-your-docs.md`], so they are served as-is and linked from a Markdown page; they do
  not appear in the nav. Zensical keeps the standard layout [doc]; that it copies arbitrary
  `.html` the same way is [unverified].
- `docs/backlog/` is not site content today; `exclude_docs` (supported by both) keeps it out
  [doc].

### Link checking and strict builds

- MkDocs 1.6: `validation` (`omitted_files`, `absolute_links`, `unrecognized_links`, `anchors`
  as `warn|info|ignore`) plus `strict: true` / `--strict`, which turns warnings into a failed
  build [doc: MkDocs `configuration.md`].
- Zensical: internal-link validation is **on by default**, `validation.invalid_links` and
  `invalid_link_anchors` default to `true`, and `zensical build --strict` aborts on them [doc:
  zensical `setup/validation.md`, `usage/build.md`]. Its other checks (unresolved references,
  footnotes) are marked deprecated pending its own Markdown parser [doc].
- External links: `mkdocs-htmlproofer-plugin` (1.6.0, 2026-09-28) requires `mkdocs>=1.4` and is
  not in Zensical's list [pypi], so it would be silently ignored. A separate step on the built
  `site/` — e.g. `lycheeverse/lychee-action` v2.9.0 (2026-07-09) [gh] — checks external links
  independently of the generator. Whether to check external links in CI at all (they flake) is
  a choice, not a default.

## 4. Publishing to GitHub Pages from Actions

- The current GitHub-supported path is a Pages artifact, not a `gh-pages` branch: repository
  Pages source set to "GitHub Actions", a workflow with `permissions: pages: write,
  id-token: write`, an environment `github-pages`, and the steps `actions/configure-pages`,
  build, `actions/upload-pages-artifact` (`path: site`), `actions/deploy-pages` [doc: zensical
  `publish-your-site.md`, which links GitHub's "Publishing with a custom GitHub Actions
  workflow"].
- Latest majors: `configure-pages` v6.0.0 (2026-03-25), `upload-pages-artifact` v5.0.0
  (2026-04-10), `deploy-pages` v5.0.1 (2026-09-01) [gh] — the versions Zensical's sample
  workflow uses.
- Zensical has no `gh-deploy` [doc]; MkDocs' `gh-deploy` pushes to a `gh-pages` branch with
  `ghp-import` [doc: MkDocs `deploying-your-docs.md`] — not needed with the artifact flow.
- Zensical's docs advise against caching on CI for now, since its cache format is changing
  [doc].
- A PR build can run `zensical build --strict` without the upload/deploy steps, so broken links
  fail CI before merge.

## 5. Versions and runtime

| Package | Version | Date | Note |
|---|---|---|---|
| `mkdocs` | 1.6.1 | 2024-08-30 | last 1.x; 2.0.dev6 on 2026-09-15 |
| `mkdocs-material` | 9.8.0 | 2026-10-24 | inactive, EOL 2027-05-05, Python ≥ 3.11 |
| `zensical` | 0.0.73 | 2026-10-26 | 0.1.0 on 2026-11-05, Python ≥ 3.11 |
| `properdocs` | 1.6.7 | 2026-03-20 | MkDocs 1.6.1 fork |
| `mkdocs-awesome-nav` | 3.3.0 | 2025-12-02 | native in Zensical |
| `mkdocs-literate-nav` | 0.6.3 | 2026-03-16 | native in Zensical |
| `mkdocs-gen-files` | 0.6.1 | 2026-03-16 | not in Zensical |
| `mkdocs-mermaid2-plugin` | 1.2.3 | 2025-10-17 | not needed |
| `mkdocs-htmlproofer-plugin` | 1.6.0 | 2026-09-28 | not in Zensical |

[pypi, 2026-10-26.] All are Python packages: the site build brings a Python toolchain into a
Java project's CI, separate from the Maven build.

## 6. `mkdocs.yml`'s nav and ADR 0018

ADR 0018 has the docs adapter hand over paths in tree order, segment by segment
(`DocumentPath.compareTo`, Java `String.compareTo` per segment), and the domain keeps that order.

**MkDocs' default nav, when `nav` is omitted** [doc: MkDocs `writing-your-docs.md`,
`configuration.md`; src: `mkdocs/structure/files.py` `get_files`, `_file_sort_key`;
`mkdocs/utils` `nest_paths`]: `os.walk` over `docs_dir`, sorting each directory's subdirectory
names and file names with Python's string order, `index` or `README` first. Per directory, that
gives files first, then subfolders — both in name order. Zensical's docs say the default nav is
built "on the basis of the folder structure" and claim MkDocs-compatible navigation; its exact
sort is [unverified].

Compared with ADR 0018's order:

- same: segment by segment, plain code-point order (`adr` before `adr-old`), zero-padded ADR
  numbers in number order;
- different: MkDocs puts `index.md`/`README.md` first and a directory's files before its
  subfolders; lazyclaude interleaves files and folders by name. Neither changes anything for
  today's tree, which has no index pages and only `context-map.md` and `glossary.md` at the root.

**Reading an explicit `nav:` instead** would cost more than it gives:

- `nav` lists titled entries, external URLs and sections that need not match folders; pages
  left out are still built (`not_in_nav`); so the panel would need a rule for every file the nav
  does not mention, and the two orders could disagree.
- With `awesome-nav` or `literate-nav` the order moves into per-directory `.nav.yml` or
  `SUMMARY.md` files, and Zensical's `zensical.toml` is its eventual home — three formats to
  follow.
- `mkdocs.yml` as written for mermaid carries `!!python/name:` tags; snakeyaml-engine (what
  lazyclaude parses YAML with) does not know them, so reading the file needs a tolerant
  constructor [unverified — not tried].
- The shell would join a second file into the docs context's order, which ADR 0018 placed in
  the adapter alone.

So: **omit `nav` and let both orders come from the file names.** If an explicit order is ever
wanted, number the files (as ADRs already are), which both the site and the panel follow
without either reading the other. The two small differences (index first, files before folders)
are a choice for the layout ticket: adopt them in `DocumentPath`'s order, or accept them.

## Sources

- PyPI JSON API, read 2026-10-26: `https://pypi.org/pypi/<name>/json` for `mkdocs`
  (incl. `2.0.dev6`), `mkdocs-material`, `zensical`, `properdocs`, `mkdocs-gen-files`,
  `mkdocs-literate-nav`, `mkdocs-awesome-nav`, `mkdocs-mermaid2-plugin`,
  `mkdocs-htmlproofer-plugin`, `mkdocs-macros-plugin`, `mkdocs-redirects`, `mkdocs-rss-plugin`,
  `mkdocs-material-adr`.
- Material for MkDocs releases 9.7.5–9.8.0 — https://github.com/squidfunk/mkdocs-material/releases
- "Zensical" (2025-11-05, updated 2026-10) —
  https://squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/
- "MkDocs 2.0" (2026-02-18, updated since) —
  https://squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/
- Zensical upcoming changes — https://zensical.org/upcoming-changes/
- Zensical docs, from the `zensical/docs` repository (`master`, read 2026-10-26):
  `compatibility/mkdocs/index.md`, `migration.md`, `plugins.md`, `setup/validation.md`,
  `setup/navigation.md`, `authoring/diagrams.md`, `usage/build.md`, `publish-your-site.md`;
  published at https://zensical.org/docs/
- Zensical releases — https://github.com/zensical/zensical/releases
- ProperDocs release notes — https://github.com/properdocs/properdocs (`docs/about/release-notes.md`)
- MkDocs 1.x docs and source, `mkdocs/mkdocs` `master`: `docs/user-guide/configuration.md`
  (`nav`, `not_in_nav`, `validation`, `strict`), `writing-your-docs.md`,
  `deploying-your-docs.md`, `mkdocs/structure/files.py`, `mkdocs/utils/__init__.py`
- GitHub Actions releases: `actions/configure-pages`, `actions/upload-pages-artifact`,
  `actions/deploy-pages`, `lycheeverse/lychee-action`
- lazyclaude: `docs/adr/0018-the-docs-adapter-orders-the-tree.md`,
  `src/main/java/dev/maxenceng/lazyclaude/docs/published/DocumentPath.java`
