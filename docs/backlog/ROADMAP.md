# Roadmap: v1

The order to refine and build v1's tickets in. The decisions behind it are on the
[LAZY-1](LAZY-1-v1-terminal-ide.md) map.

Refine each ticket just before it is built (`/ticket refine <id>`, one per session), not
the whole list up front: its *Model decision* depends on the code already there.
Ticket ids don't give the order; this list does.

## 1. Skeleton

1. [BOARD-1](BOARD-1-board-from-the-backlog.md): the board from `docs/backlog/` in the
   one-screen frame. It creates the `board` context and the `shell`, and everything else
   builds on it.

## 2. The main view

2. [BOARD-2](BOARD-2-ticket-detail.md): ticket detail, with markdown rendering.
3. [DOCS-1](DOCS-1-docs-tree.md): the docs tree. It reuses the renderer from BOARD-2.

## 3. Reading sessions

4. [SESSION-1](SESSION-1-live-sessions.md): live sessions from the registry, with state
   and marker.
5. [SESSION-2](SESSION-2-past-sessions.md): past sessions from transcripts, through the
   cache.
6. [SESSION-3](SESSION-3-ticket-sessions-and-markers.md): a ticket's linked sessions, and
   its marker on the board.

## 4. Hosting sessions

7. [SESSION-4](SESSION-4-hosted-session.md): `claude` on a pty in a dock pane. This is the
   riskiest ticket; bring it forward if you want that risk settled early.

## 5. GitHub state

8. [REVIEW-1](REVIEW-1-pr-and-ci-on-the-board.md): PR and CI glyphs, plus the open-PR and
   merged-PR fetches. The merged state feeds the three tickets after it.
9. [BOARD-3](BOARD-3-status-from-the-branch-copy.md): status from the branch copy.
10. [SESSION-5](SESSION-5-session-from-a-ticket.md): starting or resuming a session from a
    ticket, with the verb picker and the ticket worktree. With this one, the `/ticket` loop
    works end to end.

## 6. Keeping current, and the rest of sessions

11. [BOARD-4](BOARD-4-board-stays-current.md): watchers and `R`.
12. [SESSION-7](SESSION-7-waiting-sessions-and-notifications.md): the Elsewhere panel and
    `notify-send`.
13. [SESSION-6](SESSION-6-outside-session.md): an outside live session's transcript view
    and the kitty jump.
14. [SESSION-8](SESSION-8-removing-a-ticket-worktree.md): removing a ticket worktree.

## 7. PR review

15. [REVIEW-2](REVIEW-2-pr-review-screen.md): the review screen, read-only.
16. [REVIEW-3](REVIEW-3-writing-a-review.md): writing a review into the pending review.

## 8. Finish

17. [BOARD-5](BOARD-5-zoomed-status-columns.md): zoomed status columns.
18. [SESSION-9](SESSION-9-configuration.md): the configuration file. It goes last because it
    makes configurable chords that every earlier ticket hard-coded.
