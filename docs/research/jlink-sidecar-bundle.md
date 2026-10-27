# What a jlink runtime for the sidecar needs, and how Tauri bundles it on Linux

Researched 2026-10-27 on Linux x86_64 (CachyOS, glibc 2.44), against this repository's fat jar
(`make package`, `target/lazyclaude.jar`, 15.6 MB), JDK **25.0.5** (Arch Linux's build, which
was run, and Temurin **25.0.5+8**, which was only inspected), pty4j **0.14.0**, JNA **5.19.1**,
jediterm-core **3.76**, JLine **3.25.1**, TamboUI **0.5.0**, Jackson **3.2.3**, SnakeYAML Engine
**3.2**, Tauri CLI **2.12.4** (tauri-bundler **2.10.4**), tauri-build **2.7.4**, tauri-utils
**2.10.2**, linuxdeploy at `07333c6` (2026-08-01) and the `rpm` crate **0.30.0**. It builds on
[`cross-platform-blockers.md`](cross-platform-blockers.md), which covers cross-linking, one
runtime per OS, and `externalBin`'s target-triple rule. Those points are not repeated here.
`lazyclaude --sidecar` does not exist yet, so the runtime was checked by starting and quitting
the TUI, and by spawning a pty from the same jar.

Sources, in order of trust:

- **run**: commands run here: `jdeps`, `jlink`, `readelf`/`objdump`, the TUI started on the
  linked runtime under a pty, and a probe class that spawns a pty through pty4j on it.
- **jar**: the fat jar's contents and the pty4j `-sources.jar` from the local Maven repository.
- **src**: upstream source: `tauri-apps/tauri` at `7e39750` (2026-10-27), cited as
  `crates/…:line`; `tauri-build-2.7.4` and `tauri-utils-2.10.2` from the local cargo
  registry; `linuxdeploy/linuxdeploy` at `07333c6`; the `rpm` 0.30.0 crate.
- **doc**: first-party documentation: `v2.tauri.app` (`develop/sidecar`, `develop/resources`,
  `distribute/`, `distribute/debian`, `distribute/rpm`, `distribute/appimage`,
  `develop/debug/linux-graphics`), `openjdk.org/jeps/493`, Adoptium's JEP 493 announcement.
- **gh**: `tauri-apps/tauri` issues and pull requests, and the bundler's `CHANGELOG.md`.

## Summary

| Question | Answer |
|---|---|
| JDK modules | `java.base,java.desktop,java.management,java.sql,jdk.jfr`, which resolves to 10 modules. Only `java.xml` is truly required beyond `java.base`; the rest come from code paths the app does not use |
| Runtime size | 53 MB with `--strip-debug --no-header-files --no-man-pages --compress=zip-6`, 84 MB without `--compress`. The uncompressed one is the smaller download: 29 MB gzipped against 37 MB |
| Starts on it | yes: the TUI paints in 0.85 s and quits with `q`, exit 0. A pty spawns through pty4j |
| pty4j's native | `libpty.so` (27 KB, glibc ≥ 2.4) plus JNA's `libjnidispatch.so` (134 KB, glibc ≥ 2.7). Both are extracted at run time unless pointed at a pre-extracted copy. pty4j leaves its temp directory behind on every run |
| glibc floor | set by the JDK the runtime is linked from: 2.38 for Arch's build, 2.17 for Temurin's |
| `externalBin` a script? | nothing forbids it, and nothing in the bundlers needs an ELF file. But it is not needed: the relay can spawn `$RESOURCE/runtime/bin/java` itself |
| Runtime as a resource | a directory ships whole under `bundle.resources`, executable bits kept, at `/usr/lib/<productName>/` (deb, rpm) or `$APPDIR/usr/lib/<productName>/` (AppImage). `app.path().resource_dir()` finds it |
| Bundle formats | `deb`, `rpm`, `appimage`. Depends/Requires: `libwebkit2gtk-4.1-0` and `libgtk-3-0` only. Build on the oldest system to support (Tauri suggests Ubuntu 22.04 or Debian 12) |
| AppImage + WebKitGTK | over-bundled Wayland/xcb libraries broke WebKit on newer Mesa until tauri-bundler 2.10.0. linuxdeploy also rewrites every ELF under `usr/lib`, the runtime included. FUSE 2 is still required |
| `WEBKIT_DISABLE_DMABUF_RENDERER` | no bundle format sets it. The relay's `main` must set it before Tauri starts, and only if it is unset |

