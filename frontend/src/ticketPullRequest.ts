import { PullRequestRead, type TicketPullRequest } from './board/ticketPullRequest';
import type { ReviewPullRequestView, ReviewPullRequestsView } from './protocol';

const READ_OF: Record<Exclude<ReviewPullRequestsView['kind'], 'openPullRequests'>, PullRequestRead> = {
  checkingPullRequests: PullRequestRead.Reading,
  unavailablePullRequests: PullRequestRead.Unavailable,
};

/** The pull request the ticket's facts line names: `headed`, the one `review` matched to the ticket's branch, once the open pull requests are read. */
export const ticketPullRequestOf = (pullRequests: ReviewPullRequestsView, headed: ReviewPullRequestView | undefined): TicketPullRequest => {
  if (pullRequests.kind !== 'openPullRequests') {
    return { read: READ_OF[pullRequests.kind], number: undefined };
  }
  return headed === undefined ? { read: PullRequestRead.None, number: undefined } : { read: PullRequestRead.Open, number: headed.number };
};
