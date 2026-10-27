---
id: LAZY-46
status: done
context: cross-context
type: decision
created: 2026-10-27
blocked_by: [LAZY-40, LAZY-41]
---

# What does a jlink runtime for the sidecar need, and how does Tauri bundle it on Linux?

## Question

What does a jlink runtime for the sidecar need, and how does Tauri bundle it on Linux?

- Which JDK modules does the fat jar need (`jdeps` over it and its dependencies: pty4j, JLine, Jackson, SnakeYAML
  Engine, TamboUI)? What does pty4j's native library need at runtime, and how big is the runtime
  (`--strip-debug`, `--no-header-files`, `--no-man-pages`, compression)?
- Can Tauri 2's `externalBin` sidecar point at a launcher script, or must it be a binary? How does a runtime directory
  ship under `bundle.resources`, and how does the relay find it at run time?
- Which Linux bundle formats does Tauri 2 build (deb, rpm, AppImage)? What does each need of the system — WebKitGTK as a
  dependency, glibc floor — and what are the known problems with WebKitGTK inside an AppImage?
- Does `WEBKIT_DISABLE_DMABUF_RENDERER=1` (LAZY-36) have to be set by the bundle, and how?

## Kind

research

## Resolution

Researched 2026-10-27; details in [`docs/research/jlink-sidecar-bundle.md`](../research/jlink-sidecar-bundle.md).

- **Modules**: jdeps gives `java.base,java.desktop,java.management,java.sql,jdk.jfr`. `java.xml` is required even
  though jdeps never names it (Jackson). The TUI starts without `java.desktop`, but a hosted session was not tried
  without it, so it stays.
- **Size**: `--strip-debug --no-header-files --no-man-pages` gives 84 MB, 29 MB gzipped. `--compress=zip-6` gives
  53 MB but 37 MB gzipped, so it works against a package. `--generate-cds-archive` costs 29 MB and saves about 80 ms
  of start-up. The jar (15.6 MB) starts on the image and spawns a pty.
- **JDK to link from**: Temurin 25 (glibc 2.17, its own zlib and freetype; no jmods, linked through JEP 493), not
  Arch's JDK (glibc 2.38, system libraries).
- **pty4j**: needs `libpty.so` and JNA's `libjnidispatch.so`, each needing only glibc ≤ 2.7. Pre-extracted and pointed
  at with `-Dpty4j.preferred.native.folder`, `-Djna.boot.library.path` and `-Djna.nounpack=true`, they work. Otherwise
  pty4j leaves a `/tmp/pty4j-*` directory behind on every run.
- **Tauri**: no `externalBin`. The runtime and the jar ship under `bundle.resources` with their executable bits, at
  `/usr/lib/<productName>/` (deb, rpm) or `$APPDIR/usr/lib/<productName>/`. The relay spawns
  `resource_dir()/runtime/bin/java` itself. Resources are listed in a `tauri build --config` file, because
  `tauri-build` copies them, and fails without them, on every `cargo` build.
- **Formats**: deb, rpm and AppImage. deb and rpm declare only WebKitGTK 4.1 and GTK 3, and get their glibc floor from
  the build host (Tauri suggests Ubuntu 22.04 or Debian 12). The AppImage's EGL crash on newer Mesa and its forced
  XWayland were fixed only in tauri-bundler 2.10.0. linuxdeploy rewrites every ELF file in the runtime, and the
  AppImage needs FUSE 2.
- **DMA-BUF**: no bundle sets `WEBKIT_DISABLE_DMABUF_RENDERER`. The relay's `main` sets it when unset, in an `unsafe`
  block (edition 2024), before `tauri::Builder`.
