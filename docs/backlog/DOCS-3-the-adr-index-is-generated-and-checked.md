---
id: DOCS-3
status: todo
context: docs
type: feature
tracker_id: 110
created: 2026-10-27
---

# The ADR index is generated, and the build fails when it is stale

## Acceptance criteria

- [ ] `make adr-index` writes `docs/adr/README.md`: one line per ADR in number order, its number, its title from the
      H1 linked to the file, and its status line (`Accepted.`, `Superseded by 0047.`).
- [ ] `make adr-check` fails, naming the first differing line, when `docs/adr/README.md` is not what `adr-index`
      would write; it never edits the file.
- [ ] `adr-check` no longer counts `docs/adr/README.md` as an unnumbered ADR; any other unnumbered file still fails.
- [ ] A fixture with a stale index trips the guard under `guard-fixtures`, and one with an extra unnumbered file
      still does.
- [ ] The committed index is current.

## Model decision

A script under `scripts/`, beside `check-adr-numbers.py`, sharing its reading of the ADR folder; no Java change. The
Docs panel lists `docs/adr/README.md` as an ordinary document.

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in [LAZY-35](LAZY-35-docs-on-mkdocs.md). No ADR
plugin is maintained ([LAZY-34](LAZY-34-mkdocs-ecosystem.md)). Index pages are `README.md`; if DOCS-2 found Zensical
needs `index.md`, this one writes `docs/adr/index.md` instead.