## Which JDK modules the jar needs

`jdeps --print-module-deps --ignore-missing-deps --multi-release 25 target/lazyclaude.jar`
prints `java.base,java.desktop,java.management,java.sql,jdk.jfr` in 1.3 s. Without
`--ignore-missing-deps` it fails on JLine's Jansi/Windows classes, which the shaded jar does not
carry. [run]

Who pulls each module, from `jdeps -verbose:package`: [run]

| Module | Pulled by | Reached at run time? |
|---|---|---|
| `java.desktop` | jediterm's `TtyConnector` (a deprecated `resize(java.awt.Dimension)`), JNA's `com.sun.jna.platform` (AWT/Swing helpers), JLine's `console.impl`, Jackson's `ext.beans` (`java.beans`) | not on the paths tried (see below) |
| `java.management` | JLine's `builtins` (`java.lang.management`) | not by the TUI |
| `java.sql` | Jackson's `ext.sql` | only to serialize `java.sql` types |
| `jdk.jfr` | TamboUI's `dev.tamboui.jfr` and `toolkit.jfr` events | TamboUI's choice |

The image also gets the modules these require: `java.xml`, `java.logging`, `java.prefs`,
`java.datatransfer` and `java.transaction.xa` (`java --list-modules` on the image). [run]

Trimming, tried by starting the TUI on each image: [run]

- `java.base` alone fails at once with `NoClassDefFoundError: org/w3c/dom/Node` from
  Jackson's `OptionalHandlerFactory.<clinit>`. **`java.xml` is needed**, even though jdeps
  never names it: it comes in only because `java.desktop` requires it.
- `java.base,java.xml,java.management,java.sql,jdk.jfr` without `java.desktop` starts and
  paints the Board, and the image is 44 MB instead of 53 MB. Whether a hosted pane still works
  is not shown. jediterm's `TtyConnector` names `java.awt.Dimension`, and the TUI's
  `PtyConnector` implements it. ADR 0049 moves emulation out of the sidecar, which would make
  that path TUI-only. **Keep the jdeps set** unless a test hosts a session on a trimmed image.

No `jdk.unsupported` is needed: nothing in the jar references `sun.misc`. [run]

## What pty4j's native library needs

The jar carries every platform's natives: pty4j's `libpty` for 9 OS/arch pairs and its Windows
helpers, JNA's `libjnidispatch` for 25, and JLine's `libjlinenative` for 14. The ones for other
platforms come to 13.2 MB uncompressed, or 4.6 MB of the jar's 15.6 MB. Linux x86_64 uses
three: [jar, run]

| Library | Size | `NEEDED` | glibc floor |
|---|---|---|---|
| `resources/com/pty4j/native/linux/x86-64/libpty.so` | 27 KB | `libc.so.6` | 2.4 |
| `com/sun/jna/linux-x86-64/libjnidispatch.so` | 134 KB | `libc.so.6` | 2.7 |
| `org/jline/nativ/Linux/x86_64/libjlinenative.so` | 14 KB | `libutil.so.1`, `libc.so.6` | 2.2.5 |

The sidecar has no terminal, so JLine's native library matters only to the TUI.

**How pty4j finds `libpty.so`** (`com/pty4j/util/PtyUtil.java`, `ExtractedNative.java`): [jar]

- If `-Dpty4j.preferred.native.folder=<dir>` names a directory, it loads
  `<dir>/linux/x86-64/libpty.so` and extracts nothing.
- Otherwise it extracts every bundled native for the platform into a directory from
  `Files.createTempDirectory("pty4j-linux-x86-64-")` and marks it `deleteOnExit()`. A
  non-empty directory cannot be deleted that way, so **the directory stays after every
  run**. 26 of them were sitting in this machine's `/tmp`. `-Dpty4j.tmpdir=<absolute dir>`
  reuses one fixed subdirectory instead, and checks it by MD5.
- JNA unpacks `libjnidispatch.so` into `~/.cache/JNA/temp` and removes it.
  `-Djna.boot.library.path=<dir> -Djna.nounpack=true` loads a pre-extracted copy instead.

