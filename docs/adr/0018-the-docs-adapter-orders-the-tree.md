# 18. The docs adapter hands paths over in tree order; the domain keeps that order

Date: 2026-10-10

## Status

Accepted. Its order is superseded by ADR 0071: a folder's `README.md` first, then its files, then its
subfolders. Where the order is applied stands.

## Context

The docs tree shows each level sorted by name. The first cut sorted in the tree's factory, and a
later fix moved the sort into `Folder`'s constructor so every `Folder` was sorted. Review pushed
it the other way: ordering is done before anything enters the domain, as `BacklogRepository`
already does for the board's files, and `Folder` is a plain record of what it holds.

## Decision

- **`DocsPort.paths` returns paths in tree order**, the order `DocumentPath` defines: segment by
  segment, so `adr/…` comes before `adr-old/…`. Comparing whole strings would put `adr-old`
  first, because `-` comes before `/`.
- **`DocsTree.of` builds the nesting and keeps the order it is given.** A folder comes where its
  first document did. Neither `DocsTree` nor `Folder` sorts.
- **The tree's root is not a `Folder`.** `DocsTree` holds the files directly under `docs/` and
  its folders. Each `Folder` carries a published `FolderPath` that names it in the same frame as
  `DocumentPath` (relative to `docs/`), so the shell keys folded folders by it rather than
  joining names itself.

## Consequences

- A `DocsPort` implementation that hands paths over unsorted makes the panel unsorted. The order
  is tested in `DocsRepositoryTest`, not in the domain.
- `DocsTree.of` called with documents in any other order keeps that order, which is what makes
  the domain simple to test.
