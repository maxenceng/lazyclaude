import { describe, expect, it } from 'vitest';
import type { SessionWorktreeShownView } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { Worktree, type WorktreeTicket } from './worktree';

const CLEAR: SessionWorktreeShownView = { kind: 'clearWorktree', shown: '../app.worktrees/proj-1-a-ticket' };
const HELD: SessionWorktreeShownView = { kind: 'notClearWorktree', shown: '../app.worktrees/proj-1-a-ticket', state: { kind: 'noMergedPullRequest' }, held: 'cannot remove: no merged pull request' };
const DONE: WorktreeTicket = { id: 'PROJ-1', branch: 'proj-1-a-ticket', done: true };
const OTHER: WorktreeTicket = { id: 'PROJ-2', branch: 'proj-2-b-ticket', done: true };

const worktreeOf = (initial: WorktreeTicket | null = DONE, reread: (id: string) => Promise<WorktreeTicket | undefined> = () => Promise.resolve(undefined)) => {
  const client = fakeClient();
  const state: { ticket: WorktreeTicket | undefined } = { ticket: initial ?? undefined };
  const worktree = new Worktree({ client, ticket: () => state.ticket, reread });
  return { client, state, worktree };
};

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

const reads = (client: ReturnType<typeof fakeClient>) => client.calls.filter((call) => call.method === 'session/worktreeOf');

describe('reading the selected ticket\'s worktree', () => {
  it('shows nothing before the first read has landed', () => {
    const { worktree } = worktreeOf();
    worktree.refresh();
    expect(worktree.shown()).toBeUndefined();
  });

  it('asks the sidecar with the ticket\'s branch alone and shows the answer', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(reads(client)[0]?.params).toEqual({ branch: 'proj-1-a-ticket' });
    expect(worktree.shown()).toEqual(CLEAR);
  });

  it('asks nothing for a ticket that is not done', () => {
    const { client, worktree } = worktreeOf({ ...DONE, done: false });
    worktree.refresh();
    expect(reads(client)).toEqual([]);
    expect(worktree.shown()).toBeUndefined();
  });

  it('asks nothing with no ticket selected', () => {
    const { client, worktree } = worktreeOf(null);
    worktree.refresh();
    expect(reads(client)).toEqual([]);
  });

  it('shows no section where the ticket has no worktree', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    client.answer('session/worktreeOf', { kind: 'noWorktree' });
    await settle();
    expect(worktree.shown()).toBeUndefined();
  });

  it('shows nowhere a read that lands after another ticket was selected', async () => {
    const { client, state, worktree } = worktreeOf();
    worktree.refresh();
    state.ticket = OTHER;
    worktree.refresh();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(worktree.shown()).toBeUndefined();
  });

  it('reads one at a time, and once more after the one in flight when something changed meanwhile', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    worktree.refresh();
    worktree.refresh();
    expect(reads(client)).toHaveLength(1);
    client.answer('session/worktreeOf', HELD);
    await settle();
    expect(reads(client)).toHaveLength(2);
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(reads(client)).toHaveLength(2);
    expect(worktree.shown()).toEqual(CLEAR);
  });

  it('keeps what it shows while it reads again for the same ticket', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    worktree.refresh();
    expect(worktree.shown()).toEqual(CLEAR);
  });

  it('shows nothing for a ticket selected after, until its own read lands', async () => {
    const { client, state, worktree } = worktreeOf();
    worktree.refresh();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    state.ticket = OTHER;
    expect(worktree.shown()).toBeUndefined();
  });

  it('shows nothing when the read fails, and reads again at the next change', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    client.fail('session/worktreeOf', new Error('down'));
    await settle();
    expect(worktree.shown()).toBeUndefined();
    worktree.refresh();
    expect(reads(client)).toHaveLength(2);
  });
});

const clearAndShown = async () => {
  const made = worktreeOf();
  made.worktree.refresh();
  made.client.answer('session/worktreeOf', CLEAR);
  await settle();
  return made;
};