**Tried**: on the linked runtime, the probe spawned `/bin/sh` on a pty in 113–128 ms with exit
0. With `libpty.so` pre-extracted to `<dir>/linux/x86-64/`, and `libjnidispatch.so` to a
directory passed as `jna.boot.library.path` with `jna.nounpack=true`, it ran the same and
created no `/tmp/pty4j-*` directory. So a bundle can ship both libraries next to the runtime
and pass the three `-D` flags. Its natives can then never be stale or left behind. [run]

The manifest has `Enable-Native-Access: ALL-UNNAMED`, which covers `java -jar`. The Makefile
also passes `--enable-native-access=ALL-UNNAMED`. jlink's `--add-options` can bake fixed JVM
flags into the runtime, but not install-relative paths, so the `-D` paths come from whoever
spawns `java`. [jar, run]

## How big the runtime is, and the flags

All images use the same five modules, linked from Arch's JDK 25.0.5, which ships `jmods/`: [run]

| Flags | On disk | `tar` + gzip -6 | `tar` + xz -6 |
|---|---|---|---|
| none | 597 MB (Arch's jmods carry a 535 MB unstripped `libjvm.so`) | — | — |
| `--strip-debug --no-header-files --no-man-pages` | **84 MB** | **29.3 MB** | **20.0 MB** |
| the same plus `--compress=zip-6` | **53 MB** | 36.9 MB | 33.7 MB |
| the same plus `--compress=zip-9` | 53 MB | — | — |
| `zip-6` plus `--generate-cds-archive` | 84 MB | — | — |
| `zip-6` without `java.desktop` | 44 MB | — | — |
| `java.base` only (does not start) | 38 MB | — | — |

- On Linux, `--strip-debug` also strips native debug symbols through `objcopy`. That is what
  takes the image from 597 MB to 84 MB. `--strip-native-commands` would remove `bin/java`, so
  it cannot be used. [run: `jlink --list-plugins`]
- **`--compress` works against the package.** deb and rpm compress with gzip by default, and an
  AppImage is a compressed squashfs. Zip inside `lib/modules` makes the download 26 % bigger
  under gzip and 69 % bigger under xz, and saves only installed disk space. First paint did not
  measurably change (0.82 s uncompressed, 0.85 s compressed). [run;
  src: `crates/tauri-bundler/src/bundle/linux/debian.rs:383-391`, `rpm.rs:63-73`]
- **The linked image has no default CDS archive**, but the full JDK has one
  (`lib/server/classes*.jsa`). `--generate-cds-archive` adds 29 MB and brings first paint from
  0.85 s to 0.77 s, which is what the full JDK takes. [run]
- The jar adds 15.6 MB (14.5 MB under xz). Dropping the other platforms' natives at shade time
  would save about 4.6 MB of it. [run]

**Which JDK to link from.** The JDK used decides the runtime's glibc floor and the system
libraries it needs: [run, doc]

| | Arch `jdk-openjdk` 25.0.5 (run) | Temurin 25.0.5+8 (inspected, not run) |
|---|---|---|
| glibc floor of `libjvm.so` / `libjava.so` | **2.38** | **2.17** |
| zlib, libjpeg, lcms2, freetype | system (`libz.so.1`, `libjpeg.so.8`, `liblcms2.so.2`, `libfreetype.so.6`, `libharfbuzz.so.0`) | bundled (`libfreetype.so` ships in `lib/`, `libzip.so` and `libjli.so` need no `libz`) |
| `jmods/` | shipped; "Linking from run-time image disabled" | **not shipped**. Temurin enables JEP 493 since 24.0.0+36 and links from the run-time image |

glibc 2.38 is above Ubuntu 22.04 (2.35) and Debian 12 (2.36), which are the bases Tauri's docs
suggest. JEP 493 linking cannot put `jdk.jlink` in the image (not needed here). It refuses a
JDK whose configuration files were edited, and it cannot cross-link. Running the downloaded
Temurin was not allowed in this session, so its image size and start-up are not measured.
[doc: JEP 493, Adoptium "Eclipse Temurin JDK 24 enables JEP 493"]

## `externalBin`, or a runtime under `bundle.resources`

**What the bundlers do with `externalBin`.** deb and rpm copy each one to `/usr/bin/<name>`
with the `-<target triple>` suffix stripped. That puts it on the user's `PATH`. The AppImage
starts from the deb's data directory, so it gets the same file under `$APPDIR/usr/bin/`. Nothing
checks that the file is ELF. The docs say only that "Binaries are executables written in any
programming language". [src: `debian.rs:118-131`, `bundle/settings.rs:1204-1220`, `rpm.rs:147-158`,
`appimage/linuxdeploy.rs:80-113`; doc: develop/sidecar]

