import { describe, expect, it } from 'vitest';
import { PullRequestRead } from './board/ticketPullRequest';
import type { ReviewPullRequestView, ReviewPullRequestsView } from './protocol';
import { ticketPullRequestOf } from './ticketPullRequest';

const pullRequest = (number: number, head: string): ReviewPullRequestView => ({ number, title: 'A change', head, draft: false, checks: [] });
const open = (...pullRequests: ReviewPullRequestView[]): ReviewPullRequestsView => ({ kind: 'openPullRequests', pullRequests });

describe('the pull request a ticket\'s facts line names', () => {
  it('is the one review matched to the ticket\'s branch', () => {
    expect(ticketPullRequestOf(open(pullRequest(3, 'other'), pullRequest(12, 'proj-1-a')), pullRequest(12, 'proj-1-a'))).toEqual({ read: PullRequestRead.Open, number: 12 });
  });

  it('is none when review matched none to the branch', () => {
    expect(ticketPullRequestOf(open(pullRequest(3, 'other')), undefined)).toEqual({ read: PullRequestRead.None, number: undefined });
  });

  it('is unavailable when the pull requests could not be read, never none', () => {
    expect(ticketPullRequestOf({ kind: 'unavailablePullRequests', reason: 'gh failed' }, undefined).read).toBe(PullRequestRead.Unavailable);
  });

  it('is being read while they are checked', () => {
    expect(ticketPullRequestOf({ kind: 'checkingPullRequests' }, undefined).read).toBe(PullRequestRead.Reading);
  });
});
