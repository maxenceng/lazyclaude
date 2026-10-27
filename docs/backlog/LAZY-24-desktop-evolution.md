---
id: LAZY-24
status: done
context: cross-context
type: map
created: 2026-10-26
---

# The desktop evolution: mouse, GitHub Issues, MkDocs, a sidecar backend and a Tauri frontend

## Destination

Every decision the desktop evolution depends on is settled, so each phase can be written as
refined `todo` feature tickets ready for `/ticket start`, and every reversal of the v1 charter
has its superseding ADR and its CLAUDE.md edit.

The evolution, as scoped on 2026-10-26 from the second roadmap (`~/projects/plan.md`):

1. **Hosted sessions only** — lazyclaude handles only the sessions it starts; kitty, tmux,
   outside sessions and the Elsewhere panel go. Built first: nothing else should be ported,
   given mouse support or documented before it is cut.
2. **Mouse in the TUI** — every primary navigation and selection workflow by mouse, keys
   unchanged (ADR 0008), the hosted pty never intercepted.
3. **GitHub Issues as the tracker** — for the whole claude-tools harness, not only lazyclaude:
   `/ticket`, `/wayfinder` and `/debt` read and write Issues, Vikunja is dropped, and
   `docs/backlog/` is migrated and archived. lazyclaude becomes writable on Issues.
4. **MkDocs for every other doc** — ADRs, glossary, context map, research and generated
   endpoint maps, in lazyclaude and in the projects it opens. lazyclaude keeps reading the
   markdown sources, never the built site.
5. **A sidecar backend** — the Java backend driven over JSON Lines on stdin/stdout, hosted
   ptys included; the TUI and the desktop frontend share one application layer.
6. **jlink packaging and a Tauri frontend** — React + TypeScript + xterm.js, the Java sidecar
   bundled with a minimal runtime. The TUI stays first-class alongside it.

## Notes

- Map and decision tickets carry `LAZY-n` and `context: cross-context`; the feature tickets
  they lead to take a context prefix and `docs/glossary.md`'s words.
- Grilling uses `mattpocock-skills:grilling` with `ddd-workflow:domain-modeling`; research
  uses `mattpocock-skills:research`, findings under `docs/research/`; prototypes use
  `mattpocock-skills:prototype`. `~/projects/tauri-learning` is an existing Tauri sandbox.
- The harness lives in `~/projects/claude-tools`; changing it is in scope for phase 3.
- Facts gathered while charting (2026-10-26, TamboUI 0.5.0, Claude Code 2.1.310):
  - the architecture rules already keep TamboUI out of the domain and application layers,
    and the shell reaches contexts only through `*Query`/`*Command` — a sidecar is one more
    primary adapter, not a decoupling refactor;
  - the hosted session's one adapter holds both pty4j and jediterm (ADR 0026); a desktop
    frontend emulates the terminal in xterm.js, so the raw pty stream has to be reachable
    without jediterm;
  - TamboUI's mouse reporting is off by default (LAZY-14);
  - lazyclaude itself has no endpoints, so "endpoint maps" means those of the projects it
    opens.
- Standing preferences: cross-platform if possible, Linux only if that is painful for now;
  risky choices are prototyped before the phase that depends on them.
- The spring-ddd template's share of the docs build (DOCS-2, DOCS-3, and `/trace-endpoint`'s index name)
  is a ticket in claude-tools' own backlog, not here.
- This map is charted in `docs/backlog/` and migrates to Issues with everything else in
  phase 3 — it is that migration's first test case.

## Decisions so far