So a launcher script can be an `externalBin`. linuxdeploy's `listExecutables()` skips files in
`usr/bin` that are not ELF, so a script is not patched. tauri#11898 (open, ELF sidecars get
their rpath rewritten) and tauri#5189 (open, a `pkg`-built Node sidecar breaks only in the
AppImage) are about ELF files. Not tried. [src: linuxdeploy `src/core/appdir.cpp:793-809`; gh]

**But nothing needs `externalBin`.** It exists for the shell plugin's `sidecar()`, which takes
the file name and needs a `shell:allow-spawn` capability. The LAZY-36 relay spawned its sidecar
with `std::process::Command`. ADR 0057's relay spawns and owns the child itself. It can run
`<resource_dir>/runtime/bin/java` with the `-D` flags above, `-jar
<resource_dir>/lazyclaude.jar --sidecar`. That needs no script, no shell plugin and no
capability. [doc: develop/sidecar; code: branch `prototype/LAZY-36-pty-over-a-sidecar`,
`desktop/src-tauri/src/lib.rs`]

**How a directory ships under `bundle.resources`.**

- A directory is copied whole, with its structure. A map (`"../../target/runtime/": "runtime/"`)
  chooses where it lands. [doc: develop/resources]
- Executable bits survive in all three formats. Each file goes through `std::fs::copy`, which
  copies permission bits. deb's data tar uses `HeaderMode::Deterministic`, which keeps 0755 for
  any executable file and 0644 for the rest. The `rpm` crate's `with_file` takes the file's
  own mode when no default is set. [src: `utils/fs_utils.rs:81-96`, `debian.rs:364-366`;
  `rpm-0.30.0/src/rpm/builder.rs:550-562`]
- jlink's image holds no symlinks or empty directories, so the copy, which follows files only,
  loses nothing. [run: `find lz-rt -type l`, `-empty`]
- Resources land in `/usr/lib/<productName>/` for deb and rpm, and in
  `$APPDIR/usr/lib/<productName>/` for an AppImage. [src: `debian.rs:327-331`, `rpm.rs:181-191`]

**How the relay finds them.** `app.path().resource_dir()` (or `resolve(…,
BaseDirectory::Resource)`) tries `<exe dir>/../lib/<productName>` first, then
`$APPDIR/usr/lib/<productName>`, then `/usr/lib/<productName>`. Under `target/<profile>/`
with a `.cargo-lock` beside the executable, it returns the executable's own directory. That
covers dev builds. [src: `tauri-utils-2.10.2/src/platform.rs:289-334`]

**A cost in development.** `tauri-build` copies every resource next to the dev binary on each
build script run. A missing resource path fails the build (`ResourcePathNotFound`). Listing the
runtime in `tauri.conf.json`, or in `tauri.linux.conf.json` (always merged on Linux), therefore
makes every `cargo clippy` and `cargo test` in `make frontend` need an 84 MB runtime and the jar
built first, and copy them. A file passed as `tauri build --config <file>` is merged only for
that build, through `TAURI_CONFIG`, which `tauri-build` watches. That keeps dev and lint builds
free of it. ADR 0057's dev mode runs the `make` jar on the system `java` anyway. [src:
`tauri-build-2.7.4/src/lib.rs:124-186, 655, 735-761`, `tauri-utils-2.10.2/src/resources.rs:207`,
`tauri-utils/src/config/parse.rs:56`, `tauri-cli/src/build.rs:50-54`]

## Bundle formats on Linux and what they need of the system

Tauri 2 builds `deb`, `rpm` and `appimage` on Linux. The docs also describe Flatpak, Snap and
the AUR, but Tauri's CLI does not build those. [doc: distribute]

