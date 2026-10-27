# 59. The desktop frontend ships as a deb and an AppImage, built by `make bundle` and smoke-tested in CI

Date: 2026-10-27

## Status

Accepted. Builds on ADR 0048 (a hosted session ends with its frontend), ADR 0056 (Linux x86_64 only) and ADR 0057
(the desktop frontend).

## Context

The desktop frontend is a Tauri window whose relay spawns the sidecar, a jar on a jlink runtime. LAZY-46 found what
that runtime needs:
- linked from Temurin 25, for its glibc 2.17 floor;
- pty4j's and JNA's natives pre-extracted, or every run leaves a `/tmp/pty4j-*` directory;
- the runtime and jar shipped as `bundle.resources` from a `tauri build --config` file.

It also found that Tauri bundles deb, rpm and AppImage on Linux. The AppImage is the riskiest: linuxdeploy rewrites
every ELF file in the runtime, it needs FUSE 2, and its Mesa fix landed only in tauri-bundler 2.10.0. The one
developer runs CachyOS, where only the AppImage installs as it comes. CI runs on Ubuntu, where only the deb installs as
it comes. Neither has a real `claude` and a display at once.

## Decision

- **A deb and an AppImage, no rpm.** The deb is what CI installs and checks. The AppImage is what gets used. Nothing
  here ever installs an rpm. Rejected: `tauri build --no-bundle` plus a `make install`, which leaves the AppImage's
  rewrite of the runtime untested on the machine that uses it.
- **Inside either one, `/usr/lib/<productName>/` holds `runtime/`, `lazyclaude.jar` and `natives/`.** The runtime is
  stripped, without `--compress` and without a CDS archive: 29 MB buys about 80 ms of start-up, which the window
  opening hides. The relay starts `runtime/bin/java` with `-Dpty4j.preferred.native.folder`, `-Djna.boot.library.path`
  and `-Djna.nounpack=true`, all pointing at `natives/`.
- **`make bundle` builds both, outside `make verify`.** It downloads a Temurin 25 into `target/` at a pinned version
  and SHA-256 and links from it, so a local bundle is CI's bundle. Rejected: linking from `JAVA_HOME`. Arch's JDK
  needs glibc 2.38, so a bundle linked from it would differ from the one CI tested. `make doctor` reports what
  `make bundle` needs. The pom's version is the bundle's: `make bundle` writes it into the `tauri build --config` file.
- **CI's `bundle` job builds and smoke-tests the bundle on every pull request, on `ubuntu-22.04`.** The build host sets
  the deb's glibc floor. The job uploads the deb and the AppImage as artifacts only on `main`, kept 14 days. Rejected:
  a release workflow publishing to GitHub Releases. It has no user yet, and adding one later decides nothing new.
- **The smoke test is one script, run by CI and by `make bundle-smoke`, against the installed deb and the extracted
  AppImage.** It:
  1. starts the sidecar on the bundle's own runtime and natives, and checks that `ready {protocol}` comes first;
  2. starts a hosted session, with a stub `claude` on `PATH` that echoes and waits;
  3. closes stdin, then checks that the sidecar exits, no stub is left and no `/tmp/pty4j-*` appeared;
  4. runs the relay under `xvfb-run` and checks, from outside, that it spawned the sidecar and that both are alive after
     a few seconds.

  The relay gains no test-only flag. A real `claude` is not in it: the stub covers what packaging can break, which is
  the natives, the pty and the lifetime.
- **What a stub and Xvfb cannot show is walked by hand once, on the AppImage, as the packaging ticket's acceptance
  criteria.** The checks are:
  - a real `claude` on Hyprland with NVIDIA, rendering without DMA-BUF artefacts;
  - whether the kitty window still loses focus (LAZY-36);
  - the key table, F5, Ctrl+R and the context menu (ADR 0057, ADR 0058);
  - `q` leaving no `claude`.

  There is no release cadence to repeat the walk on.
- **A focus loss that recurs in a packaged build is a fix ticket of its own**, not a blocker for the bundle. The
  relay's `main` keeps setting `WEBKIT_DISABLE_DMABUF_RENDERER` when unset. The walk only confirms it.
- **The bundle ships only the desktop frontend.** The TUI keeps the jar and `make run`. Rejected: a `lazyclaude-tui`
  launcher on the same runtime, which the AppImage cannot expose cleanly and nobody has asked for.

## Consequences

- A bundle is never built by `make verify`, so a change that breaks only packaging shows first in the pull request's
  `bundle` job.
- Moving the pinned Temurin is an edit to the version and the checksum together.
- A real `claude` is never run against a bundle in CI. A Claude Code upgrade that breaks only the packaged frontend
  shows in use, not in the build.
- An Arch user other than this one would install the AppImage as well. A pacman package is not built.
- The 14-day artifacts are the only built bundles kept anywhere.
