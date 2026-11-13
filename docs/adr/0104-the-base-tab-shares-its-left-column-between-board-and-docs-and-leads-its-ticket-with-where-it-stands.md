# 104. The Base tab shares its left column between Board and Docs and leads its ticket with where it stands

Date: 2026-11-13

## Status

Accepted.

## Context

The Base tab stacked the Board over the Docs in a narrow left column, so each had half a column, and the main view kept the
ticket's prose hugging its left edge under a row of facts that said nothing of the ticket's pull request.
A session's marker showed on its ticket only as a small glyph. A throwaway prototype (variant H) was drawn and chosen.

## Decision

- **Panel tabs.** The left column shows the Board or the Docs panel at full height, never both: *panel tabs* in its title
  row, each with its count. `Layout` remembers which is shown (`Layout.panel`), derived from focus: a move that focuses Docs
  shows its tab, a move back to the Board shows the Board's, focus elsewhere leaves it. A click on a tab or `o` (`Action.TogglePanel`)
  switches and focuses it. Each panel keeps its own cursor, since each is its own controller. The new-draft `+` is the Board's
  pane action, so it shows on the Board tab only.
- **A ticket's strongest marker is a worded pill** (`permission`, `needs input`, `your turn`) at the end of its Board row, none
  for a busy or ended session.
- **A facts line under the title** says status, type, context, other labels and open pull request; a ticket with no open
  pull request says so, and unreadable pull requests say `unavailable`. It leaves out branch and worktree because their long
  text wraps the line. The pull request is the one `review` matches to the ticket's branch (`review/pullRequestOn`, through
  `TicketPullRequests`, as the Board tab asks it), so both tabs name the same one; the window only adds the reading and
  unavailable states of the open pull requests. The prototype's links are not built: nothing produces them.
- **Sessions come right after the facts**, those carrying a marker first and the strongest first (`rowsFor` orders them, so the
  cursor and the display agree, and the cursor keeps to its session when a read reorders them), each row one button carrying its action (`attach`, `resume`) and its key, `enter`. The ticket's prose is
  centred at its reading measure.
- **Session rows are tall, as in H:** a worded state pill (permission, needs input, your turn, busy, starting, unknown, ended),
  read from the session's state and not its lamp, which merges the states that carry no marker; the prompt, the
  age as elapsed time, then the action and `enter`; a row with a marker takes the frame and tint of its marker. The ticket pane's title
  row carries status, context and type as chips, and the number reads `#n` above the title.
- **An empty board** leaves the main view in place saying `No ticket selected.`, with no facts line, sessions or new-session
  bar; the Board shows every status at 0, the project is known from the empty board so a first draft can be captured, and the
  key bar keeps only the keys that act.
- **Key bars were trimmed to fit 80 columns** with the panel key added: `n/N new` is one entry, `enter open` and `l read` on
  the Docs panel, `n new` on the sessions, and the document's own bar does not carry the panel key.
- **A ticket lists a hosted tab whose live session is linked to it**, not only a tab started for it: a resumed session is
  started for no ticket, so it showed nowhere under its ticket. `session`'s `TicketSessionsView` says so, which widens ADR
  0096's "a tab was started for".

## Consequences

- With no ticket under the cursor the main view lists no sessions, and the sessions' cursor has no row to act on, so the five
  newest unlinked sessions are no longer reachable there; `N` still starts an unlinked session.
- The Docs panel's `enter open or fold` is labelled `enter open`; the fold is unchanged.
