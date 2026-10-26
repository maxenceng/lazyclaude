---
id: LAZY-24
status: in-progress
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
- This map is charted in `docs/backlog/` and migrates to Issues with everything else in
  phase 3 — it is that migration's first test case.

## Decisions so far

- [What is the state of MkDocs in 2026?](LAZY-34-mkdocs-ecosystem.md): start on Zensical (Material's successor, 0.1.0 on 2026-11-05) with a `mkdocs.yml` both tools read, version pinned; MkDocs 1.x stalled and Material ends 2027-05-05; mermaid, `--strict` link checks and Pages deploys built in; ADRs get a hand-kept index; generated pages committed to their own folder; `mkdocs.yml`'s nav is no source for the docs tree order.
- [What can GitHub Issues and `gh` carry of a ticket?](LAZY-29-github-issues-capabilities.md): title, body, open/closed, native `blocked_by` dependencies and sub-issues (map → decisions) on a personal repo; issue types/fields are org-only, so `type`, `context`, `status` are labels (or a Projects v2 field, needing the `project` scope); branch and PR links readable back; no conditional write, only an `updatedAt` check before writing; ETag polling is free; no server-less push; `createdAt` not backdatable.
- [What mouse support do TamboUI and the hosted pty offer?](LAZY-27-tamboui-mouse-support.md): SGR press/release/drag/move/wheel with modifiers, no double-click; only left press, move and wheel reach elements and a click never selects a row, so the shell hit-tests against its own geometry; `claude` turns on 1000/1002/1003 SGR and handles its own clicks, so a pane forwards press/drag/release in order; existing gaps: no `mouseMotion` (no hover in `claude`), a drag leaving the pane loses its release; risks: X10 terminals type a click as keys, Windows mouse likely absent.
- [What stands between lazyclaude and macOS or Windows?](LAZY-39-cross-platform-blockers.md): our Linux-only code is small (`/proc` liveness, `notify-send`/`gdbus`, ConPTY opt-in, `claude.exe` resolution); the cost is TamboUI's Windows input bugs, JLine 3's quit hang on macOS, signing and notarizing a bundled runtime, one jlink runtime per OS and arch; only closing a notification on macOS/Windows via Tauri is blocked; all from sources, nothing run off Linux.

## Not yet specified

- **jlink packaging** — which modules pty4j, JLine and Jackson need, the runtime per
  platform, the Tauri sidecar layout. Waits on the protocol and the platform decision.
- **The desktop frontend's architecture** — repo layout (a `frontend/` here or its own repo),
  how far `ddd-frontend` applies without an OpenAPI client, the typed client over Tauri
  events, and the order features reach parity with the TUI. Waits on the protocol.
- **Whether the TUI goes through the protocol too**, or keeps calling the application layer
  in-process. Waits on the protocol.
- **Notifications and configuration in the desktop frontend** — `config.yml`'s reserved
  chords and the D-Bus notification have no obvious desktop equivalent. Waits on the
  platform decision and the frontend's architecture.
- **Integration tests across the boundary** — a protocol test harness, pty lifecycle tests,
  packaged-build smoke tests. Waits on the protocol.

## Out of scope

- Sessions lazyclaude did not start — outside live sessions, their transcript view, the
  kitty jump, tmux, and the Elsewhere panel. Ruled out when the map was charted.
- Vikunja — dropped with the move to GitHub Issues.
- Several projects in one lazyclaude — one project per instance still.
- A Rust backend, or GraalVM Native Image, until a measured constraint asks for one.
- GitLab and any tracker but GitHub Issues.
