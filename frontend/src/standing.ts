import { PullRequestRead, type TicketStanding } from './board/ticketStanding';
import type { BoardTicketView, ReviewPullRequestsView } from './protocol';

const READ_OF: Record<Exclude<ReviewPullRequestsView['kind'], 'openPullRequests'>, PullRequestRead> = {
  checkingPullRequests: PullRequestRead.Reading,
  unavailablePullRequests: PullRequestRead.Unavailable,
};

/** What the ticket's facts line says of it beyond the ticket: the open pull request whose head is its branch, from `review`. */
export const standingOf = (ticket: BoardTicketView, pullRequests: ReviewPullRequestsView): TicketStanding => {
  if (pullRequests.kind !== 'openPullRequests') {
    return { pullRequest: { read: READ_OF[pullRequests.kind], number: undefined } };
  }
  const headed = pullRequests.pullRequests.find((pullRequest) => pullRequest.head === ticket.branch);
  return { pullRequest: headed === undefined ? { read: PullRequestRead.None, number: undefined } : { read: PullRequestRead.Open, number: headed.number } };
};
