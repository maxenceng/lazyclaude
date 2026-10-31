# 71. The docs tree puts a folder's README first, then its files, then its subfolders

Date: 2026-10-31

## Status

Accepted. Supersedes the order in ADR 0018; where the order is applied stays as 0018 has it.

## Context

ADR 0018 ordered the docs tree by name at each level, segment by segment. The docs now also build into a site (ADR
0069), and the site shows each folder's `README.md` as its index page, ahead of the folder's other pages. Ordering the
panel by name alone put `docs/README.md` after every folder whose name sorts before it and buried each folder's
`README.md` among its files, so the panel and the site disagreed on what comes first. Reading the site's order from a
`nav:` section was rejected: `mkdocs.yml` has none, and keeping one would be a second list of the docs to maintain.

## Decision

- **At each level, a folder's `README.md` comes first, then its other files by name, then its subfolders by name.**
  `docs/README.md` heads the whole tree. Only a level's own `README.md` heads it: `adr/superseded/README.md` comes
  among `adr/`'s subfolders, not before `adr/`'s files.
- **Names still compare segment by segment**, so `adr/…` comes before `adr-old/…`. A folder with no `README.md`
  orders its files and subfolders by name, as before.
- **The rule is `DocumentPath`'s comparison.** `DocsPort.paths` hands paths over in that order and `DocsTree.of` keeps
  it, as ADR 0018 decided. `DocsTree` holds a level's files apart from its folders, so its shape agrees with the
  files-before-subfolders tier rather than depending on it; the tier is what makes the flat list read in tree order.
- **`README.md` is matched exactly**, by case, as the site build does; `index.md` gets no special place, because the
  site is built from `README.md`.

## Consequences

- The panel opens on `docs/README.md` and shows each folder's index first, as the site does, with no `nav:` to parse.
- A folder whose index is named differently (`readme.md`, `index.md`) sorts among its files by name. README-as-index
  is the convention the panel follows for every project, whether or not it builds a site.
