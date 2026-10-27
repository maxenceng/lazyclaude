---
id: DOCS-4
status: todo
context: docs
type: feature
tracker_id: 111
created: 2026-10-27
---

# The Docs panel orders documents as the site does

## Acceptance criteria

- [ ] At each level of the docs tree, the folder's `README.md` comes first, then its other files by name, then its
      subfolders by name; `docs/README.md` heads the whole tree.
- [ ] Names still compare segment by segment, so `adr/…` comes before `adr-old/…`.
- [ ] A folder with no `README.md` orders as before.
- [ ] `.html` files stay unlisted.
- [ ] An ADR supersedes 0018's order, and 0018's status says so.

## Model decision

The order is the adapter's to hand over, as ADR 0018 has it: `DocsPort.paths` returns paths in the new order and
`DocsTree.of` keeps it. `DocumentPath`'s comparison is where the README-first rule goes.

## Glossary impact

*Docs tree*'s row names the README-first order.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-35](LAZY-35-docs-on-mkdocs.md) so the
panel shows what the site shows without a `nav:` to parse. If DOCS-2 found Zensical needs `index.md`, `index.md` takes
`README.md`'s place here.
