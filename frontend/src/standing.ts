import { PullRequestRead, type TicketStanding, WorktreeRead, type WorktreeStanding } from './board/ticketStanding';
import type { BoardTicketView, ReviewPullRequestsView, SessionWorktreeView } from './protocol';

const READ_OF: Record<Exclude<ReviewPullRequestsView['kind'], 'openPullRequests'>, PullRequestRead> = {
  checkingPullRequests: PullRequestRead.Reading,
  unavailablePullRequests: PullRequestRead.Unavailable,
};

const worktreeStandingOf = (worktree: SessionWorktreeView | undefined): WorktreeStanding => {
  switch (worktree?.kind) {
    case undefined:
      return { read: WorktreeRead.Reading, path: undefined };
    case 'heldWorktree':
      return { read: WorktreeRead.Held, path: worktree.path };
    case 'noWorktree':
      return { read: WorktreeRead.None, path: undefined };
    case 'unreadableWorktree':
      return { read: WorktreeRead.Unavailable, path: undefined };
  }
};

/** What the ticket's facts line says of it beyond the ticket: its worktree, from `session`, and the open pull request whose head is its branch, from `review`. The two contexts meet here, in the window's composition, and nowhere else. */
export const standingOf = (ticket: BoardTicketView, worktree: SessionWorktreeView | undefined, pullRequests: ReviewPullRequestsView): TicketStanding => {
  const standing = worktreeStandingOf(worktree);
  if (pullRequests.kind !== 'openPullRequests') {
    return { worktree: standing, pullRequest: { read: READ_OF[pullRequests.kind], number: undefined } };
  }
  const headed = pullRequests.pullRequests.find((pullRequest) => pullRequest.head === ticket.branch);
  return { worktree: standing, pullRequest: headed === undefined ? { read: PullRequestRead.None, number: undefined } : { read: PullRequestRead.Open, number: headed.number } };
};
