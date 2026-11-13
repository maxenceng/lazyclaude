import { describe, expect, it } from 'vitest';
import { ticketFixture } from './board/fixtures';
import { PullRequestRead } from './board/ticketStanding';
import type { ReviewPullRequestView, ReviewPullRequestsView } from './protocol';
import { standingOf } from './standing';

const pullRequest = (number: number, head: string): ReviewPullRequestView => ({ number, title: 'A change', head, draft: false, checks: [] });
const open = (...pullRequests: ReviewPullRequestView[]): ReviewPullRequestsView => ({ kind: 'openPullRequests', pullRequests });
const ticket = ticketFixture({ id: 'PROJ-1', branch: 'proj-1-a' });

describe('where a ticket stands', () => {
  it('joins the open pull request whose head is the ticket\'s branch', () => {
    expect(standingOf(ticket, open(pullRequest(3, 'other'), pullRequest(12, 'proj-1-a'))).pullRequest).toEqual({ read: PullRequestRead.Open, number: 12 });
  });

  it('says no pull request is open when none heads the branch', () => {
    expect(standingOf(ticket, open(pullRequest(3, 'other'))).pullRequest).toEqual({ read: PullRequestRead.None, number: undefined });
  });

  it('says the pull requests are unavailable when they could not be read, never none', () => {
    expect(standingOf(ticket, { kind: 'unavailablePullRequests', reason: 'gh failed' }).pullRequest.read).toBe(PullRequestRead.Unavailable);
  });

  it('says they are being read while they are checked', () => {
    expect(standingOf(ticket, { kind: 'checkingPullRequests' }).pullRequest.read).toBe(PullRequestRead.Reading);
  });
});
