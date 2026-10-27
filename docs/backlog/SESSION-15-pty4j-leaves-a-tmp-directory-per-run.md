---
id: SESSION-15
status: draft
context: session
type: fix
tracker_id: 117
created: 2026-10-27
---

# pty4j leaves a `/tmp/pty4j-linux-x86-64-*` directory behind on every run

## Acceptance criteria

_Not analysed yet — `/ticket refine SESSION-15`._

## Model decision

_Not analysed yet — `/ticket refine SESSION-15`._

## Design direction

_Not analysed yet — `/ticket refine SESSION-15`._

## Glossary impact

_Not analysed yet._

## Notes

Found by [LAZY-46](LAZY-46-what-a-jlink-bundle-needs.md)'s research (`docs/research/jlink-sidecar-bundle.md`): `deleteOnExit` cannot remove a non-empty directory, and 26 were found in `/tmp`. Suggested fix: ship the natives pre-extracted and point pty4j at them (`-Dpty4j.preferred.native.folder`, `-Djna.boot.library.path`, `-Djna.nounpack=true`).