| | deb | rpm | AppImage |
|---|---|---|---|
| Declared dependencies | `Depends: libwebkit2gtk-4.1-0, libgtk-3-0` plus `bundle.linux.deb.depends` | `Requires: libwebkit2gtk-4.1.so.0()(64bit), libgtk-3.so.0()(64bit)` plus `rpm.depends` | none; WebKitGTK, GTK and their libraries are bundled by linuxdeploy |
| glibc | not declared; whatever the build host's `libc` gives the Tauri binary | same | same, and the bundled libraries come from the build host |
| Compression | gzip, default level, not configurable | gzip 6 by default; `rpm.compression` takes zstd, xz, bzip2 | squashfs |
| Runtime under | `/usr/lib/lazyclaude/` | `/usr/lib/lazyclaude/` | `$APPDIR/usr/lib/lazyclaude/`, read-only mount |

[src: `tauri-cli/src/interface/rust.rs:1422-1433`, `debian.rs:204-206, 383-391`, `rpm.rs:63-93`;
doc: distribute/debian, distribute/rpm, distribute/appimage]

- **glibc.** All three pages say to "build your Tauri application using the oldest base system
  you intend to support", for example Ubuntu 22.04 or Debian 12. Neither deb nor rpm declares a
  glibc version, so a too-new build fails only at run time
  (`version 'GLIBC_2.33' not found`). The jlink runtime has its own floor (above). [doc]
- **Nothing declares the runtime's own needs.** A Temurin-linked image needs only glibc, plus
  X11/ALSA libraries if `java.desktop`'s native code is ever loaded. It is not on the paths
  tried. An Arch-linked image also needs system zlib, libjpeg, lcms2, freetype and harfbuzz.
  [run]

## AppImage and WebKitGTK

- **linuxdeploy touches the runtime.** It walks `usr/lib` recursively. For every ELF file there
  (every `.so` in the runtime, and `bin/java`, `jspawnhelper` and `jexec`), it deploys that
  file's dependencies into the AppImage and rewrites its rpath, adding `$ORIGIN` where it is
  missing (`libjvm.so` has none). The JDK's own libraries already carry `$ORIGIN` RPATHs. A
  `java.desktop` library whose dependency is missing on the build host (`libasound.so.2` for
  `libjsound.so`, the X11 libraries for `libawt_xawt.so`) can fail the bundling. This is the
  mechanism behind tauri#11898 (open): an ELF sidecar cannot be excluded. Not tried with a
  runtime. [src: linuxdeploy `appdir.cpp:811-860`; gh]
- **WebKit on newer Mesa.** tauri#15976 and tauri#15665: a default AppImage built on Ubuntu
  22.04 or 24.04 bundled `libwayland-client` and the xcb/xkbcommon libraries. On Fedora 44 and
  Ubuntu 26.04 (Mesa 25/26, Wayland), WebKitWebProcess aborted with
  `Could not create default EGL display: EGL_BAD_PARAMETER` and no window opened. Both were
  closed 2026-09-25 by tauri#16062, which updated linuxdeploy and its GTK plugin.
  tauri-bundler 2.10.0's changelog: "fixing `EGL_BAD_PARAMETER` crashes on newer Mesa and
  bundling on Fedora and Arch. AppImages no longer force `GDK_BACKEND=x11`". The author's own
  note on the PR: "So far i only tested building and running on arch. I assume this _will_ cause
  issues". [gh; src: `crates/tauri-bundler/CHANGELOG.md`]
