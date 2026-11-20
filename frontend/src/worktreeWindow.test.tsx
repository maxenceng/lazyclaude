import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { AppModel } from './app';
import { boardFixture, ticketFixture } from './board/fixtures';
import { WindowTab } from './layout';
import { Ready } from './protocol';
import { treeFixture } from './docs/fixtures';
import { sessionsFixture } from './session/fixtures';
import { fakeClient } from './test/fakeClient';
import { testPlatform } from './testPlatform';

const PATH = '../app.worktrees/proj-1-a-ticket';
const CLEAR = { kind: 'clearWorktree', shown: PATH };
const HELD = { kind: 'notClearWorktree', shown: PATH, state: { kind: 'noMergedPullRequest' }, held: 'cannot remove: no merged pull request' };

const done = boardFixture([ticketFixture({ id: 'PROJ-1', branch: 'proj-1-a-ticket', title: 'Finished', status: 'DONE' })]);
const open = boardFixture([ticketFixture({ id: 'PROJ-1', branch: 'proj-1-a-ticket', title: 'Working', status: 'IN_PROGRESS' })]);

const settle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

const readyApp = async (shown = done, worktree: unknown = CLEAR) => {
  const client = fakeClient(new Ready());
  const app = new AppModel(client, testPlatform());
  app.start();
  render(<App app={app} packaged={false} />);
  await act(async () => {
    client.answer('board/watch', shown);
    client.answer('session/configuration', { chords: [], desktopNotifications: true, ticketPlugin: 'ddd-workflow', statusVerbs: [], mouse: true });
    client.answer('warning/watch', { kind: 'noWarning' });
    await Promise.resolve();
  });
  await settle();
  await act(async () => {
    client.answer('docs/watch', treeFixture([]));
    client.answer('session/watch', sessionsFixture([]));
    if (worktree !== null) {
      client.answer('session/worktreeOf', worktree);
    }
    await Promise.resolve();
  });
  await settle();
  return { client, app, user: userEvent.setup() };
};

const reads = (client: ReturnType<typeof fakeClient>) => client.calls.filter((call) => call.method === 'session/worktreeOf');
const keyBar = () => screen.getByRole('contentinfo');

describe('a done ticket\'s worktree in the window', () => {
  it('shows a worktree section under the sessions, with the clean pill and remove d', async () => {
    await readyApp();
    const section = screen.getByRole('region', { name: 'worktree' });
    expect(section).toHaveTextContent('clean');
    expect(section).toHaveTextContent(PATH);
    expect(section).toHaveTextContent('remove');
  });

  it('shows no section for a ticket that is not done, and reads nothing for it', async () => {
    const { client } = await readyApp(open, null);
    expect(screen.queryByRole('region', { name: 'worktree' })).toBeNull();
    expect(reads(client)).toEqual([]);
  });

  it('shows no section where the ticket has no worktree', async () => {
    await readyApp(done, { kind: 'noWorktree' });
    expect(screen.queryByRole('region', { name: 'worktree' })).toBeNull();
  });

  it('shows no section before the first read has landed', async () => {
    await readyApp(done, null);
    expect(screen.queryByRole('region', { name: 'worktree' })).toBeNull();
  });

  it('shows no section in the Board tab\'s preview', async () => {
    const { app } = await readyApp();
    act(() => app.chooseTab(WindowTab.Board));
    expect(screen.queryByRole('region', { name: 'worktree' })).toBeNull();
  });

  it('reads the worktree again when the sessions change, the merged pull requests change or the board changes', async () => {
    const { client } = await readyApp();
    act(() => client.push('session/changed', sessionsFixture([])));
    expect(reads(client)).toHaveLength(2);
    client.answerAll('session/worktreeOf', CLEAR);
    await settle();
    act(() => client.push('review/mergedChanged', []));
    expect(reads(client)).toHaveLength(3);
    client.answerAll('session/worktreeOf', CLEAR);
    await settle();
    act(() => client.push('board/changed', done));
    expect(reads(client)).toHaveLength(4);
  });

  it('says held with its reason and no remove, and offers no d on the key bar', async () => {
    await readyApp(done, HELD);
    const section = screen.getByRole('region', { name: 'worktree' });
    expect(section).toHaveTextContent('unknown');
    expect(section).toHaveTextContent('cannot remove: no merged pull request');
    expect(section).not.toHaveTextContent('remove d');
    expect(keyBar()).not.toHaveTextContent('d remove worktree');
  });

  it('offers d remove worktree on the key bar while the worktree is clean', async () => {
    await readyApp();
    expect(keyBar()).toHaveTextContent('d remove worktree');
  });
});