describe('removing the worktree', () => {
  it('opens the question for a clean worktree', async () => {
    const { worktree } = await clearAndShown();
    worktree.request();
    expect(worktree.isAsking()).toBe(true);
    expect(worktree.asking.get()?.shown).toBe('../app.worktrees/proj-1-a-ticket');
  });

  it('opens nothing for a worktree that is held back', async () => {
    const { client, worktree } = worktreeOf();
    worktree.refresh();
    client.answer('session/worktreeOf', HELD);
    await settle();
    worktree.request();
    expect(worktree.isAsking()).toBe(false);
  });

  it('opens nothing before a read, or for a ticket not done', () => {
    const { worktree, state } = worktreeOf();
    worktree.request();
    expect(worktree.isAsking()).toBe(false);
    worktree.refresh();
    state.ticket = { ...DONE, done: false };
    worktree.request();
    expect(worktree.isAsking()).toBe(false);
  });

  it('offers removal only while the selected worktree is clean', async () => {
    const { client, worktree } = worktreeOf();
    expect(worktree.removable()).toBe(false);
    worktree.refresh();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(worktree.removable()).toBe(true);
    worktree.refresh();
    client.answer('session/worktreeOf', HELD);
    await settle();
    expect(worktree.removable()).toBe(false);
  });

  it('removes nothing on Keep, esc or a press outside', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(false);
    expect(worktree.isAsking()).toBe(false);
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toEqual([]);
  });

  it('removes the worktree on Remove and drops the section once it is removed', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    expect(worktree.isAsking()).toBe(false);
    expect(client.calls.at(-1)).toEqual({ method: 'session/removeWorktree', params: { branch: 'proj-1-a-ticket' } });
    client.answer('session/removeWorktree', { kind: 'removedWorktree' });
    await settle();
    expect(worktree.shown()).toBeUndefined();
    expect(worktree.refusal.get()).toBeUndefined();
  });

  it('shows a refusal in the ticket and reads the worktree again, retrying nothing by itself', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: 1 dirty path' });
    await settle();
    expect(worktree.refusal.get()).toBe('cannot remove: 1 dirty path');
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toHaveLength(1);
    expect(reads(client)).toHaveLength(2);
  });

  it('shows the reason of a removal that fails outright', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    client.fail('session/removeWorktree', new Error('sidecar gone'));
    await settle();
    expect(worktree.refusal.get()).toBe('sidecar gone');
  });

  it('forgets a refusal once another ticket is selected', async () => {
    const { client, state, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: x' });
    await settle();
    state.ticket = OTHER;
    worktree.refresh();
    expect(worktree.refusal.get()).toBeUndefined();
  });

  it('offers nothing again while a removal is in flight, so d and the row cannot send a second one', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    expect(worktree.removable()).toBe(false);
    worktree.request();
    expect(worktree.isAsking()).toBe(false);
    client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: 1 dirty path' });
    await settle();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(worktree.removable()).toBe(true);
  });

  it('keeps the reason of a refused removal when the read after it fails', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: 1 dirty path' });
    await settle();
    client.fail('session/worktreeOf', new Error('sidecar gone'));
    await settle();
    expect(worktree.shown()).toBeUndefined();
    expect(worktree.refusal.get()).toBe('cannot remove: 1 dirty path');
  });

  it('drops the reason of an earlier refusal once the retry is answered', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.request();
    worktree.answer(true);
    client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: 1 dirty path' });
    await settle();
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    worktree.request();
    worktree.answer(true);
    client.answer('session/removeWorktree', { kind: 'removedWorktree' });
    await settle();
    expect(worktree.refusal.get()).toBeUndefined();
  });

  it('answers Remove on nothing when no question is open', async () => {
    const { client, worktree } = await clearAndShown();
    worktree.answer(true);
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toEqual([]);
  });
});

describe('a /ticket done session ending', () => {
  it('reads the board again and opens the question for a ticket now done with a clean worktree, wherever focus is', async () => {
    const { client, worktree } = worktreeOf(null, () => Promise.resolve(OTHER));
    void worktree.ticketDoneEnded('PROJ-2');
    await settle();
    expect(reads(client)[0]?.params).toEqual({ branch: 'proj-2-b-ticket' });
    client.answer('session/worktreeOf', { kind: 'clearWorktree', shown: '../app.worktrees/proj-2-b-ticket' });
    await settle();
    expect(worktree.asking.get()).toEqual({ branch: 'proj-2-b-ticket', shown: '../app.worktrees/proj-2-b-ticket' });
  });

  it('opens nothing for a ticket that does not read done', async () => {
    const { client, worktree } = worktreeOf(null, () => Promise.resolve({ ...OTHER, done: false }));
    void worktree.ticketDoneEnded('PROJ-2');
    await settle();
    expect(reads(client)).toEqual([]);
    expect(worktree.isAsking()).toBe(false);
  });

  it('opens nothing for a ticket the board no longer has', async () => {
    const { client, worktree } = worktreeOf();
    void worktree.ticketDoneEnded('PROJ-2');
    await settle();
    expect(reads(client)).toEqual([]);
  });

  it.each([{ kind: 'noWorktree' }, HELD, { kind: 'unreadWorktree', reason: 'x', held: 'cannot remove: unreadable (x)' }] as SessionWorktreeShownView[])('opens nothing for a worktree that reads %j', async (view) => {
    const { client, worktree } = worktreeOf(null, () => Promise.resolve(OTHER));
    void worktree.ticketDoneEnded('PROJ-2');
    await settle();
    client.answer('session/worktreeOf', view);
    await settle();
    expect(worktree.isAsking()).toBe(false);
  });

  it('removes the branch the question was opened for, not the one selected', async () => {
    const { client, worktree } = worktreeOf(DONE, () => Promise.resolve(OTHER));
    void worktree.ticketDoneEnded('PROJ-2');
    await settle();
    client.answer('session/worktreeOf', { kind: 'clearWorktree', shown: '../x' });
    await settle();
    worktree.answer(true);
    expect(client.calls.at(-1)).toEqual({ method: 'session/removeWorktree', params: { branch: 'proj-2-b-ticket' } });
  });

  it('never reads twice at once when the ended ticket is the selected one, whose read the board re-read started', async () => {
    const { client, worktree } = worktreeOf(DONE, () => {
      worktree.refresh();
      return Promise.resolve(DONE);
    });
    void worktree.ticketDoneEnded('PROJ-1');
    await settle();
    expect(reads(client)).toHaveLength(1);
    client.answer('session/worktreeOf', HELD);
    await settle();
    expect(reads(client)).toHaveLength(2);
    client.answer('session/worktreeOf', CLEAR);
    await settle();
    expect(worktree.asking.get()?.branch).toBe('proj-1-a-ticket');
    expect(worktree.shown(), 'the section shows the read the question was opened from').toEqual(CLEAR);
  });
});