- **`GDK_BACKEND=x11`** was forced in AppImages until then, so an AppImage ran under XWayland
  (tauri#15781, #15902). Since 2.10.0 it uses native Wayland, the same as the deb. The line in
  the GTK hook is now commented out "Monitor this closely. AppImage used to crash on Wayland!".
  [src: `appimage/linuxdeploy-plugin-gtk.sh:246`; gh]
- **GStreamer.** AppRun exported a plugin path that does not exist without
  `bundleMediaFramework`. Media fails, but the maintainer judged it harmless for an app that
  plays none (tauri#15665). lazyclaude plays none. [gh]
- **FUSE 2.** tauri#7155 (open): AppImages do not start on Ubuntu 23.04+ without `libfuse2`,
  or without `--appimage-extract-and-run`. [gh]
- **Other open AppImage issues**: tauri#6200 (scroll lag only in the AppImage) and tauri#11023
  (cursor theme reset). Bundling "really only works on debian/ubuntu" (maintainer, tauri#15665).
  linuxdeploy cannot cross-build for ARM (doc). [gh; doc: distribute/appimage]

## `WEBKIT_DISABLE_DMABUF_RENDERER=1`

- **No bundle format sets it.** Neither the deb/rpm `.desktop` file nor the AppImage's GTK hook
  mentions it. [src: `appimage/linuxdeploy-plugin-gtk.sh`; doc: distribute/*]
- **Tauri's own advice** puts it in `main()` behind `#[cfg(target_os = "linux")]`, before
  `tauri::Builder`. It is step 3 of 4: kernel mode setting, then `__NV_DISABLE_EXPLICIT_SYNC=1`
  ("often fixes the Wayland `Error 71` crash without a performance cost"), then this variable,
  then `WEBKIT_DISABLE_COMPOSITING_MODE=1`. The page warns: "Only ship an unconditional override
  like this if you have verified your app is affected … It disables a faster path for
  everyone". [doc: develop/debug/linux-graphics]
- **In edition 2024, `std::env::set_var` is `unsafe`**. Both LAZY-36's prototype and the
  `tauri-learning` sandbox use edition 2024. The docs' snippet does not compile there as
  written. It is sound only before any other thread starts, which is the first line of `main`.
  Setting it only when unset keeps a user's own value. [doc: Rust edition guide,
  "Newly unsafe functions"; code: prototype `Cargo.toml`]
- A `.desktop`-file `Exec=env …` would cover only launches from the menu, not `lazyclaude` run
  from a shell or an AppImage started directly. `main()` covers all three. The variable is also
  inherited by the sidecar and every hosted `claude`, where it does nothing. [doc]
- `__NV_DISABLE_EXPLICIT_SYNC=1` was not tried in LAZY-36. It may fix the same machine without
  the performance cost.

## Answer

- **Runtime**: `jlink --add-modules java.base,java.desktop,java.management,java.sql,jdk.jfr
  --strip-debug --no-header-files --no-man-pages`, without `--compress`, gives 84 MB on disk
  and 29 MB gzipped. Link it from Temurin 25 (glibc 2.17, its own zlib/freetype, JEP 493 so no
  jmods), not from the developer's Arch JDK (glibc 2.38, system libraries). `java.xml` is
  needed even though jdeps never names it, and `java.desktop` stays until a hosted session is
  shown to work without it. The jar starts on the image. `--generate-cds-archive` buys 80 ms
  for 29 MB.
- **pty4j**: ship `libpty.so` and `libjnidispatch.so` pre-extracted beside the runtime, and
  pass `-Dpty4j.preferred.native.folder`, `-Djna.boot.library.path` and `-Djna.nounpack=true`.
  This also stops the `/tmp/pty4j-*` directory left behind on every run, which affects the TUI
  today.
- **Tauri**: ship the runtime and the jar under `bundle.resources`, through a `--config` file
  merged only by `tauri build`. Have the relay spawn `resource_dir()/runtime/bin/java` with
  `std::process::Command`. No `externalBin`, no shell plugin, no launcher script. Executable
  bits survive in deb, rpm and AppImage.
- **Formats**: deb and rpm depend only on WebKitGTK 4.1 and GTK 3 and leave the glibc floor to
  the build host. Build on Ubuntu 22.04 or Debian 12. The AppImage carries its own WebKitGTK and
  needs FUSE 2. Its Mesa/EGL crash was fixed only in tauri-bundler 2.10.0 and is barely
  exercised yet. linuxdeploy rewrites the runtime's ELF files. That is untried and the riskiest
  of the three formats.
- **DMA-BUF**: the relay's `main` sets `WEBKIT_DISABLE_DMABUF_RENDERER=1` in an `unsafe` block
  when it is unset, on Linux, before `tauri::Builder`. No bundle sets it for us.
- Also seen: the pty4j probe's `stty size` printed `0 0` despite `setInitialColumns(100)` and
  `setInitialRows(30)`. Whether `HostedPty` resizes right after spawning was not checked.
