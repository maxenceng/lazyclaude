import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { AppModel } from './app';
import { boardFixture, ticketFixture } from './board/fixtures';
import { Focus } from './layout';
import { Ready } from './protocol';
import { treeFixture } from './docs/fixtures';
import { pullRequestFixture } from './review/fixtures';
import { hostedFixture, sessionFixture, sessionsFixture, ticketSessionsFixture } from './session/fixtures';
import { fakeClient } from './test/fakeClient';
import { testPlatform } from './testPlatform';

const configuration = { chords: [], desktopNotifications: true, ticketPlugin: 'ddd-workflow', statusVerbs: [], mouse: true };

const settle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

const board = boardFixture(
  [
    ticketFixture({ id: 'PROJ-1', title: 'First', status: 'IN_PROGRESS', branch: 'proj-1-first' }),
    ticketFixture({ id: 'PROJ-2', title: 'Second', status: 'TODO', branch: 'proj-2-second' }),
    ticketFixture({ id: 'PROJ-3', title: 'Third', status: 'TODO', branch: 'proj-3-third' }),
    ticketFixture({ id: 'PROJ-4', title: 'Fourth', status: 'DONE', branch: 'proj-4-fourth' }),
  ],
  [{ id: 'PROJ-9', reason: 'no status label' }],
);

const failing = pullRequestFixture({ number: 12, title: 'Break it', head: 'proj-2-second', checks: [{ name: 'build', outcome: 'FAILED' }] });
const passing = pullRequestFixture({ number: 10, title: 'Fix it', head: 'proj-1-first', checks: [{ name: 'build', outcome: 'PASSED' }] });
const pending = pullRequestFixture({ number: 11, title: 'Wait for it', head: 'chore-tidy', checks: [{ name: 'build', outcome: 'PENDING' }] });
const draft = pullRequestFixture({ number: 13, title: 'Not yet', head: 'proj-3-third', draft: true, checks: [] });

const started = async (watch: unknown = { kind: 'openPullRequests', pullRequests: [passing, draft, failing, pending] }, read: unknown = board) => {
  const client = fakeClient(new Ready());
  const app = new AppModel(client, testPlatform());
  app.start();
  render(<App app={app} packaged={false} />);
  await act(async () => {
    client.answer('board/watch', read);
    client.answer('session/configuration', configuration);
    client.answer('warning/watch', { kind: 'noWarning' });
    await Promise.resolve();
  });
  await settle();
  await act(async () => {
    client.answer('docs/watch', treeFixture([]));
    client.answer('review/watch', watch);
    await Promise.resolve();
  });
  await settle();
  return { client, app, user: userEvent.setup() };
};

const hostSession = async ({ client, user }: Awaited<ReturnType<typeof started>>) => {
  await user.keyboard('N');
  await act(async () => {
    client.answer('session/newSessionFor', { kind: 'unlinkedStart' });
    await settle();
  });
  await act(async () => {
    client.answer('session/startUnlinked', { kind: 'startedHost', id: 'h1' });
    await Promise.resolve();
  });
  await settle();
};

const tab = (name: RegExp | string) => within(screen.getByRole('tablist', { name: 'window tabs' })).getByRole('tab', { name });

