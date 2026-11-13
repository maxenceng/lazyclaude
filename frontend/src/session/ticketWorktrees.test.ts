import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fakeClient } from '../test/fakeClient';
import { TicketWorktrees, WORKTREE_DEBOUNCE_MS } from './ticketWorktrees';

const PROJECT = { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] };

const setup = () => {
  const client = fakeClient();
  const worktrees = new TicketWorktrees({ client, project: () => PROJECT });
  return { client, worktrees };
};

const settled = async () => {
  await vi.advanceTimersByTimeAsync(WORKTREE_DEBOUNCE_MS);
};

describe('the worktree of the ticket under the cursor', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('is not known while it is being read', async () => {
    const { worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    expect(worktrees.of('PROJ-1')).toBeUndefined();
  });

  it('is none when the ticket holds no worktree', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    client.answer('session/worktreeOf', { kind: 'noWorktree' });
    await vi.advanceTimersByTimeAsync(0);
    expect(worktrees.of('PROJ-1')).toEqual({ kind: 'noWorktree' });
  });

  it('is unreadable when git could not say, the reason kept', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    client.answer('session/worktreeOf', { kind: 'unreadableWorktree', reason: 'git failed' });
    await vi.advanceTimersByTimeAsync(0);
    expect(worktrees.of('PROJ-1')).toEqual({ kind: 'unreadableWorktree', reason: 'git failed' });
  });

  it('is asked for with the project, the branch and no merged head', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    expect(client.calls).toEqual([{ method: 'session/worktreeOf', params: { sessions: PROJECT, branch: 'proj-1-a', mergedHead: { kind: 'noMergedHead' } } }]);
  });

  it('is asked for once the cursor rests, not at every ticket it passes', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await vi.advanceTimersByTimeAsync(WORKTREE_DEBOUNCE_MS - 1);
    worktrees.ask({ id: 'PROJ-2', branch: 'proj-2-b' });
    await settled();
    expect(client.calls.map((call) => (call.params as { branch: string }).branch)).toEqual(['proj-2-b']);
  });

  it('drops the answer for a ticket the cursor has left', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    worktrees.ask({ id: 'PROJ-2', branch: 'proj-2-b' });
    client.answer('session/worktreeOf', { kind: 'heldWorktree', path: '/p/.worktrees/PROJ-1', state: { kind: 'noMergedPullRequest' }, clear: false });
    await vi.advanceTimersByTimeAsync(0);
    expect(worktrees.of('PROJ-1')).toBeUndefined();
  });

  it('asks nothing for no ticket, and drops a read pending for the one that was there', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    worktrees.ask(undefined);
    await settled();
    expect(client.calls).toEqual([]);
  });

  it('is a path while a worktree is held', async () => {
    const { client, worktrees } = setup();
    worktrees.ask({ id: 'PROJ-1', branch: 'proj-1-a' });
    await settled();
    client.answer('session/worktreeOf', { kind: 'heldWorktree', path: '/p/.worktrees/PROJ-1', state: { kind: 'noMergedPullRequest' }, clear: false });
    await vi.advanceTimersByTimeAsync(0);
    expect(worktrees.of('PROJ-1')).toEqual({ kind: 'heldWorktree', path: '/p/.worktrees/PROJ-1', state: { kind: 'noMergedPullRequest' }, clear: false });
  });
});
