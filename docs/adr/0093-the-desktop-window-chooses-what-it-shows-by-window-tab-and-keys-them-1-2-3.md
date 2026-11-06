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
  tab is what the window draws over them. It opens on Base at every launch and is kept nowhere. The Board tab's own
  state, whether its preview is shown and what a second press on a ticket does, sits there too: going to Base is a
  window move, as Base's keys are routed in the composition.
- **The keys are `1`, `2` and `3`.** They are plain keys no binding uses, no reserved chord is a digit, and they read in the
  key bar as `1/2/3 tabs`. `KeyRouter` takes them only where it already takes a plain key: a hosted pane that has focus
  passes every plain key to its terminal, and an open review or the verb picker ignores them, so a digit typed to `claude`
  switches nothing. A click on a tab works wherever the window is, except under a review.
- **A tab other than Base takes focus to the Board** and draws over the panes, the dock folded to its strip (the TUI's dock
  strip). `h`/`j`/`k`/`l` and `enter` act on the tab; `esc` returns to Base. On a pull request `enter` or a click opens the
  Review screen, and the tab and row are kept, so ending the review returns to them.
- **The Board tab is cards over a preview, not the TUI's zoomed Board copied.** Its columns run the lifecycle left to right,
  draft first and done last, so a ticket moves rightwards as it advances; Base and the TUI keep what is under way first.
  Each ticket is a card, its title whole up to three lines over its number, type and labels. A click selects a card and
  the preview beneath details it as Base's main view does; `enter`, or a click on the card already selected, goes to Base
  with it selected. A first click never leaves the Board, since reading a ticket is the tab's job and going is the second.
  A click where no card is closes the preview, the columns taking its height; a card clicked or a key that moves the
  cursor opens it again.
- **Done shows its ten latest closed tickets**, the heading still counting all of them, where the TUI folds done to its
  count. `board` orders the done group latest closed first from the issue's `closedAt`, so the order is the domain's, and
  every reader of the board gets it. Base folds done, so a done card is previewed in place and never goes to Base.
- **A ticket's pull request on the Board tab is asked of `review`** (`review/pullRequestOn`, once per ticket branch, when
  the tab is shown and whenever the pull requests or the tickets change), never worked out by comparing heads in the window.
  The join is ADR 0090's.
- **The Pull requests list order lives in the window**, `worstFirst` in `frontend/src/review/pullRequestList.ts`: failed,
  pending, passed, a pull request with no check after those, and every draft last. The TUI lists no pull requests, so the
  order has one reader and a `deferred:` marker; it moves into `review` when a second frontend lists them.

## Consequences

- A ticket's pull request is asked once per drawn ticket on every change while the Board tab is shown, done cut to its
  ten; a long board sends many small calls. Closing that means a `review` operation that joins a whole board at once.
  A branch whose ask fails keeps the pull request it last answered.
- The Board tab's mark and CI glyph restate the TUI's rule (a draft or no check is marked; the result is the worst
  check, `review` listing them worst first) instead of reading it from `review`, since the decision keeps the protocol
  unchanged. A `deferred:` marker in `pullRequestList.ts` says so; a protocol change for `review` moves both onto its view.
- A done ticket whose issue is still open (labelled `status:done`) sorts before the closed ones, so the ten never hide
  the ticket marked done just now.
- The digits are not yet configurable; `config.yml` reserves only chords.
- The board read asks `gh` for `closedAt` as well; a closed issue without one, or with one that is not a time, makes the
  board unavailable with that reason, as any field it lacks does.
- Ten is a window choice in `frontend/src/board/rows.ts`; a done ticket older than the ten is read on GitHub or not at all.
- A tab label's finding (the in-progress count, the number of open pull requests and their worst result) is computed from
  what the window already holds, so it needs no read of its own.
