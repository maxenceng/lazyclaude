# 93. The desktop window chooses what it shows by window tab, keyed 1, 2 and 3

Date: 2026-11-06

## Status

Accepted. Builds on ADR 0082 (the composition side) and ADR 0090 (`review` decides the rules both frontends show).

## Context

The window showed one arrangement, the Board, Sessions and Docs panels over the main view and the dock. The TUI's zoomed
Board shows every ticket at once, and nothing in either frontend lists the open pull requests. The window needed the Board
as status columns and a list of pull requests, both reachable by key and by click, without taking a key from a hosted session.

## Decision

- **Three window tabs, Base, Board and Pull requests, start the status line.** The selected tab is `WindowTabs` in
  `frontend/src/windowTabs.ts`, a class beside `Layout`, not a field of it: the layout is where focus and the dock stand, the
  tab is what the window draws over them. It opens on Base at every launch and is kept nowhere.
- **The keys are `1`, `2` and `3`.** They are plain keys no binding uses, no reserved chord is a digit, and they read in the
  key bar as `1/2/3 tabs`. `KeyRouter` takes them only where it already takes a plain key: a hosted pane that has focus
  passes every plain key to its terminal, and an open review or the verb picker ignores them, so a digit typed to `claude`
  switches nothing. A click on a tab works wherever the window is, except under a review.
- **A tab other than Base takes focus to the Board** and draws over the panes, the dock folded to its strip (the TUI's dock
  strip). `h`/`j`/`k`/`l` and `enter` act on the tab; `esc` returns to Base. `enter` or a click on a ticket goes to Base
  with it selected; on a pull request it opens the Review screen, and the tab and row are kept, so ending the review
  returns to them.
- **A ticket's pull request on the Board tab is asked of `review`** (`review/pullRequestOn`, once per ticket branch, when
  the tab is shown and whenever the pull requests or the tickets change), never worked out by comparing heads in the window.
  The join is ADR 0090's.
- **The Pull requests list order lives in the window**, `worstFirst` in `frontend/src/review/pullRequestList.ts`: failed,
  pending, passed, a pull request with no check after those, and every draft last. The TUI lists no pull requests, so the
  order has one reader and a `deferred:` marker; it moves into `review` when a second frontend lists them.

## Consequences

- A ticket's pull request is asked once per branch on every change while the Board tab is shown; a long board sends many
  small calls. Closing that means a `review` operation that joins a whole board at once.
- The digits are not yet configurable; `config.yml` reserves only chords.
- A tab label's finding (the in-progress count, the number of open pull requests and their worst result) is computed from
  what the window already holds, so it needs no read of its own.
