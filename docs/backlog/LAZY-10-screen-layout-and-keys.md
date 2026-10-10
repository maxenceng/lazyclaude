---
id: LAZY-10
status: done
context: cross-context
type: decision
created: 2026-10-09
blocked_by: [LAZY-1, LAZY-4]
---

# What do the screens look like and how does one move between them?

## Question

What do the screens look like and how does one move between them?

lazygit-style side panels with a main view, or kanban columns per status? Where ticket detail, the ticket's sessions, the docs tree and waiting markers live; the global keys (`enter`, `q`, `?`, panel switching) and how they follow lazygit's conventions. Settled by reacting to a rough prototype.

From LAZY-7:
- there's no `e`, and every view is read-only;
- hosted sessions are embedded `claude` panes that swallow every key while focused, so a chord is needed to get back to lazyclaude;
- from LAZY-8, the three session markers need glyphs, colours and a place: waiting for permission, waiting for input, and your turn. A ticket shows the strongest marker among its sessions, and a status line shows the strongest overall;
- the screens also need a place for several hosted session panes, for an outside session's read-only transcript view, and for the "new unlinked session" action.

## Kind

prototype

## Resolution

**One screen: lazygit's panels on the left, a main view over a session dock on the right.**
Picked from a prototype of three layouts (lazygit panels, kanban with a stack of screens, an
IDE workbench with a dock) and a fourth that merges them. The user took A's panels, B's
columns and C's dock. The prototype lives on branch `prototype/lazy-10-screens`
(`prototypes/screens/index.html?variant=D`).

**Layout.**
- A status line on top: the strongest marker overall, per-state counts for this project, and
  other projects' waiting sessions (`elsewhere: ! next-suggestions`).
- Left column, four stacked panels:
  - **Board**: tickets grouped by status (`▾ in-progress (2)`), `done` folded. Each ticket shows
    the strongest marker among its sessions.
  - **Sessions**: the selected ticket's sessions, then an **unlinked** section with this
    project's sessions tied to no ticket. Each row shows its marker, its name, and where it
    is: `pane` (hosted), `kitty` (outside), or its age (past). Rows `n new: /ticket <verb>` and
    `N new unlinked` close each section. Sections replace tabs because `[ ]` need AltGr on
    AZERTY.
  - **Docs**: the `docs/**/*.md` tree.
  - **Elsewhere**: live sessions in other projects on this machine, with their markers.
- Right column, top: the **main view**, which follows the selection. It shows a ticket's
  detail, a rendered doc, or an outside or past session's read-only transcript.
- Right column, bottom: the **dock**. Hosted sessions stay on screen in it, one tab each with
  its marker, so the board stays visible while `claude` works.
- `m` (or `alt+m` from inside a session) hides the main view, so the dock fills the right
  column. It stays hidden until toggled back.
- `z` zooms: on the Board it opens status columns across the screen (the dock folds to a
  one-line strip of its tabs and markers, `enter` picks a ticket and returns). On the main
  view or the dock (`alt+z`) it goes full screen.
- A key bar at the bottom lists the keys for whatever has focus. `?` opens the full list.

**Keys outside a session pane** (lazygit's):
- `j`/`k` move, `h`/`l` change panel, `1`–`4` jump to a panel (Shift on AZERTY, so `h`/`l` is
  the main path).
- `enter` goes into the selection: on a ticket, it moves to its sessions. On a hosted session,
  it focuses its dock tab. On a past session, it resumes it in the dock. An outside session's
  transcript is already in the main view, and `o` jumps to its kitty window.
- `n` opens the verb picker on the likeliest verb (LAZY-9). `N` starts an unlinked session.
- `w` goes to the next waiting session. `p` opens the PR review (LAZY-11). `q` quits, with a
  confirmation while a hosted session is busy or waiting.

**Keys that work everywhere, a focused session included:** `alt+h/j/k/l` move focus between
panels, main view and dock. In the dock, `alt+h`/`alt+l` step through tabs, and `alt+h` on the
first tab leaves for the panels. `alt+k` returns to the main view (and shows it if hidden).
`alt+m` hides or shows the main view, `alt+z` zooms the dock, `alt+w` goes to the next waiting
session. Claude Code 2.1.294 binds `alt+b/d/f/y/o/p/t` (and `v`/`m` on Windows only), so none of
these collide. `ctrl+]` was rejected: it needs AltGr on AZERTY.

**Markers**, as prototyped and accepted:
- `!` red: waiting for permission.
- `?` yellow: waiting for input.
- `•` cyan: your turn.
- `∼` green marks busy, and `○` dim marks a past session. Neither counts as a marker.

The ADR is `docs/adr/0008-one-screen-with-a-session-dock-and-alt-hjkl-focus.md`.