describe('the window tabs', () => {
  it('begin the status line with Base, Board and Pull requests, and open on Base', async () => {
    await started();
    expect(within(screen.getByRole('tablist', { name: 'window tabs' })).getAllByRole('tab').map((each) => each.textContent)).toEqual(['Base', 'Board1 in progress', 'Pull requests4 open✗']);
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('region', { name: 'Board' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Ticket' })).toBeInTheDocument();
  });

  it('carry no finding on Base, the in-progress count on Board, and the open count and worst result on Pull requests', async () => {
    await started();
    expect(within(tab('Base')).queryByText(/\d/)).not.toBeInTheDocument();
    expect(tab(/^Board/)).toHaveTextContent('1 in progress');
    expect(tab(/^Pull requests/)).toHaveTextContent('4 open');
    expect(within(tab(/^Pull requests/)).getByRole('img', { name: 'checks failed' })).toBeInTheDocument();
  });

  it('name what their count counts, a zero included', async () => {
    await started({ kind: 'openPullRequests', pullRequests: [] });
    expect(tab(/^Pull requests/)).toHaveTextContent('0 open');
    expect(within(tab(/^Pull requests/)).getByText('open').nextElementSibling, 'no empty check slot widens the badge').toBeNull();
  });

  it('show a pending result on the tab when none failed', async () => {
    await started({ kind: 'openPullRequests', pullRequests: [passing, pending] });
    expect(within(tab(/^Pull requests/)).getByRole('img', { name: 'checks pending' })).toBeInTheDocument();
  });

  it('are chosen by a click, and by 1, 2 and 3 which the key bar shows', async () => {
    const { user } = await started();
    expect(screen.getByRole('contentinfo')).toHaveTextContent('1/2/3 tabs');
    await user.click(tab(/^Pull requests/));
    expect(tab(/^Pull requests/)).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('2');
    expect(tab(/^Board/)).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('1');
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
  });
});

describe('the Board tab', () => {
  it('draws a column of cards per status headed by its count, a status with no ticket kept and done showing its cards', async () => {
    const { user } = await started();
    await user.keyboard('2');
    const columns = ['IN PROGRESS', 'IN REVIEW', 'TODO', 'DRAFT', 'DONE'].map((name) => screen.getByRole('group', { name: new RegExp(name, 'i') }));
    expect(columns.map((column) => column.textContent)).toEqual([
      'in progress1FirstPROJ-1',
      'in review0none',
      'todo2SecondPROJ-2ThirdPROJ-3',
      'draft0none',
      'done1FourthPROJ-4',
    ]);
  });

  it('marks a ticket\'s pull request with its draft mark and CI glyph, once review has joined them', async () => {
    const { client, user } = await started();
    await user.keyboard('2');
    await act(async () => {
      const headed = new Map([passing, failing, draft].map((pullRequest) => [pullRequest.head, pullRequest]));
      client.calls
        .filter((call) => call.method === 'review/pullRequestOn')
        .map((call) => headed.get((call.params as { branch: string }).branch))
        .forEach((pullRequest) =>
          client.answer('review/pullRequestOn', pullRequest === undefined ? { kind: 'noHeadedPullRequest' } : { kind: 'headedPullRequest', pullRequest }),
        );
      await Promise.resolve();
    });
    await settle();
    expect(within(screen.getByRole('button', { name: /PROJ-1/ })).getByRole('img', { name: 'checks passed' })).toBeInTheDocument();
    expect(within(screen.getByRole('button', { name: /PROJ-2/ })).getByRole('img', { name: 'checks failed' })).toBeInTheDocument();
    expect(within(screen.getByRole('button', { name: /PROJ-3/ })).getByRole('img', { name: 'draft or without checks' })).toBeInTheDocument();
  });

  it('asks review for the pull requests of the cards drawn only, done cut to its latest closed', async () => {
    const closed = Array.from({ length: 12 }, (_, index) =>
      ticketFixture({ id: `PROJ-${index + 20}`, title: `Closed ${index}`, status: 'DONE', branch: `proj-${index + 20}-closed` }),
    );
    const { client, user } = await started(undefined, boardFixture(closed, []));
    await user.keyboard('2');
    await settle();
    const asked = client.calls.filter((call) => call.method === 'review/pullRequestOn').map((call) => (call.params as { branch: string }).branch);
    expect(new Set(asked), 'the two done tickets past the ten drawn have no card to mark, so asking for them is a round trip for nothing').toEqual(
      new Set(closed.slice(0, 10).map((ticket) => ticket.branch)),
    );
  });

  it('draws no section and no row for the tickets that could not be read', async () => {
    const { user } = await started();
    await user.keyboard('2');
    expect(screen.queryByText(/PROJ-9/)).not.toBeInTheDocument();
    expect(screen.queryByText('unreadable')).not.toBeInTheDocument();
  });

  it('draws the Base tab\'s Board without the tickets that could not be read either', async () => {
    await started();
    expect(screen.queryByText(/PROJ-9/)).not.toBeInTheDocument();
    expect(screen.queryByText('unreadable')).not.toBeInTheDocument();
  });

  it('moves the cursor with j, k, h and l, and enter goes to Base with that ticket selected', async () => {
    const { user } = await started();
    await user.keyboard('2');
    expect(screen.getByRole('button', { name: /PROJ-1/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('h');
    expect(screen.getByRole('button', { name: /PROJ-2/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('j');
    expect(screen.getByRole('button', { name: /PROJ-3/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('k');
    await user.keyboard('l');
    expect(screen.getByRole('button', { name: /PROJ-1/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('h{Enter}');
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('button', { name: /PROJ-2/ })).toHaveAttribute('aria-current', 'true');
  });

  it('selects a card on a click and previews it beside the columns, staying on the Board', async () => {
    const { user } = await started();
    await user.keyboard('2');
    await user.click(screen.getByRole('button', { name: /PROJ-3/ }));
    expect(tab(/^Board/)).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('button', { name: /PROJ-3/ })).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('heading', { name: 'Third' })).toBeInTheDocument();
  });

  it('goes to Base on a ticket when its selected card is clicked again', async () => {
    const { user } = await started();
    await user.keyboard('2');
    await user.click(screen.getByRole('button', { name: /PROJ-3/ }));
    await user.click(screen.getByRole('button', { name: /PROJ-3/ }));
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('button', { name: /PROJ-3/ })).toHaveAttribute('aria-current', 'true');
  });

  it('keeps the preview when a click lands where no card is', async () => {
    const { user } = await started();
    await user.keyboard('2');
    await user.click(screen.getByRole('group', { name: /draft/i }));
    expect(screen.getByRole('heading', { name: 'First' })).toBeInTheDocument();
    expect(tab(/^Board/)).toHaveAttribute('aria-selected', 'true');
  });

  it('previews the cursor\'s ticket in a pane of its own beside the Board pane, as Base lays a ticket out', async () => {
    const { user } = await started();
    await user.keyboard('2');
    const preview = within(screen.getByRole('region', { name: 'Ticket' }));
    expect(preview.getByRole('heading', { name: 'First' })).toBeInTheDocument();
    expect(preview.getByText('status')).toBeInTheDocument();
    expect(preview.getByText('pull request')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Board' })).queryByRole('heading', { name: 'First' })).not.toBeInTheDocument();
  });

  it('goes to a done card on Base, by click or enter, as Base lists it', async () => {
    const { user } = await started();
    await user.keyboard('2');
    await user.click(screen.getByRole('button', { name: /PROJ-4/ }));
    await user.click(screen.getByRole('button', { name: /PROJ-4/ }));
    expect(tab(/^Base/)).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('heading', { name: 'Fourth' })).toBeInTheDocument();
  });

  it('goes to the selected done card on Base with enter', async () => {
    const { user } = await started();
    await user.keyboard('2');
    await user.click(screen.getByRole('button', { name: /PROJ-4/ }));
    await user.keyboard('{Enter}');
    expect(tab(/^Base/)).toHaveAttribute('aria-selected', 'true');
  });
});

const pushSessions = (ready: Awaited<ReturnType<typeof started>>, ...live: ReturnType<typeof sessionFixture>[]) => {
  const hosted = live.map((session) => hostedFixture(`h-${session.id}`, { ticket: 'PROJ-1', live: session }));
  act(() => ready.client.push('session/changed', { ...sessionsFixture(live, { tickets: [ticketSessionsFixture('PROJ-1', { live, hosted })] }), hosted }));
};

describe('a card on the Board tab and the sessions waiting on its ticket', () => {
  it('carries the worded pill of the strongest waiting session and is tinted in its colour', async () => {
    const ready = await started();
    await ready.user.keyboard('2');
    pushSessions(ready, sessionFixture({ id: 'turn', ticket: 'PROJ-1', state: { kind: 'yourTurn' } }), sessionFixture({ id: 'ask', ticket: 'PROJ-1', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } } }));
    const card = screen.getByRole('button', { name: /First/ });
    expect(within(card).getByRole('img')).toHaveTextContent('needs input');
    expect(card, 'the card is tinted in the lamp of the strongest session').toHaveAttribute('data-lamp', 'input');
  });

  it('carries no pill and no tint when its sessions are busy or ended', async () => {
    const ready = await started();
    await ready.user.keyboard('2');
    pushSessions(ready, sessionFixture({ id: 'busy', ticket: 'PROJ-1', state: { kind: 'busy' } }));
    const card = screen.getByRole('button', { name: /First/ });
    expect(within(card).queryByRole('img')).not.toBeInTheDocument();
    expect(card).not.toHaveAttribute('data-lamp');
  });

  it('is the only card tinted, the other tickets\' cards keeping the same look in every column', async () => {
    const ready = await started();
    await ready.user.keyboard('2');
    pushSessions(ready, sessionFixture({ id: 'turn', ticket: 'PROJ-1', state: { kind: 'yourTurn' } }));
    ['PROJ-2', 'PROJ-3', 'PROJ-4'].forEach((id) => expect(screen.getByRole('button', { name: new RegExp(id) })).not.toHaveAttribute('data-lamp'));
  });

  it('lists the sessions of the ticket under the cursor in the preview', async () => {
    const ready = await started();
    await ready.user.keyboard('2');
    pushSessions(ready, sessionFixture({ id: 'turn', ticket: 'PROJ-1', state: { kind: 'yourTurn' } }));
    expect(within(screen.getByRole('region', { name: 'Ticket' })).getByRole('button', { name: /attach/ })).toBeInTheDocument();
  });
});

describe('the Board tab with no ticket under the cursor', () => {
  const emptied = [
    ['has none', { kind: 'board', root: '/p', unreadable: [], groups: [] }],
    ['has only tickets that could not be read', boardFixture([], [{ id: 'PROJ-9', reason: 'no status label' }])],
  ] as const;

  it.each(emptied)('keeps five equal columns at 0 and none when the board %s', async (_name, read) => {
    const { user } = await started(undefined, read);
    await user.keyboard('2');
    const columns = ['DRAFT', 'TODO', 'IN PROGRESS', 'IN REVIEW', 'DONE'].map((name) => screen.getByRole('group', { name: new RegExp(name, 'i') }));
    expect(columns.map((column) => column.textContent)).toEqual(['draft0none', 'todo0none', 'in progress0none', 'in review0none', 'done0none']);
  });

  it.each(emptied)('keeps the preview pane saying no ticket is selected when the board %s', async (_name, read) => {
    const { user } = await started(undefined, read);
    await user.keyboard('2');
    expect(within(screen.getByRole('region', { name: 'Ticket' })).getByText('No ticket selected.')).toBeInTheDocument();
  });

  it.each(emptied)('keeps only the keys that act in the key bar when the board %s', async (_name, read) => {
    const { user } = await started(undefined, read);
    await user.keyboard('2');
    const keys = screen.getByRole('contentinfo');
    expect(keys).toHaveTextContent('c new draft');
    expect(keys).toHaveTextContent('1/2/3 tabs');
    expect(keys).not.toHaveTextContent('move');
    expect(keys).not.toHaveTextContent('go to ticket');
  });

  it('keeps the columns at 0 and none and says why while the issues cannot be read', async () => {
    const { user } = await started(undefined, { kind: 'unavailableBoard', reason: 'gh is not signed in' });
    await user.keyboard('2');
    expect(screen.getByText('gh is not signed in')).toBeInTheDocument();
    expect(screen.getByRole('group', { name: /todo/i })).toHaveTextContent('todo0none');
    expect(within(screen.getByRole('region', { name: 'Ticket' })).getByText('No ticket selected.')).toBeInTheDocument();
  });
});

describe('the Pull requests tab', () => {
  it('lists every open pull request with its CI glyph, number, title and head, failed first and drafts last', async () => {
    const { user } = await started();
    await user.keyboard('3');
    const rows = screen.getAllByRole('button').filter((button) => /#\d+/.test(button.textContent));
    expect(rows.map((row) => row.textContent)).toEqual(['✗#12Break itproj-2-second', '◌#11Wait for itchore-tidy', '✓#10Fix itproj-1-first', '#13Not yetproj-3-third']);
  });

  it('says there is no open pull request', async () => {
    const { user } = await started({ kind: 'openPullRequests', pullRequests: [] });
    await user.keyboard('3');
    expect(screen.getByText('No open pull requests.')).toBeInTheDocument();
  });

  it('offers no move or review key while no pull request is open, and the keys come back with one', async () => {
    const empty = await started({ kind: 'openPullRequests', pullRequests: [] });
    await empty.user.keyboard('3');
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('j/k move');
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('enter review');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('1/2/3 tabs');
    cleanup();
    const open = await started();
    await open.user.keyboard('3');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('j/k move');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('enter review');
  });

  it('says it is checking while review/watch has not answered', async () => {
    const { user } = await started({ kind: 'checkingPullRequests' });
    await user.keyboard('3');
    expect(screen.getByRole('status')).toHaveTextContent('Reading the pull requests');
  });

  it('shows the failure as an alert with Retry, never an empty list, and reads again on Retry', async () => {
    const { client, user } = await started({ kind: 'unavailablePullRequests', reason: 'gh is not signed in' });
    await user.keyboard('3');
    expect(screen.getByRole('alert')).toHaveTextContent('gh is not signed in');
    client.calls.length = 0;
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(client.calls).toEqual([{ method: 'review/watch', params: { root: '/p' } }]);
  });

  it('moves with j and k, opens the Review screen on enter, and returns to the tab and row on escape', async () => {
    const { client, user } = await started();
    await user.keyboard('3');
    await user.keyboard('j');
    expect(screen.getByRole('button', { name: /#11/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('{Enter}');
    await act(async () => {
      client.answerAll('review/pendingOf', { kind: 'noPendingReview' });
      client.answerAll('review/threadsOf', { kind: 'readThreads', threads: [] });
      client.answerAll('review/diffOf', { kind: 'readDiff', files: [] });
      await Promise.resolve();
    });
    await settle();
    expect(screen.getByRole('region', { name: 'PR #11 Wait for it' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(tab(/^Pull requests/)).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('button', { name: /#11/ })).toHaveAttribute('aria-current', 'true');
  });

  it('ends a review opened from it when another tab is clicked or keyed, the AZERTY digit row included', async () => {
    const { client, user } = await started();
    await user.keyboard('3');
    await user.click(screen.getByRole('button', { name: /#11/ }));
    await act(async () => {
      client.answerAll('review/pendingOf', { kind: 'noPendingReview' });
      client.answerAll('review/threadsOf', { kind: 'readThreads', threads: [] });
      client.answerAll('review/diffOf', { kind: 'readDiff', files: [] });
      await Promise.resolve();
    });
    await settle();
    await user.click(tab(/^Board/));
    expect(screen.queryByRole('region', { name: 'PR #11 Wait for it' })).not.toBeInTheDocument();
    expect(tab(/^Board/)).toHaveAttribute('aria-selected', 'true');
    fireEvent.keyDown(window, { key: '&', code: 'Digit1' });
    expect(tab(/^Base/)).toHaveAttribute('aria-selected', 'true');
  });

  it('opens the review of a pull request whose head is no ticket\'s branch when it is clicked', async () => {
    const { client, user } = await started();
    await user.keyboard('3');
    await user.click(screen.getByRole('button', { name: /#11/ }));
    expect(client.calls.map((call) => call.method)).toContain('review/diffOf');
  });
});

describe('the window tabs and a hosted session', () => {
  it('fold the dock to a strip of its tabs under the Board tab, and unfold it on Base', async () => {
    const ready = await started();
    await hostSession(ready);
    await ready.user.click(tab(/^Board/));
    expect(screen.getByRole('tablist', { name: 'hosted sessions' })).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'session terminal' })).not.toBeInTheDocument();
    await ready.user.keyboard('1');
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
  });

  it('keep the review\'s key bar and unfolded dock while a review opened from Pull requests is open', async () => {
    const ready = await started();
    await hostSession(ready);
    await ready.user.click(tab(/^Pull requests/));
    await ready.user.click(screen.getByRole('button', { name: /#11/ }));
    await act(async () => {
      ready.client.answerAll('review/pendingOf', { kind: 'noPendingReview' });
      ready.client.answerAll('review/threadsOf', { kind: 'readThreads', threads: [] });
      ready.client.answerAll('review/diffOf', { kind: 'readDiff', files: [] });
      await Promise.resolve();
    });
    await settle();
    expect(screen.queryByText('enter review'), 'the review draws its own panels, so the tab\'s keys no longer apply').not.toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'session terminal' }), 'the review keeps the full dock, so a session focused under it can be used').toBeInTheDocument();
  });

  it('go back to Base with the session\'s pane focused when its folded tab is clicked', async () => {
    const ready = await started();
    await hostSession(ready);
    await ready.user.keyboard('2');
    await ready.user.click(within(screen.getByRole('tablist', { name: 'hosted sessions' })).getByRole('tab'));
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
  });

  it('leave a digit typed in the pane to the session, switching no tab', async () => {
    const ready = await started();
    await hostSession(ready);
    const textarea = screen.getByRole('group', { name: 'session terminal' }).querySelector('textarea')!;
    const event = new KeyboardEvent('keydown', { key: '2', cancelable: true, bubbles: true });
    textarea.dispatchEvent(event);
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
    expect(event.defaultPrevented).toBe(false);
  });

  it('switch no tab on a digit while the pane has focus but the key is not typed in the terminal', async () => {
    const ready = await started();
    await hostSession(ready);
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
    await ready.user.keyboard('2');
    expect(tab('Base')).toHaveAttribute('aria-selected', 'true');
  });
});
