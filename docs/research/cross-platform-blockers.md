# What stands between lazyclaude and macOS or Windows

Researched 2026-10-26 against TamboUI **0.5.0** (and the **0.6.0** release notes), JLine
**3.25.1** (the version TamboUI's JLine backend pins), pty4j **0.14.0**, jediterm-core **3.76**,
JDK **25**, Claude Code **2.1.310**, and Tauri **v2**. Everything was read on Linux. Nothing here
was run on a Mac or on Windows, so every macOS or Windows behaviour below comes from a source,
not from a test.

Sources, in order of trust:

- **code**: this repository's `src/main`, cited as `file:line` under
  `src/main/java/dev/maxenceng/lazyclaude/`.
- **jar**: the `-sources.jar` and binary jars in the local Maven repository (pty4j, JLine,
  TamboUI's JLine backend).
- **jdk**: OpenJDK source at tag `jdk-25+36` (`github.com/openjdk/jdk`).
- **doc**: first-party documentation: `code.claude.com/docs/en/` (`setup`, `claude-directory`),
  `v2.tauri.app` (`develop/sidecar`, `develop/resources`, `plugin/notification`),
  `openjdk.org/jeps/493`, Microsoft Learn (`CreateProcessW`), Apple's Mac Automation Scripting
  Guide, and TamboUI's `backends.adoc`.
- **gh**: upstream issue trackers and releases: `tamboui/tamboui`, `jline/jline3`,
  `JetBrains/pty4j`, `tauri-apps/tauri`, `tauri-apps/plugins-workspace` (the notification
  plugin's source), `cli/cli`, plus the Adoptium API for which JDK 25 builds exist.
- **obs**: strings from the installed Linux build of Claude Code 2.1.310
  (`/opt/claude-code/bin/claude`). This build includes the code paths for other platforms, but
  what it shows can change in any release.

Classification: **free** means it works as it is, or with a configuration flag. **Work** means
code has to change, but the path is known. **Blocked** means no known path exists, or the path
depends on an upstream fix that has not shipped.

## Summary

| Piece | macOS | Windows | Why |
|---|---|---|---|
| TamboUI + JLine (the TUI) | **work** | **work, upstream-dependent** | macOS: TamboUI's JLine 3.25.1 can hang on quit, and the Panama backend avoids it. Windows: arrow keys and Shift+Tab/Delete issues are still open upstream |
| pty4j | **free** | **work** (small) | universal `libpty.dylib` is bundled. ConPTY is opt-in through `setUseWinConPty(true)`, and winpty is the default |
| jediterm-core | **free** | **free** | pure Java, and IntelliJ runs it on every OS |
| `claude` binary and `~/.claude` paths | **free** | **work** (small) | `user.home` resolves `~/.claude` everywhere. On Windows, `claude` has to resolve to an `.exe` |
| Registry liveness (`/proc`, `/etc/machine-id`) | **work** | **work** | Linux-only today. Claude Code writes other `pidDomain`/`procStart` shapes on other platforms |
| Transcript folder names | **free** | **free** | the documented rule (non-alphanumerics → `-`) is what the code already applies |
| `gh` | **free** | **free** | official builds for macOS and Windows on amd64 and arm64 |
| git worktrees | **free** | **free** (untested) | plain `git` subcommands, run through `ProcessBuilder` |
| `WatchService` | **free**, slower | **free** | macOS polls every 2 s. Windows is native |
| Desktop notifications | **work** (show only), **blocked** (close/replace) without native code | same | D-Bus and `notify-send` exist only on Linux. Tauri's plugin cannot remove a shown notification on macOS or Windows |
| `config.yml` and cache under XDG paths | **free** (non-native) | **free** (non-native) | falls back to `~/.config` and `~/.cache` under `user.home`. Native locations are a choice, not a need |
| jlink runtime per OS/arch | **work** | **work** | per-platform build, with no cross-linking from a linkable runtime. No Temurin 25 build for Windows on arm64 |
| Tauri sidecar | **work** (signing, notarization) | **work** (installer quirks) | `externalBin` takes one executable per target triple, but a jlink image is a directory |

Nothing is **blocked** outright except closing or replacing a desktop notification on macOS or
Windows through Tauri's notification plugin. That needs native code of our own, or it gets
dropped. The riskiest piece is the TUI on Windows. It depends on TamboUI fixing open input
issues, and the desktop frontend sidesteps that because it renders through xterm.js and not
through TamboUI.

## Linux-specific assumptions in `src/main`

Paths are relative to `src/main/java/dev/maxenceng/lazyclaude/`. Kitty and tmux are left out
because they are being removed. That covers `KittyConfigRepository`, `TerminalJumpRepository`,
`RemoteControlPromptText`, and `ProcessEnvironmentRepository`, whose `/proc/<pid>/environ`
read (`ProcessEnvironmentRepository.java:20`) only feeds the kitty/tmux jump and residence
(`JumpManager`, `SessionManager.java:306`).

| Where | Assumption | Platforms it breaks | Class |
|---|---|---|---|
| `LazyClaude.java:89` | `PROC = Path.of("/proc")` | macOS, Windows | work |
| `LazyClaude.java:90` | `MACHINE_ID = Path.of("/etc/machine-id")` | macOS, Windows | work |
| `session/infrastructure/secondary/LivenessCheck.java:13-14,20-21,37-38` | the domain is `linux:<machine-id>:<readlink /proc/self/ns/pid>` | macOS, Windows (`thisMachinesDomain` throws `SessionsUnavailableException`, so the whole registry is unreadable) | work |
| `LivenessCheck.java:54-55,58-66` | liveness is `/proc/<pid>` existing, plus field 22 of `/proc/<pid>/stat` | macOS, Windows | work |
| `session/infrastructure/secondary/SessionRegistryRepository.java:44,75` | calls the two checks above | macOS, Windows | work |
| `LazyClaude.java:94-95`, `session/infrastructure/secondary/NotifySendRepository.java:20-21,30-38` | `notify-send -p`/`-r`, and `gdbus call --session … org.freedesktop.Notifications.CloseNotification` | macOS, Windows (the commands are absent, and each call fails as `DesktopNotificationUnavailableException`) | work / blocked (see below) |
| `LazyClaude.java:85-86,223-230` | `$XDG_CACHE_HOME`, else `~/.cache` | none: works everywhere, but not native | free |
| `session/infrastructure/secondary/ConfigurationRepository.java:16,22-23,55-60` | `$XDG_CONFIG_HOME/lazyclaude/config.yml`, else `~/.config/lazyclaude/config.yml` | none: works everywhere, but not native | free |
| `LazyClaude.java:83-84`, `session/infrastructure/secondary/ClaudeKeybindingsRepository.java:32,52` | `~/.claude` is fixed, and `CLAUDE_CONFIG_DIR` is not honoured | every OS, when the user sets `CLAUDE_CONFIG_DIR` (not platform-specific) | work |
| `LazyClaude.java:91-92` | argv `claude`, and `claude agents --json` | Windows, when only a `claude.cmd` shim is on `PATH` | work (small) |
| `session/infrastructure/secondary/HostedPty.java:76-80` | `PtyProcessBuilder` without `setUseWinConPty(true)` | Windows (falls back to winpty) | work (one line) |
| `HostedPty.java:133,150-157` | resize is assumed to reach the child as `SIGWINCH`, and `destroy()` is assumed to be a polite stop | Windows (resize is ConPTY's `ResizePseudoConsole`, which works. `destroy()` is `TerminateProcess`, so the stop is never polite) | free / note |
| `session/infrastructure/secondary/ScrubbedEnvironment.java:16,29-32` | sets `TERM=xterm-256color` and `COLORTERM=truecolor` | none: harmless on Windows | free |
| `shared/infrastructure/secondary/TreeWatcher.java:26,220` | the comments explain Linux's non-recursive watches, and Linux keeping a moved directory's watch | none: the per-directory approach is portable. macOS polls | free |
| `session/infrastructure/secondary/TranscriptReadRepository.java:32,57` | folder name is the cwd with `[^A-Za-z0-9]` replaced by `-` | none: this is Claude Code's documented rule, which turns `C:\x` into `C--x` | free |
| `docs/infrastructure/secondary/DocumentPathCollector.java:38`, `docs/infrastructure/secondary/DocsScope.java:40-42` | relative doc paths joined with `/` | none: already separator-neutral (`getSeparator()` / `Path` iteration) | free |
| `shell/ReviewText.java:61` | `/` between path segments | none: these are git paths, and git uses `/` on every OS | free |
| `shell/FocusReportingBackend.java:28-29` | the outer terminal honours `?1004` focus reporting | unverified on Terminal.app, conhost, and Windows Terminal. Without it, the code is a no-op | free / unverified |
| `Makefile:88` and `scripts/check-*` | POSIX shell and `make` | Windows development (not runtime). Needs WSL or Git Bash | out of scope |

## TamboUI with JLine: work on macOS, work on Windows (upstream-dependent)

- **What lazyclaude runs.** `Shell` builds its backend with `BackendFactory.create()`
  (`shell/Shell.java:524`), which loads `tamboui-jline3-backend` through `ServiceLoader`. That
  backend calls `TerminalBuilder.builder().system(true).jansi(true)` and handles
  `Signal.WINCH` (`JLineBackend.java:57,287`). It pins `org.jline:jline:3.25.1`, and so do
  TamboUI's v0.5.0 and v0.6.0 `libs.versions.toml`. The bundled jline jar carries the Jansi,
  JNA, and FFM terminal providers, including `impl/*/win` and `impl/*/osx` and native
  `org/jline/nativ/Windows/{x86,x86_64,arm64}`. JLine can therefore open a Windows console and a
  macOS tty. [jar; gh]
- **TamboUI's own platform statement.** `tamboui-jline3-backend` is "the reliable default on all
  platforms". `tamboui-panama-backend` (Java 22+, no third-party dependencies) ships
  `unix/UnixTerminal` with macOS constants in `PlatformConstants`, and
  `windows/WindowsTerminal` over `Kernel32`. Since 0.5.0, `-Dtamboui.backend=panama,jline3`
  means "try Panama, then fall back to JLine". [doc: `backends.adoc`; gh: v0.5.0 tree, release
  notes]
- **macOS: quitting can hang.** tamboui#354 (closed 2026-05-26): "When the TUI exits …
  `TuiRunner.close()` hangs indefinitely on macOS". The main thread blocks in
  `FileDescriptor.close0()` while JLine's reader thread sits in `read0()` on the same stdin fd.
  TamboUI closed the issue as JLine's to fix. The fix is jline3#1911, merged to `master` on
  2026-05-28 under milestone **4.1.3**. TamboUI 0.6.0 still pins JLine 3.25.1, and no 3.x
  backport turned up. Possible routes are the Panama backend (lazyclaude already runs on JDK 25),
  or waiting for TamboUI to move to JLine 4. Either way it needs a test on a Mac. **Work.** [gh]
- **Windows: input is not solid yet.**
  - tamboui#220 "Windows not fully supported" (**open**): with the demos, Up/Down print `A`/`B`.
    A maintainer also notes a stray `?2027;3$y` mode query. [gh]
  - tamboui#328 (**open**): Shift+Tab and Delete misread on cmd and PowerShell. Delete arrives as
    Ctrl+H. The maintainer expects the kitty keyboard protocol (tamboui#446) to help, but "a
    legacy fallback for these keys may still be needed". Shift+Tab matters to lazyclaude,
    because the hosted pane takes it back from the bindings. [gh]
  - Fixed upstream: Backspace sent as BS (8) and read as Ctrl+H (tamboui#302, closed
    2026-04-24), and arrow keys on the Windows Panama backend (tamboui#359, fixed in 0.4.0).
    [gh]
  - Verdict: **work, upstream-dependent**. The TUI on Windows is the least certain piece. A
    desktop frontend does not have this problem, because it draws the session in xterm.js.

## pty4j: free on macOS, a one-line change on Windows

- **Natives.** The 0.14.0 jar bundles `native/darwin/libpty.dylib` and
  `pty4j-unix-spawn-helper`, both Mach-O universal (x86_64 + arm64). It also bundles
  `native/win/{x86-64,aarch64}/{conpty.dll,OpenConsole.exe,winpty.dll,winpty-agent.exe,win-helper.dll}`
  and `native/win/x86/winpty*`. pty4j ships its own `conpty.dll` and `OpenConsole.exe`, so it
  does not depend on the console host the user's Windows build happens to ship. [jar;
  `util/ExtractedNative.java`, `util/PtyUtil.java:47-60`]
- **ConPTY is opt-in.** `PtyProcessBuilder.start()` on Windows picks Cygwin if asked, then
  `WinConPtyProcess` only `if (myUseWinConPty && !myConsole)` (`myUseWinConPty = false` by
  default), and otherwise **winpty**. `HostedPty.java:76-80` never calls `setUseWinConPty(true)`.
  The fix is one builder call. A missing `conpty.dll` already falls back to winpty, logged at
  info level. [jar: `PtyProcessBuilder.java:33,115,180-193`]
- **Spawning `claude`.** `WinConPtyProcess` calls `CreateProcessW(null, commandLine, …)`. With a
  NULL application name, Windows appends `.exe` to an extension-less name and searches `PATH`,
  but "to run a batch file, you must start the command interpreter". The native installer and
  WinGet install `claude.exe`. An npm install leaves a `.cmd` shim on `PATH`. So the `claude`
  argv either has to resolve to a real `.exe` or be wrapped in `cmd.exe /c`. The JDK's own
  `ProcessBuilder`, which runs `claude agents --json`, `gh` and `git`, resolves names by the same
  rules (`ProcessImpl.java:357-390`). **Work (small).** [jar: `conpty/ProcessUtils.java:94-105`;
  doc: CreateProcessW; jdk; doc: setup]
- **Stopping.** `WinConPtyProcess.destroy()` is `TerminateProcess(h, 1)` (`:199-206`). There is
  no polite stop, and no job object, so `claude`'s own children (MCP servers, Bash) are not
  reaped by pty4j. Whether that matters depends on how a hosted session ends, which is open
  elsewhere. [jar]
- **JDK 25.** Native access is already granted (`--enable-native-access=ALL-UNNAMED`,
  `Makefile:88`). This is the same on every OS. [code]
- Open pty4j issues that touch Windows: #187 (cursor desync after kitty graphics plus resize, in
  PowerShell) and #102 (GUI children open behind other windows). Neither concerns `claude`.
  [gh]

## jediterm-core: free

- Pure Java with no natives. It only parses the byte stream (ADR 0026 keeps it behind the hosted
  session's adapter). It is the emulator of IntelliJ's terminal on all three OSes, fed by pty4j
  there too. ConPTY re-encodes the child's output as VT sequences, which jediterm already
  parses. [jar; gh: pty4j README "Linux, OSX, Windows and FreeBSD"]
- A desktop frontend replaces it with xterm.js anyway, so it matters only for the TUI.

## `claude`: its binary, registry and transcripts per OS

- **Platforms.** macOS 13.0+, Windows 10 1809+ / Server 2019+, and Linux. x64 or ARM64. npm
  builds cover `darwin-arm64`, `darwin-x64`, `win32-x64` and `win32-arm64`. Native Windows needs
  no WSL. Git for Windows is optional and provides Git Bash for the Bash tool. [doc: setup]
- **Binary location.** On macOS the binary is at `~/.local/bin/claude` (native installer),
  installed by the Homebrew cask, or at the npm prefix. On Windows it is
  `%USERPROFILE%\.local\bin\claude.exe` (native) or installed by WinGet (`Anthropic.ClaudeCode`).
  lazyclaude finds it on `PATH`, which is **free** except for the `.cmd` case above. [doc:
  setup]
- **`~/.claude`.** "On Windows, `~/.claude` resolves to `%USERPROFILE%\.claude`. If you set
  `CLAUDE_CONFIG_DIR`, every `~/.claude` path on this page lives under that directory instead."
  Java's `user.home` is that same directory, so `home.resolve(".claude/sessions")` works on every
  OS: Windows NIO accepts `/` in a path string. `CLAUDE_CONFIG_DIR` is the real gap, and it is
  not platform-specific (`LazyClaude.java:83-84`). [doc: claude-directory]
- **Transcripts.** `projects/<project>/<session>.jsonl` on every OS. The docs give the folder rule
  for the scratchpad: "your working directory path with every character other than letters and
  digits replaced by `-`". That is the rule `TranscriptReadRepository.java:32,57` already applies.
  **Free.** [doc: claude-directory]
- **Registry.** `sessions/` ("one small file per running session") is documented without a
  platform caveat. The record's identity fields differ by platform. [doc; obs]
  - `pidDomain` is computed by one function. Linux and WSL give
    `linux:${/etc/machine-id}:${readlink /proc/self/ns/pid}`, which is what `LivenessCheck`
    rebuilds. Windows gives `` `linux:${i().toLowerCase()}` ``, where `i()` is not resolved in
    this build. Any other platform gives the bare string `"linux"`. [obs: `async function tii(n)`]
  - `procStart` on Linux is read from `/proc/<pid>/stat`. Elsewhere it is the output of
    `ps -o lstart= -p <pid>` under `LC_ALL=C TZ=UTC`. On Windows the record carries
    `procStartFt` instead of `procStart` (`T0`/`ese` switch on the platform). [obs:
    `async function b(e,t)`, `function T0`]
  - So `LivenessCheck` needs a per-OS shape. One portable route is `ProcessHandle.of(pid)` for
    "is it alive" plus a per-OS start-time comparison. The documented alternative is
    `claude agents --json`, which lazyclaude already wires as the fallback (`LazyClaude.java:92`).
    **Work.** With hosted sessions only, lazyclaude holds each child's `Process`, so liveness of
    its own sessions needs no registry check at all. The registry is then read only for `status`
    and `waitingFor`, which are plain file reads.

## `gh` and git worktrees: free

- `gh` v2.103.3 ships `macOS_{amd64,arm64}.zip`, a `macOS_universal.pkg`, and
  `windows_{386,amd64,arm64}.{zip,msi}`. It is called as `gh.exe` through `ProcessBuilder`, which
  is fine. [gh: cli/cli release assets]
- Git worktrees are plain `git worktree list --porcelain | add | remove | prune` calls
  (`session/infrastructure/secondary/WorktreeRepository.java:24-56`), and Git for Windows ships
  `git.exe` (v2.56.0.windows.4). One untested Windows detail: the porcelain output prints paths
  with forward slashes (`C:/…`), which `Path.of` accepts on Windows. **Free, untested.** [gh;
  code]

## `WatchService`: free, slower on macOS

- **macOS.** `BsdFileSystem.newWatchService()` returns `new PollingWatchService()` ("use polling
  implementation until we implement a BSD/kqueue one"), with `POLLING_INTERVAL = 2` seconds. An
  FSEvents implementation (JDK-8293067, from JetBrains) was withdrawn in February 2023 and is
  not in JDK 25 or `master`. Each poll lists every registered directory, so the board, docs,
  registry and transcript watchers react within about 2 s. They also cost one directory scan per
  watched directory per tick. The existing timer fallbacks (`WatchServiceState` / `NotWatching`)
  are not needed. **Free, with latency.** [jdk:
  `src/java.base/macosx/classes/sun/nio/fs/BsdFileSystem.java:52-57`,
  `share/classes/sun/nio/fs/PollingWatchService.java:63`; nio-dev list]
- **Windows.** `WindowsWatchService` uses `ReadDirectoryChangesW` and opens each directory with
  `FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE`, so watching does not lock a directory
  against deletion. It also accepts `ExtendedOptions.FILE_TREE` for a recursive watch, though
  lazyclaude's per-directory `TreeWatcher` does not need it. **Free.** [jdk:
  `windows/classes/sun/nio/fs/WindowsWatchService.java:44,340-362`]

## Desktop notifications: work to show, blocked to close without native code

lazyclaude's notification port (`DesktopNotificationPort`) shows, **replaces** and **closes** a
notification. `DesktopNotificationManager` closes one when a session leaves `waiting`, and
replaces it when the reason changes.

- **TUI on macOS.** No D-Bus. `osascript -e 'display notification …'` takes a title, subtitle
  and sound, and the guide documents no way to update or remove a shown notification. Showing is
  **work**. Replacing and closing are **blocked** without a signed app bundle using
  `UNUserNotificationCenter`. [doc: Mac Automation Scripting Guide, Displaying Notifications]
- **TUI on Windows.** No command-line notifier ships with Windows. A toast needs WinRT and an
  AppUserModelID. **Work.** Closing a toast needs the same native route.
- **Desktop frontend, through Tauri's notification plugin.** It lists Windows, Linux and macOS.
  On Windows it "Only works for installed apps". The desktop source settles closing and
  replacing: `remove_active` "**macOS and Windows**: does nothing, `notify-rust` cannot remove a
  shown notification there" (Linux and the BSDs do close). So the plugin can **show** a
  notification everywhere, but closing or replacing one on macOS or Windows is **blocked**
  unless we write native code (a Tauri command over `UNUserNotificationCenter` or WinRT toast
  history). The alternative is to accept that a stale notification stays up there. [doc:
  plugin/notification; gh: `plugins/notification/src/desktop.rs:225-231,435-485`]

## `config.yml` and the cache: free, not native

- Both resolve under `user.home` when the XDG variables are unset
  (`ConfigurationRepository.java:55-60`, `LazyClaude.java:223-230`). On macOS and Windows,
  `~/.config/lazyclaude/config.yml` and `~/.cache/lazyclaude/past-sessions.json` work as they
  are. Moving to `~/Library/Application Support` or `%APPDATA%`/`%LOCALAPPDATA%` is a choice for
  native feel, not a requirement. A choice like that is worth an ADR if it is made, because the
  config path is user-visible. **Free.** [code]
- The desktop frontend needs the same file read by the same Java backend, so nothing changes
  there.

## jlink runtimes per OS and architecture: work

- **No cross-linking from a linkable runtime.** JEP 493 (JDK 24) lets `jlink` work without JMOD
  files, but: "Cross-linking, e.g., running `jlink` on Linux/x64 to create a run-time image for
  Windows/x64, is not possible". Whether a vendor build enables `--enable-linkable-runtime` "is
  up to that vendor". The local JDK 25 still ships `jmods/`. Run `jlink` once per target, in a
  CI matrix of macOS arm64, macOS x64, Windows x64 and Linux x64/arm64. [doc: JEP 493]
- **Which JDK builds exist.** Temurin 25.0.5 is published for `mac/{aarch64,x64}`,
  `windows/x64` and `linux/{x64,aarch64,…}`. There is **no Temurin 25 for Windows on arm64**.
  That platform needs another vendor's build (Microsoft's OpenJDK, for example), or it is left
  out. [gh: Adoptium API `assets/latest/25/hotspot`]
- **What goes in the image.** The application is a shaded, non-modular jar, so `jlink` only
  builds the runtime (`jdeps --print-module-deps` decides the modules) and the jar runs on it.
  jediterm's two `java.awt` references mean `java.desktop` may be pulled in (see
  `embedding-claude-on-a-pty.md`). pty4j extracts its natives to a temp directory at runtime, or
  to `-Dpty4j.preferred.native.folder`. A packaged build should pre-extract them next to the
  runtime, so they can be signed (macOS, below). [jar]

## Tauri sidecar per OS: work

- **One executable per target triple.** `bundle.externalBin` takes paths, and "a file with the
  same name plus a `-$TARGET_TRIPLE` suffix must exist" (for example
  `my-sidecar-aarch64-apple-darwin` and `my-sidecar-x86_64-unknown-linux-gnu`, with `.exe`
  appended on Windows). The shell plugin spawns it with `shell:allow-spawn` and
  `"sidecar": true`, and talks to it over stdin/stdout through `CommandEvent::Stdout` and
  `child.write`. That fits the JSON Lines protocol. [doc: develop/sidecar]
- **A jlink image is a directory, not one file.** Two routes:
  - Ship the image through `bundle.resources` ("If the path is a directory, the entire
    directory will be copied to the `$RESOURCE` directory", with its structure kept), and spawn
    its launcher by the path from `path().resolve(…, BaseDirectory::Resource)`.
  - Make the `externalBin` a small native launcher that starts the bundled runtime.

  The resources page does not say whether executable bits survive bundling, so this needs a
  prototype per OS. Tauri has no per-arch resources yet (tauri#8501, open), so per-arch images
  mean per-arch builds. [doc: develop/resources; gh]
- **macOS signing and notarization.**
  - tauri#11992 (open): notarization fails once any `externalBin` is added.
  - tauri#16211 (closed as not planned, 2026-10-05): sidecars are signed with the **app's**
    entitlements.
  - Since tauri#11676, binaries in `resources` get signed too (tauri#13922, open, asks to
    control that).
  - A JVM under the hardened runtime needs the entitlements that jpackage's own macOS template
    grants: `com.apple.security.cs.allow-jit`, `allow-unsigned-executable-memory` and
    `disable-library-validation`. The last one also lets pty4j's JetBrains-built
    `libpty.dylib` load. [jdk: `jdk.jpackage/macosx/…/resources/sandbox.plist`]
  - Every Mach-O in the image, plus pty4j's pre-extracted dylib and spawn helper, has to be
    signed. **Work.** This is the costliest piece of the macOS port.
- **Windows.** tauri#15134 (open): an NSIS reinstall may silently keep a stale `externalBin`
  sidecar. tauri#9232 (open) is a sidecar that runs on macOS but not on Windows, for an R
  runtime. **Work**: pick MSI, or check the NSIS upgrade path.
- **Linux.** tauri#11898 (open): AppImage needs a way to exclude standalone sidecars. Not a
  blocker for `.deb`/`.rpm`.

## Answer

Most of lazyclaude is portable already. Path handling, the transcript folder rule, `gh`, git,
jediterm, the config and cache paths, and the watchers all work on macOS and Windows as they
are. macOS polls files every 2 s.

The Linux-only code is small and concentrated:

- **Registry liveness** (`LazyClaude.java:89-90`, `LivenessCheck`), which hosted-only sessions
  mostly make unnecessary.
- **D-Bus notifications** (`NotifySendRepository`).
- **Windows small fixes**: one missing pty4j flag (`setUseWinConPty(true)` in `HostedPty`) and
  `.exe` resolution for `claude`.

The real costs are outside lazyclaude's code:

- **Upstream input bugs in TamboUI on Windows** (open #220 and #328), and a JLine 3.25.1
  quit hang on macOS (fixed only in JLine 4). Together these make the TUI the riskiest
  piece off Linux.
- **macOS signing and notarization** of a jlink runtime inside a Tauri bundle.
- **One build per OS and architecture.** There is no Temurin 25 for Windows on arm64.
- **Closing or replacing a notification** on macOS and Windows. Tauri's plugin cannot do it,
  so it is the one thing blocked without our own native code.
