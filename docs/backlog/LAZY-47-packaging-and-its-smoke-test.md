---
id: LAZY-47
status: done
context: cross-context
type: decision
created: 2026-10-27
blocked_by: [LAZY-46]
---

# How is the desktop frontend packaged, and what does a smoke test of the bundle check?

## Question

How is the desktop frontend packaged, and what does a smoke test of the bundle check?

Which bundle format or formats to ship, and where the runtime and the jar sit in it. Whether `make` builds the bundle,
and whether CI builds and keeps it as an artifact or only checks that it builds. What a smoke test of the installed
bundle checks: the sidecar's `ready`, a hosted `claude` started and quit, no `claude` left behind. Which parts are
automated and which are walked by hand. How the WebKitGTK workaround and the Hyprland focus risk from LAZY-36 are
handled in a packaged build.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0059](../adr/0059-the-desktop-frontend-ships-as-a-deb-and-an-appimage-smoke-tested-in-ci.md).

- **Words**: *Bundle* (the deb or AppImage, not `make package`'s jar) and *Runtime* (the jlink image), in the
  glossary's `sidecar` section.
- **Formats**: a deb, which CI installs and checks, and an AppImage, which is what gets used on CachyOS. Rejected: rpm,
  which nothing here installs, and `--no-bundle` with a `make install`, which leaves the AppImage untested.
- **Layout**: `runtime/`, `lazyclaude.jar` and `natives/` under `/usr/lib/<productName>/`, with the relay passing the
  pty4j and JNA properties. Stripped, no `--compress`, no CDS archive (29 MB for about 80 ms).
- **Build**: `make bundle`, outside `make verify`, links from a Temurin 25 it downloads at a pinned version and
  SHA-256, so a local bundle is CI's. Rejected: linking from `JAVA_HOME`, whose Arch JDK needs glibc 2.38. The pom's
  version is written into the `tauri build --config` file. `make doctor` reports the bundle's needs.
- **CI**: a `bundle` job on `ubuntu-22.04` builds and smoke-tests on every pull request. The deb and AppImage are
  uploaded as 14-day artifacts on `main` only. Rejected: a release workflow.
- **Smoke test**: one script, run by CI and `make bundle-smoke`, against the installed deb and the extracted AppImage.
  It checks that `ready` comes first, starts a hosted session on a stub `claude`, closes stdin, and checks that no stub
  is left and no `/tmp/pty4j-*` appeared. It also runs the relay under `xvfb-run` and checks from outside that it
  spawned the sidecar. The relay gets no test-only flag.
- **By hand**: walked once on the AppImage, as the packaging ticket's acceptance criteria: a real `claude` on
  Hyprland/NVIDIA, DMA-BUF, the key table with F5, Ctrl+R and the context menu, `q` leaving no `claude`, and the kitty
  focus loss.
- **Risks**: the DMA-BUF variable stays in the relay's `main`. A focus loss that recurs in a packaged build becomes its
  own fix ticket, not a blocker.
- **Scope**: the bundle ships only the desktop frontend. The TUI keeps the jar and `make run`.

Graduated into [DESKTOP-6](DESKTOP-6-the-desktop-frontend-ships-as-a-bundle.md).
