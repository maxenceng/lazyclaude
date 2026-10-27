---
id: DESKTOP-6
status: todo
context: desktop
type: feature
tracker_id: 119
created: 2026-10-27
---

# The desktop frontend ships as a deb and an AppImage

## Acceptance criteria

- [ ] `make bundle` downloads Temurin 25 into `target/` at a pinned version, refuses a SHA-256 mismatch and links the
      runtime from it: `java.base,java.desktop,java.management,java.sql,java.xml,jdk.jfr`, stripped, no `--compress`,
      no CDS archive.
- [ ] `make bundle` builds a deb and an AppImage holding `runtime/`, `lazyclaude.jar` and `natives/` (`libpty.so`,
      `libjnidispatch.so`) under `/usr/lib/<productName>/`, listed as `bundle.resources` in a `tauri build --config`
      file that also carries the pom's version.
- [ ] The relay starts `resource_dir()/runtime/bin/java` with `-Dpty4j.preferred.native.folder`,
      `-Djna.boot.library.path` and `-Djna.nounpack=true` pointing at `natives/`; `LAZYCLAUDE_SIDECAR` still overrides
      it in development.
- [ ] `make doctor` reports what `make bundle` needs; `make verify` does not build a bundle.
- [ ] `make bundle-smoke` runs one script against the installed deb and the extracted AppImage. It checks that `ready`
      comes first, that a hosted session starts on a stub `claude` from `PATH`, and that after stdin closes the sidecar
      exits with no stub and no `/tmp/pty4j-*` left. It also checks that the relay under `xvfb-run` spawns the sidecar
      and both are alive after a few seconds.
- [ ] Each smoke check is watched to fail once: natives missing from the bundle, and a stub left running.
- [ ] CI's `bundle` job, on `ubuntu-22.04`, runs `make bundle` and the smoke test on every pull request. It uploads the
      deb and the AppImage as artifacts only on `main`, kept 14 days.
- [ ] Walked by hand on the AppImage, on Hyprland with NVIDIA and a real `claude`:
  - the window renders with no DMA-BUF artefacts;
  - the TUI's key table, F5, Ctrl+R and the context menu behave as ADR 0057 and ADR 0058 say;
  - `q` leaves no `claude`;
  - whether the kitty window loses focus while the window is open (LAZY-36) is recorded. If it does, that is a fix
    ticket of its own, not a failure of this one.

## Model decision

No backend change. The bundle carries the jar `make package` already builds (ADR 0059).

## Design direction

None. Nothing visible changes.

## Glossary impact

*Bundle* and *Runtime*, in the `sidecar` section.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-47](LAZY-47-packaging-and-its-smoke-test.md), recorded in ADR 0059, the runtime's facts in
[LAZY-46](LAZY-46-what-a-jlink-bundle-needs.md) and `docs/research/jlink-sidecar-bundle.md`. Blocked by
[DESKTOP-2](DESKTOP-2-hosted-sessions-in-the-desktop-frontend.md): the smoke test and the walk need a hosted session
in a pane.
