import { describe, expect, it } from 'vitest';
import { ticketFixture } from './board/fixtures';
import { PullRequestRead, WorktreeRead } from './board/ticketStanding';
import type { ReviewPullRequestView, ReviewPullRequestsView } from './protocol';
import { standingOf } from './standing';

const pullRequest = (number: number, head: string): ReviewPullRequestView => ({ number, title: 'A change', head, draft: false, checks: [] });
const open = (...pullRequests: ReviewPullRequestView[]): ReviewPullRequestsView => ({ kind: 'openPullRequests', pullRequests });
const ticket = ticketFixture({ id: 'PROJ-1', branch: 'proj-1-a' });

describe('where a ticket stands', () => {
  it('joins the open pull request whose head is the ticket\'s branch', () => {
    expect(standingOf(ticket, undefined, open(pullRequest(3, 'other'), pullRequest(12, 'proj-1-a'))).pullRequest).toEqual({ read: PullRequestRead.Open, number: 12 });
  });

  it('says no pull request is open when none heads the branch', () => {
    expect(standingOf(ticket, undefined, open(pullRequest(3, 'other'))).pullRequest).toEqual({ read: PullRequestRead.None, number: undefined });
  });

  it('says the pull requests are unavailable when they could not be read, never none', () => {
    expect(standingOf(ticket, undefined, { kind: 'unavailablePullRequests', reason: 'gh failed' }).pullRequest.read).toBe(PullRequestRead.Unavailable);
  });

  it('says they are being read while they are checked', () => {
    expect(standingOf(ticket, undefined, { kind: 'checkingPullRequests' }).pullRequest.read).toBe(PullRequestRead.Reading);
  });

  it.each([
    [undefined, WorktreeRead.Reading, undefined],
    [{ kind: 'noWorktree' } as const, WorktreeRead.None, undefined],
    [{ kind: 'unreadableWorktree', reason: 'git failed' } as const, WorktreeRead.Unavailable, undefined],
    [{ kind: 'heldWorktree', path: '/p/.worktrees/PROJ-1', state: { kind: 'noMergedPullRequest' }, clear: false } as const, WorktreeRead.Held, '/p/.worktrees/PROJ-1'],
  ])('joins the worktree `session` read: %j', (view, read, path) => {
    expect(standingOf(ticket, view, open()).worktree).toEqual({ read, path });
  });
});