describe('removing it', () => {
  it('opens the question on d, with the path and what stays, and removes nothing on esc', async () => {
    const { client, user } = await readyApp();
    await user.keyboard('d');
    const dialog = screen.getByRole('dialog', { name: 'Remove this worktree?' });
    expect(dialog).toHaveTextContent(PATH);
    expect(dialog).toHaveTextContent('The branch stays.');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toEqual([]);
  });

  it('opens the question from a click on the row, which keeps the focus where it was', async () => {
    const { user } = await readyApp();
    await user.click(within(screen.getByRole('region', { name: 'worktree' })).getByRole('button'));
    expect(screen.getByRole('dialog', { name: 'Remove this worktree?' })).toBeInTheDocument();
  });

  it('opens nothing on d where the worktree is held back', async () => {
    const { user } = await readyApp(done, HELD);
    await user.keyboard('d');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('opens nothing on d for a ticket that is not done', async () => {
    const { user } = await readyApp(open, null);
    await user.keyboard('d');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps the worktree on Keep, the close action and a press outside', async () => {
    const { client, user } = await readyApp();
    await user.keyboard('d');
    await user.click(screen.getByRole('button', { name: 'Keep' }));
    await user.keyboard('d');
    await user.click(screen.getByRole('button', { name: /close/i }));
    await user.keyboard('d');
    await user.pointer({ keys: '[MouseLeft>]', target: screen.getByRole('dialog').parentElement as HTMLElement });
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toEqual([]);
  });

  it('removes it on y and drops the section once it is removed', async () => {
    const { client, user } = await readyApp();
    await user.keyboard('d');
    await user.keyboard('y');
    expect(client.calls.at(-1)).toEqual({ method: 'session/removeWorktree', params: { branch: 'proj-1-a-ticket' } });
    await act(async () => {
      client.answer('session/removeWorktree', { kind: 'removedWorktree' });
      client.answerAll('session/worktreeOf', { kind: 'noWorktree' });
      await Promise.resolve();
    });
    await settle();
    expect(screen.queryByRole('region', { name: 'worktree' })).toBeNull();
    expect(keyBar()).not.toHaveTextContent('d remove worktree');
  });

  it('removes it from the Remove button too', async () => {
    const { client, user } = await readyApp();
    await user.keyboard('d');
    await user.click(screen.getByRole('button', { name: 'Remove' }));
    expect(client.calls.at(-1)?.method).toBe('session/removeWorktree');
  });

  it('shows a refusal in the ticket pane and offers d again as the retry', async () => {
    const { client, user } = await readyApp();
    await user.keyboard('d');
    await user.keyboard('y');
    await act(async () => {
      client.answer('session/removeWorktree', { kind: 'notRemovedWorktree', refusal: 'cannot remove: 1 dirty path' });
      await Promise.resolve();
    });
    await settle();
    expect(screen.getByRole('alert')).toHaveTextContent('cannot remove: 1 dirty path');
    expect(client.calls.filter((call) => call.method === 'session/removeWorktree')).toHaveLength(1);
    act(() => client.answerAll('session/worktreeOf', CLEAR));
    await settle();
    await user.keyboard('d');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});

describe('a /ticket done session ending', () => {
  it('opens the question at once for a ticket now done with a clean worktree, wherever focus is', async () => {
    const { client, app } = await readyApp(open, null);
    act(() => client.push('session/ticketDoneEnded', 'PROJ-1'));
    await act(async () => {
      client.answer('board/watch', done);
      await Promise.resolve();
    });
    await settle();
    await act(async () => {
      client.answerAll('session/worktreeOf', CLEAR);
      await Promise.resolve();
    });
    await settle();
    expect(client.calls.filter((call) => call.method === 'session/worktreeOf'), 'the read for the ended ticket waits for the one the board re-read started').toHaveLength(2);
    await act(async () => {
      client.answerAll('session/worktreeOf', CLEAR);
      await Promise.resolve();
    });
    await settle();
    expect(app.worktree.isAsking()).toBe(true);
    expect(screen.getByRole('dialog', { name: 'Remove this worktree?' })).toBeInTheDocument();
  });

  it('opens nothing when the ticket does not read done, or its worktree is held back', async () => {
    const { client, app } = await readyApp(open, null);
    act(() => client.push('session/ticketDoneEnded', 'PROJ-1'));
    await act(async () => {
      client.answer('board/watch', open);
      await Promise.resolve();
    });
    await settle();
    expect(reads(client)).toEqual([]);
    act(() => client.push('session/ticketDoneEnded', 'PROJ-1'));
    await act(async () => {
      client.answer('board/watch', done);
      await Promise.resolve();
    });
    await settle();
    await act(async () => {
      client.answerAll('session/worktreeOf', HELD);
      await Promise.resolve();
    });
    await settle();
    expect(app.worktree.isAsking()).toBe(false);
  });
});
