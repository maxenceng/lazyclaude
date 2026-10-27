# Roadmap: v1.1, the desktop evolution

The order to refine and build the desktop evolution's tickets in. The decisions behind it are on the
[LAZY-24](LAZY-24-desktop-evolution.md) map, and the phases are its *Destination*'s.

Refine each ticket just before it is built (`/ticket refine <id>`, one per session), not the whole list up front: its
*Model decision* depends on the code already there. Ticket ids don't give the order; this list does. Every phase is
Linux x86_64 only (ADR 0056).

## 1. Hosted sessions only

Built first: nothing should be ported, given mouse support or documented before it is cut.

1. [SESSION-10](SESSION-10-only-hosted-sessions.md): outside sessions, Elsewhere, the kitty and tmux jumps and the
   transcript view go (ADR 0047).
2. [SESSION-11](SESSION-11-session-id-known-at-launch.md): `--session-id` names a hosted session at launch, and its
   state comes from its own registry record.
3. [SESSION-12](SESSION-12-the-launch-log.md): the launch log lists past sessions and marks the interrupted ones
   (ADR 0048).
4. [SESSION-13](SESSION-13-the-pty-port-carries-bytes.md): the pty port carries bytes, and the TUI's primary adapter
   emulates (ADR 0049). The seam the sidecar needs.

## 2. Mouse in the TUI

5. [BOARD-7](BOARD-7-the-mouse-on-the-main-screen.md): focus, selection, second click, wheel and the per-frame hit map
   (ADR 0051). The other two build on its hit map and global handler.
6. [SESSION-14](SESSION-14-a-hosted-pane-takes-the-mouse-once-focused.md): a hosted pane takes the mouse once it is
   focused, keeps a drag's release, and gets hover.
7. [REVIEW-4](REVIEW-4-the-mouse-on-the-review-screen.md): files, comments, diff lines and a dragged range.

## 3. GitHub Issues as the tracker

One cut-over, for the claude-tools harness and every project (ADR 0052–0055).

8. [LAZY-42](LAZY-42-harness-on-github-issues.md): `/ticket`, `/wayfinder` and `/debt` on Issues, in plugin 3.0.0.
   It is built in claude-tools, and no project upgrades before it lands.
9. [LAZY-43](LAZY-43-migrating-a-backlog-to-issues.md): the resumable migration script, with next-suggestions
   migrated first.
10. [BOARD-8](BOARD-8-the-board-reads-github-issues.md): the board reads Issues through `gh` (ADR 0054).
11. [LAZY-44](LAZY-44-lazyclaude-backlog-on-issues.md): lazyclaude's own backlog moves to Issues. This file is deleted
    with `docs/backlog/` here (ADR 0055); what is left of it moves to a pinned *Roadmap v1.1* issue, a
    checklist of issue numbers, and from this step on ids are issue numbers.
12. [BOARD-9](BOARD-9-capturing-a-draft-ticket.md): `c` on the Board captures a draft ticket as a new issue.

## 4. MkDocs for every other doc

This phase depends on none of the first three, so it can be built earlier.

13. [DOCS-2](DOCS-2-docs-build-on-zensical.md): the docs build on Zensical, pinned, `make docs` strict in `verify`.
14. [DOCS-3](DOCS-3-the-adr-index-is-generated-and-checked.md): `make adr-index`, checked stale by `adr-check`.
15. [DOCS-4](DOCS-4-the-panel-orders-as-the-site-does.md): the Docs panel orders documents as the site does.

The spring-ddd template's share of the docs build is in claude-tools' own backlog.

## 5. A sidecar backend

Needs phase 1's SESSION-11 to SESSION-13 for its hosted sessions (ADR 0050).

16. [SIDECAR-1](SIDECAR-1-the-sidecar-answers-the-board.md): `lazyclaude --sidecar` answers the board over JSON Lines.
17. [SIDECAR-2](SIDECAR-2-the-protocol-schema-is-generated-and-checked.md): the protocol schema is generated from the
    Views and drift-checked.
18. [SIDECAR-3](SIDECAR-3-hosted-sessions-over-the-sidecar.md): hosted sessions over the sidecar, `session/repaint`
    included.
19. [SIDECAR-4](SIDECAR-4-docs-and-review-over-the-sidecar.md): docs and review over the sidecar.

## 6. The desktop frontend and its packaging

20. [DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md): `frontend/`, the relay, the protocol client and
    the Board, plus the window-level key handler (ADR 0057, ADR 0058).
21. [DESKTOP-2](DESKTOP-2-hosted-sessions-in-the-desktop-frontend.md): hosted sessions in xterm.js panes, with reload,
    keys, copy and paste, and the watched session.
22. [DESKTOP-3](DESKTOP-3-docs-in-the-desktop-frontend.md): the Docs panel.
23. [DESKTOP-4](DESKTOP-4-review-in-the-desktop-frontend.md): the Review screen.
24. [DESKTOP-5](DESKTOP-5-capture-in-the-desktop-frontend.md): capture.
25. [DESKTOP-6](DESKTOP-6-the-desktop-frontend-ships-as-a-bundle.md): `make bundle` builds a deb and an AppImage on
    a pinned Temurin runtime, smoke-tested in CI's `bundle` job and walked once by hand (ADR 0059). Needs only
    DESKTOP-2, so it can come straight after it.
