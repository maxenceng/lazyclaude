# 102. The bundle job caches the Temurin download and tauri's AppImage tools, keyed on their pins

Date: 2026-11-08

## Status

Accepted. Extends [ADR 0059](0059-the-desktop-frontend-ships-as-a-deb-and-an-appimage-smoke-tested-in-ci.md).

## Context

CI's `bundle` job (ADR 0059) fetched two things from GitHub releases on every run: the pinned Temurin 25 archive, in
`scripts/bundle.sh`, and the tools tauri's AppImage step downloads into `~/.cache/tauri` (AppRun, linuxdeploy and its
plugins). A pull request's `bundle` job failed twice in a row on an HTTP 504 from those downloads, once for each, though
the branch caused neither; `curl --retry 3` did not outlast the outage.

## Decision

- **`target/bundle/download` is cached under the Makefile's `TEMURIN_SHA256`**, read from the Makefile by a step that
  fails if it finds none. `bundle.sh` already reuses a kept archive only while it matches the pin, and deletes and
  downloads again one that does not, so a corrupt restore is never linked from.
- **`~/.cache/tauri` is cached under the `@tauri-apps/cli` version locked in `frontend/pnpm-lock.yaml`**, the CLI that
  chooses which AppRun and linuxdeploy to fetch. Read the same way, failing closed.
- **No `restore-keys`.** A bumped pin misses the cache and downloads afresh; an older pin's files are never restored
  under the new one. Rejected: a prefix fallback like the pnpm and cargo caches, which suits a store that tolerates
  extra entries, not a pinned binary.
- **A cache, not a longer retry.** Rejected: a longer `curl` retry in `bundle.sh`, which reaches only Temurin; tauri
  fetches its AppImage tools itself, and no flag of ours reaches those downloads.
- **Restore and save are separate steps** (`actions/cache/restore` and `actions/cache/save`), and the saves run right
  after `make bundle`, so a failing install or smoke test still leaves both caches filled. Rejected: the combined
  `actions/cache`, whose post-job save is skipped when any step fails. A save whose key was restored is skipped; one
  whose key another run saved meanwhile is a warning, not a failure.
- **Temurin is saved even when `make bundle` failed**, if an archive is there: `bundle.sh` moves it into place only once
  its SHA-256 matched. **Tauri's tools are saved only after a whole bundle**, because a build that failed mid-fetch could
  freeze an incomplete set under a key no later run would refill.

## Consequences

- A cold run still downloads both and can still fail on a 504. A pull request reads only its own entries and
  `main`'s, so a pin bump runs cold twice, on its pull request and on the push to `main`; an entry is also lost after
  seven idle days or under the repository's cache size limit. Accepted: a rerun of the job is the remedy.
- A cache entry is never overwritten, so a restored archive that fails the checksum is downloaded again on every run
  until its entry is deleted by hand. Only an archive whose checksum matched is ever saved, so this needs a corrupted
  store, not a bad download.
- The tauri key follows the CLI version, not the bundler crate in `Cargo.lock`. If tauri ever moves the AppImage tools'
  choice out of the CLI, the key must follow it.