- [What is the state of MkDocs in 2026?](LAZY-34-mkdocs-ecosystem.md): start on Zensical (Material's successor, 0.1.0 on 2026-11-05) with a `mkdocs.yml` both tools read, version pinned; MkDocs 1.x stalled and Material ends 2027-05-05; mermaid, `--strict` link checks and Pages deploys built in; ADRs get a hand-kept index; generated pages committed to their own folder; `mkdocs.yml`'s nav is no source for the docs tree order.
- [What can GitHub Issues and `gh` carry of a ticket?](LAZY-29-github-issues-capabilities.md): title, body, open/closed, native `blocked_by` dependencies and sub-issues (map → decisions) on a personal repo; issue types/fields are org-only, so `type`, `context`, `status` are labels (or a Projects v2 field, needing the `project` scope); branch and PR links readable back; no conditional write, only an `updatedAt` check before writing; ETag polling is free; no server-less push; `createdAt` not backdatable.
- [What mouse support do TamboUI and the hosted pty offer?](LAZY-27-tamboui-mouse-support.md): SGR press/release/drag/move/wheel with modifiers, no double-click; only left press, move and wheel reach elements and a click never selects a row, so the shell hit-tests against its own geometry; `claude` turns on 1000/1002/1003 SGR and handles its own clicks, so a pane forwards press/drag/release in order; existing gaps: no `mouseMotion` (no hover in `claude`), a drag leaving the pane loses its release; risks: X10 terminals type a click as keys, Windows mouse likely absent.
- [What stands between lazyclaude and macOS or Windows?](LAZY-39-cross-platform-blockers.md): our Linux-only code is small (`/proc` liveness, `notify-send`/`gdbus`, ConPTY opt-in, `claude.exe` resolution); the cost is TamboUI's Windows input bugs, JLine 3's quit hang on macOS, signing and notarizing a bundled runtime, one jlink runtime per OS and arch; only closing a notification on macOS/Windows via Tauri is blocked; all from sources, nothing run off Linux.
- [What does `session` keep when only the sessions lazyclaude started count?](LAZY-25-what-session-keeps.md): outside sessions, Elsewhere, kitty, tmux and the transcript view go; `--session-id` names a session at launch and `HostedSessionId` merges into `SessionId`; state from the hosted pid's registry record, no liveness check or `claude agents` fallback; a launch log under `$XDG_STATE_HOME` lists past sessions, pruned when the transcript goes; links known at launch then moved by `/ticket` args, no branch link; notifications hosted-only (ADR 0047).
- [Does a hosted session outlive the process that hosts it?](LAZY-26-sessions-outliving-lazyclaude.md): no — the sidecar is its frontend's child and quitting ends its sessions (ADR 0048), so the protocol has no attach or reattach; the launch log records an end only when `claude` exits by itself, so a session cut off by a quit or crash is *interrupted*, listed first and resumed by hand; closing the desktop window is `q`, no tray.
- [Can a hosted `claude` run smoothly through a JSON Lines sidecar into xterm.js in a Tauri window?](LAZY-36-pty-over-a-sidecar.md): yes — typing, paste, Ctrl+C, resize, mouse and a 119 MB stream all fine; the sidecar costs ~30 % over a raw pty, base64 is the encoding (137 ms per 119 MB), the relay batches lines into a Tauri `Channel`, stdin EOF is the shutdown and leaves no `claude`; WebKitGTK needs `WEBKIT_DISABLE_DMABUF_RENDERER=1` on NVIDIA Wayland, and a Hyprland focus loss under `tauri dev` is undiagnosed.
- [How does `session` expose a hosted pty to two frontends?](LAZY-37-hosted-pty-for-two-frontends.md): emulation belongs to the frontend — jediterm, input encoding and query replies in the TUI's primary adapter, xterm.js on the desktop; `HostPort` is a pty carrying `PtyBytes` both ways; the backend's domain owns lifetime; resize goes emulator then pty; an empty emulator is repainted by a one-row SIGWINCH nudge (ADR 0049).
- [What is the sidecar protocol?](LAZY-38-sidecar-protocol.md): JSON-RPC 2.0 over JSON Lines from `lazyclaude --sidecar`, a `SHELL`-kind `sidecar` module serializing the primary adapters' Views with Jackson, one method per operation; refusals are results, `error` only for faults; explicit watches push full Views; pty traffic as base64 notifications; `ready {protocol}` first, stdin EOF the only shutdown, no cancellation; schema generated and drift-checked; the TUI stays in-process (ADR 0050).
- [What does a mouse do in each part of the screen?](LAZY-28-mouse-interaction-rules.md): it focuses, selects, switches tabs, scrolls and drags a diff range, nothing else; a second click (on the row already selected, in the focused pane) is `enter`; the wheel acts under the pointer and focuses; a focusing click on a hosted pane is never forwarded, a drag keeps its pane and its release, motion only to the focused pane that asked; popups modal; key bar, status line and links inert; a per-frame hit map; `mouse: false`, default under `TERM=linux`, for X10 (ADR 0051).
- [How does a ticket map onto a GitHub issue?](LAZY-30-ticket-model-on-issues.md): the issue number is the id and `gh issue develop` names the branch; status is a `status:*` label on an open issue and `done` is closed as completed by the merge's closing keyword, so the branch copy and ADR 0028 go; `context:`/`type:` labels, native dependencies, sub-issues for a map; a bare issue is an unassigned `draft`, missing labels from `todo` on are unreadable; no frontmatter in the body; no GitHub remote, no backlog; *ticket* stays the word (ADR 0052).
- [How do `/ticket`, `/wayfinder` and `/debt` work on GitHub Issues?](LAZY-31-harness-on-issues.md): one stdlib Python `ticket-tracker` script over `gh` behind every step; a section write refuses if `updatedAt` moved; labels created as applied, context only from the map; issue templates for capture; `done` after merge, closing a missed keyword itself; frontier from sub-issues, label claims; nothing offline, the build never online; `ticket-check` bans every `#n` outside the record; one cut-over in plugin 3.0.0 (ADR 0053).
- [What does lazyclaude write to GitHub Issues, and how does it avoid overwriting a remote change?](LAZY-32-lazyclaude-writes-to-issues.md): only capture — `c` on the Board creates a label-less issue, a `draft`, with the template body; every other write goes through a `/ticket` verb, so the `updatedAt` check stays in `ticket-tracker`; a failed create keeps its popup, never retried by itself; the board reads through `gh` on a 30 s ETag poll plus `R`, uncached, the board kept under a warning on a failed read, selection following the issue number (ADR 0054).
- [How is every project's `docs/backlog/` migrated to Issues?](LAZY-33-migrating-the-backlog.md): every ticket, done ones closed as completed, by one resumable stdlib script in claude-tools from a 2.x project's `main` with no ticket branch open; `Migrated from <id>, created <date>.` closes each body; two passes rewrite ids to `#n`; decisions become sub-issues of their map, edges onto a map dropped; `verify` catches a lost or doubled ticket; `docs/backlog/` deleted under a `backlog-archive` tag; `context:cross-context` accepted everywhere; next-suggestions first, lazyclaude after its board reads Issues (ADR 0055).
- [Which platforms does this evolution support?](LAZY-40-which-platforms.md): Linux x86_64 only, both frontends, every phase; macOS, Windows and aarch64 a later discussion; each Linux-only spot carries a `// deferred: Linux only` comment, no CI or check guard; equal cost takes Tauri's cross-platform way; one jlink runtime, one target triple (ADR 0056).
- [How are a project's docs laid out and built on MkDocs?](LAZY-35-docs-on-mkdocs.md): one tree, `docs_dir: docs` with backlog excluded and no `nav:`, so the panel takes the site's order (`README.md` first, files, then folders); `README.md` index pages; the ADR index written by `make adr-index` and checked stale by `adr-check`; endpoint maps committed as `/trace-endpoint` writes them, no staleness check; `uvx zensical==<pin>`, `make docs` strict in `verify`; Pages deploy gated on `DOCS_PAGES`; the template ships it, projects adopt by hand — DOCS-2, DOCS-3, DOCS-4.
- [How is the desktop frontend laid out and built?](LAZY-41-desktop-frontend-architecture.md): `frontend/` here, a Rust relay that knows no method and a TypeScript JSON-RPC client generated from `docs/protocol/`; dev runs the jar `make` builds in the argument's or launch directory; `ddd-frontend` minus OpenAPI, TanStack, i18n and MSW, one store per watch; the TUI's screens and keys in order Board, session pane, Docs, Review, capture as DESKTOP-1…5; `make frontend` in `verify`; a reload restores the modes `claude`'s full SIGWINCH repaint omits from `sessionStorage`, then `session/repaint` nudges with a 50 ms gap; packaged builds cannot reload (ADR 0057).
- [How do notifications, reserved chords and swallowed keys work in the desktop frontend?](LAZY-45-notifications-and-configuration-on-the-desktop.md): the sidecar fires notifications over D-Bus as today; watched means the selected pane while it and the window have focus, from Tauri's focus events; reserved chords come from the same `config.yml` through `ConfigurationQuery`, matched on `KeyboardEvent.key`; `ctrl+z` and the fixed chords as in the TUI; `ctrl+shift+c`/`ctrl+shift+v` copy and paste, `shift`+drag selects, no zoom; every TUI key is `preventDefault`ed, checked in a packaged build (ADR 0058).
- [What does a jlink runtime for the sidecar need, and how does Tauri bundle it on Linux?](LAZY-46-what-a-jlink-bundle-needs.md): jdeps' `java.base,java.desktop,java.management,java.sql,jdk.jfr` plus the `java.xml` it misses; 84 MB stripped without `--compress`, 29 MB gzipped against zip-6's 37; linked from Temurin for glibc 2.17, not Arch's 2.38; pty4j's and JNA's natives pre-extracted, or pty4j leaves a `/tmp` directory per run; runtime and jar as `bundle.resources` from a `tauri build --config` file, spawned by the relay, no `externalBin`; deb, rpm and AppImage, the AppImage riskiest (linuxdeploy rewrites the runtime, Mesa fix only since bundler 2.10.0, FUSE 2); the relay's `main` sets the DMA-BUF variable.
- [How is the desktop frontend packaged, and what does a smoke test of the bundle check?](LAZY-47-packaging-and-its-smoke-test.md): a deb CI checks and an AppImage for use, no rpm; `runtime/`, the jar and pty4j's `natives/` under `/usr/lib/<productName>/`, no CDS; `make bundle` outside `verify`, linked from a pinned, checksummed Temurin download; a `bundle` job on `ubuntu-22.04` every PR, 14-day artifacts on `main`, no releases; the smoke test runs the bundle's sidecar on a stub `claude` (`ready`, no stub or `/tmp/pty4j-*` left) and the relay under Xvfb; a real `claude`, keys and the Hyprland focus walked once by hand, a recurring focus loss its own ticket; no TUI in the bundle (ADR 0059).

## Not yet specified

Nothing.

## Out of scope

- Sessions lazyclaude did not start — outside live sessions, their transcript view, the
  kitty jump, tmux, and the Elsewhere panel. Ruled out when the map was charted.
- Vikunja — dropped with the move to GitHub Issues.
- Several projects in one lazyclaude — one project per instance still.
- A Rust backend, or GraalVM Native Image, until a measured constraint asks for one.
- GitLab and any tracker but GitHub Issues.
- macOS, Windows and Linux on aarch64 — nothing can be tried off Linux x86_64; a later, separate
  discussion takes them on from LAZY-39's research ([LAZY-40](LAZY-40-which-platforms.md), ADR 0056).
