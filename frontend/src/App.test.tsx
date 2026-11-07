import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { AppModel } from './app';
import { boardFixture, ticketFixture } from './board/fixtures';
import { Focus, WindowTab } from './layout';
import { Exited, Ready } from './protocol';
import { bytesOfText, toBase64 } from './protocol';
import { folderFixture, readFixture, treeFixture } from './docs/fixtures';
import { hostedFixture, pastFixture, sessionFixture, sessionsFixture, ticketSessionsFixture } from './session/fixtures';
import { Dock } from './session/dock';
import { CLAUDE_START, CLAUDE_STOPS_REPORTING_THE_MOUSE } from './session/recordings';
import { fakeClient } from './test/fakeClient';
import { testPlatform } from './testPlatform';

const board = boardFixture([
  ticketFixture({ id: 'PROJ-1', title: 'First', status: 'IN_PROGRESS', branch: 'proj-1-first' }),
  ticketFixture({ id: 'PROJ-2', title: 'Second', status: 'TODO', branch: 'proj-2-second', verbs: ['START', 'REVIEW'] }),
]);

const configuration = {
  chords: [
    { action: 'FOCUS_LEFT', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 104 } } },
    { action: 'FOCUS_RIGHT', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 108 } } },
    { action: 'ZOOM', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 122 } } },
  ],
  desktopNotifications: true,
  ticketPlugin: 'ddd-workflow',
  statusVerbs: [],
  mouse: true,
};

const settle = () => act(async () => {
  await new Promise((resolve) => setTimeout(resolve, 0));
});

const readyApp = async (packaged = false) => {
  const client = fakeClient(new Ready());
  const platform = testPlatform();
  const app = new AppModel(client, platform);
  app.start();
  render(<App app={app} packaged={packaged} />);
  await act(async () => {
    client.answer('board/watch', board);
    client.answer('session/configuration', configuration);
    client.answer('warning/watch', { kind: 'noWarning' });
    await Promise.resolve();
  });
  await settle();
  await act(async () => {
    client.answer('docs/watch', treeFixture([]));
    await Promise.resolve();
  });
  return { client, app, platform, user: userEvent.setup() };
};

type Started = Awaited<ReturnType<typeof readyApp>>;

const newSessionAnswered = (client: Started['client'], answer: unknown) =>
  act(async () => {
    client.answer('session/newSessionFor', answer);
    await settle();
  });

const startedSession = async ({ client, user }: Started, key = 'N') => {
  await user.keyboard(key);
  await newSessionAnswered(client, { kind: 'unlinkedStart' });
  await act(async () => {
    client.answer('session/startUnlinked', { kind: 'startedHost', id: 'h1' });
    client.answer('session/startTicket', { kind: 'startedHost', id: 'h1' });
    await Promise.resolve();
  });
  await settle();
};

const output = (client: Started['client'], session: string, text: string) =>
  act(async () => {
    client.push('pty/output', { session, data: toBase64(bytesOfText(text)) });
    await settle();
  });

const dockTabs = () => screen.getByRole('tablist', { name: 'hosted sessions' });

const keyBar = () => screen.getByRole('contentinfo');

describe('the window', () => {
  it('takes j and k to move the cursor', async () => {
    const { user } = await readyApp();
    await user.keyboard('j');
    expect(screen.getByRole('button', { name: /PROJ-2/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('k');
    expect(screen.getByRole('button', { name: /PROJ-1/ })).toHaveAttribute('aria-current', 'true');
  });

  it('puts a lamp tile on the status line for each live session, numbered by its ticket', async () => {
    const { client } = await readyApp();
    act(() =>
      client.push(
        'session/changed',
        sessionsFixture([sessionFixture({ state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-2' })]),
      ),
    );
    expect(screen.getAllByRole('img', { name: 'PROJ-2: waiting for input' }).length).toBeGreaterThan(0);
  });

  it('shows the connection screen instead of the board while the backend is down', () => {
    const client = fakeClient(new Exited(1, ['boom']));
    render(<App app={new AppModel(client, testPlatform())} packaged={false} />);
    expect(screen.getByRole('alert')).toHaveTextContent('The backend stopped');
  });

  it('shows a configuration it cannot read as an alert whose retry reads it again', async () => {
    const client = fakeClient(new Ready());
    const app = new AppModel(client, testPlatform());
    app.start();
    render(<App app={app} packaged={false} />);
    await act(async () => {
      client.answer('board/watch', board);
      client.fail('session/configuration', new Error('the configuration is unreadable'));
      await Promise.resolve();
    });
    expect(screen.getByRole('alert')).toHaveTextContent('the configuration is unreadable');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }));
    expect(client.calls.filter((call) => call.method === 'session/configuration')).toHaveLength(2);
  });
});

const docsRead = (started: Started) => {
  act(() => {
    started.client.push(
      'docs/changed',
      treeFixture(
        [readFixture('README.md', '# Readme\n\nSee [setup](guide/intro.md#setup) and [nowhere](guide/intro.md#nowhere).')],
        [folderFixture('guide', [readFixture('guide/intro.md', '# Intro\n\n## Setup\n\nText.')])],
      ),
    );
  });
};

describe('the Docs panel in the window', () => {
  it('shows the tree under the Board and the document the main view shows once a row is pressed', async () => {
    const started = await readyApp();
    docsRead(started);
    expect(within(screen.getByRole('region', { name: 'Docs' })).getAllByRole('button').map((row) => row.textContent)).toEqual(['README.md', 'guide1', 'intro.md']);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    expect(screen.getByRole('region', { name: 'Document' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Readme' })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Ticket' })).toBeNull();
  });

  it('moves over the tree with j and k while it has focus, and gives the main view back to the ticket on the Board', async () => {
    const started = await readyApp();
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    await started.user.keyboard('jj');
    expect(screen.getByRole('button', { name: 'intro.md' })).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('heading', { level: 1, name: 'Intro' })).toBeInTheDocument();
    await started.user.keyboard('{Escape}');
    expect(screen.getByRole('region', { name: 'Ticket' })).toBeInTheDocument();
  });

  it('follows a link to a heading, and says on the status line when the heading is not there', async () => {
    const started = await readyApp();
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    await started.user.click(screen.getByRole('link', { name: 'nowhere' }));
    expect(screen.getByRole('button', { name: 'intro.md' })).toHaveAttribute('aria-current', 'true');
    expect(screen.getByText(/No heading "nowhere" in guide\/intro.md; opened at its top\./)).toBeInTheDocument();
  });

  it('opens a web link through the system browser and shows its refusal as a notice', async () => {
    const started = await readyApp();
    started.platform.opener.failure = new Error('no browser');
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    await act(async () => {
      started.app.docs.follow('https://example.com');
      await settle();
    });
    expect(started.platform.opener.urls).toEqual(['https://example.com']);
    expect(screen.getByText(/Could not open https:\/\/example.com: no browser/)).toBeInTheDocument();
  });

  it('shows a link\'s notice beside the backend\'s warning, never over it, and clears it with the next ticket selected', async () => {
    const started = await readyApp();
    docsRead(started);
    act(() => started.client.push('warning/changed', { kind: 'warning', text: 'docs: watching by polling', count: 2 }));
    act(() => started.app.docs.followFromTicket('#notes'));
    expect(screen.getByText("Did not follow #notes: a ticket's own headings are not followed.")).toBeInTheDocument();
    expect(screen.getByText('⚠ 2 · docs: watching by polling')).toBeInTheDocument();
    await started.user.keyboard('{Escape}j');
    expect(screen.queryByText("Did not follow #notes: a ticket's own headings are not followed.")).toBeNull();
    expect(screen.getByText('⚠ 2 · docs: watching by polling')).toBeInTheDocument();
  });
});

const pressInPane = (key: string, init: KeyboardEventInit = {}) => {
  const textarea = screen.getByRole('group', { name: 'session terminal' }).querySelector('textarea')!;
  const event = new KeyboardEvent('keydown', { key, cancelable: true, bubbles: true, ...init });
  textarea.dispatchEvent(event);
  return event;
};

const KEY_BAR_COLUMNS = 80;

const keyBarWidth = () => {
  const keys = Array.from(keyBar().children).map((key) => key.textContent);
  return keys.join('').length + (keys.length - 1) * 3;
};

describe('a shown document and the Docs panel', () => {
  it('carries a close button that gives the main view back to the ticket, focus staying on the Docs panel', async () => {
    const started = await readyApp();
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    await started.user.click(within(screen.getByRole('region', { name: 'Document' })).getByRole('button', { name: 'Close document' }));
    expect(screen.getByRole('region', { name: 'Ticket' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Close document' })).not.toBeInTheDocument();
    expect(started.app.layout.get().focus).toBe(Focus.Docs);
  });

  it('shows a document again when the cursor moves after it was closed', async () => {
    const started = await readyApp();
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    await started.user.click(screen.getByRole('button', { name: 'Close document' }));
    await started.user.keyboard('j');
    expect(screen.getByRole('region', { name: 'Document' })).toBeInTheDocument();
  });

  it('shows esc close on the key bar while a document is shown, and esc back to the Board once it is closed', async () => {
    const started = await readyApp();
    docsRead(started);
    await started.user.click(screen.getByRole('button', { name: 'README.md' }));
    expect(keyBar()).toHaveTextContent('esc close');
    await started.user.keyboard('{Escape}');
    expect(keyBar()).not.toHaveTextContent('esc close');
    expect(started.app.layout.get().focus).toBe(Focus.Docs);
  });
});

describe('the dock minimised', () => {
  it('has no minimise button and ignores M while no session is hosted', async () => {
    const ready = await readyApp();
    expect(screen.queryByRole('button', { name: 'Minimise dock' })).not.toBeInTheDocument();
    await ready.user.keyboard('M');
    expect(ready.app.layout.get().dockMinimised).toBe(false);
    expect(keyBar()).not.toHaveTextContent('M minimise');
  });

  it('folds to the strip of its tabs on its button, and a tab click restores it with that tab selected and its pane focused', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    await ready.user.click(screen.getByRole('button', { name: 'Minimise dock' }));
    expect(screen.queryByRole('group', { name: 'session terminal' })).not.toBeInTheDocument();
    expect(screen.getByRole('tablist', { name: 'hosted sessions' })).toBeInTheDocument();
    expect(ready.app.layout.get().focus, 'minimising the focused dock moves focus to the ticket\'s sessions').toBe(Focus.Sessions);
    await ready.user.click(within(screen.getByRole('tablist', { name: 'hosted sessions' })).getByRole('tab'));
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
  });

  it('folds on M from a panel and unfolds on its restore button, the key bar saying which', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.focusOn(Focus.Board));
    expect(keyBar()).toHaveTextContent('M minimise');
    await ready.user.keyboard('M');
    expect(screen.queryByRole('group', { name: 'session terminal' })).not.toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('M restore');
    await ready.user.click(screen.getByRole('button', { name: 'Restore dock' }));
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
  });

  it('leaves M typed in a hosted session\'s pane to the session', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    const event = pressInPane('M');
    expect(event.defaultPrevented).toBe(false);
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
    expect(keyBar()).not.toHaveTextContent('M minimise');
  });

  it('leaves the no-dock layout when the last session exits, and opens the next dock unfolded', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    ready.app.focusOn(Focus.Board);
    await ready.user.keyboard('M');
    await act(async () => {
      ready.client.push('session/exited', { session: 'h1', code: 0 });
      await Promise.resolve();
    });
    expect(screen.queryByRole('tablist', { name: 'hosted sessions' })).not.toBeInTheDocument();
    expect(ready.app.layout.get().dockMinimised).toBe(false);
  });

  it('restores on a click on a tab that was not selected, with that tab selected', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.keyboard('N');
    await newSessionAnswered(ready.client, { kind: 'unlinkedStart' });
    await act(async () => {
      ready.client.answer('session/startUnlinked', { kind: 'startedHost', id: 'h2' });
      await Promise.resolve();
    });
    await settle();
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.keyboard('M');
    const [first] = within(dockTabs()).getAllByRole('tab');
    expect(first).toHaveAttribute('aria-selected', 'false');
    await ready.user.click(first!);
    expect(within(dockTabs()).getAllByRole('tab')[0]).toHaveAttribute('aria-selected', 'true');
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
  });

  it('restores a minimised dock when a new session starts', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.keyboard('M');
    await ready.user.keyboard('N');
    await newSessionAnswered(ready.client, { kind: 'unlinkedStart' });
    await act(async () => {
      ready.client.answer('session/startUnlinked', { kind: 'startedHost', id: 'h2' });
      await Promise.resolve();
    });
    await settle();
    expect(ready.app.layout.get().dockMinimised).toBe(false);
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
  });

  it('offers no dock key on the Board and Pull requests tabs, where M and m do nothing', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.chooseTab(WindowTab.Board));
    expect(keyBar()).not.toHaveTextContent('M minimise');
    act(() => ready.app.chooseTab(WindowTab.PullRequests));
    expect(keyBar()).not.toHaveTextContent('M minimise');
    act(() => ready.app.chooseTab(WindowTab.Base));
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.keyboard('M');
    act(() => ready.app.chooseTab(WindowTab.Board));
    expect(keyBar()).not.toHaveTextContent('M restore');
  });

  it('keeps every key bar within 80 columns', async () => {
    const ready = await readyApp();
    docsRead(ready);
    await startedSession(ready);
    const widths: number[] = [];
    [Focus.Board, Focus.Docs, Focus.Sessions, Focus.Dock].forEach((focus) => {
      act(() => ready.app.focusOn(focus));
      widths.push(keyBarWidth());
    });
    await ready.user.click(screen.getByRole('button', { name: 'README.md' }));
    widths.push(keyBarWidth());
    act(() => ready.app.hideMainView());
    [Focus.Board, Focus.Docs, Focus.Dock].forEach((focus) => {
      act(() => ready.app.focusOn(focus));
      widths.push(keyBarWidth());
    });
    act(() => ready.app.toggleDockMinimised());
    [Focus.Board, Focus.Docs, Focus.Sessions].forEach((focus) => {
      act(() => ready.app.focusOn(focus));
      widths.push(keyBarWidth());
    });
    [WindowTab.Board, WindowTab.PullRequests].forEach((tab) => {
      act(() => ready.app.chooseTab(tab));
      widths.push(keyBarWidth());
    });
    expect(Math.max(...widths)).toBeLessThanOrEqual(KEY_BAR_COLUMNS);
  });
});

describe('the ticket hidden', () => {
  it('has no hide button while no session is hosted', async () => {
    await readyApp();
    expect(screen.getByRole('region', { name: 'Ticket' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hide ticket' })).not.toBeInTheDocument();
  });

  it('hides on its button so the dock takes the column, the key bar offering m to show it again', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.click(screen.getByRole('button', { name: 'Hide ticket' }));
    expect(screen.queryByRole('region', { name: 'Ticket' })).not.toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('m ticket');
    await ready.user.keyboard('m');
    expect(screen.getByRole('region', { name: 'Ticket' })).toBeInTheDocument();
    expect(keyBar()).not.toHaveTextContent('m ticket');
  });

  it('unfolds a minimised dock when the ticket hides', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.app.focusOn(Focus.Board));
    await ready.user.keyboard('M');
    await ready.user.click(screen.getByRole('button', { name: 'Hide ticket' }));
    expect(ready.app.layout.get().dockMinimised).toBe(false);
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
  });
});

describe('starting a session from the board', () => {
  it('opens the ticket\'s sessions on enter, with the row that starts one', async () => {
    const { user } = await readyApp();
    await user.keyboard('{Enter}');
    expect(keyBar()).toHaveTextContent('esc back');
    expect(screen.getByRole('button', { name: /new session/ })).toHaveAttribute('aria-current', 'true');
  });

  it('starts an unlinked session on N at the pane size, and shows its tab and terminal', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    expect(ready.client.calls.find((call) => call.method === 'session/startUnlinked')?.params).toEqual({ root: '/p', size: { width: 80, height: 24 } });
    expect(within(dockTabs()).getByRole('tab', { selected: true })).toHaveTextContent('starting');
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('ctrl+shift+c copy');
    expect(keyBar()).toHaveTextContent('ctrl+shift+v paste');
  });

  it('asks session what n opens, the merge being unknown to the window', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
    expect(ready.client.calls.find((call) => call.method === 'session/newSessionFor')?.params).toEqual({
      sessions: { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] },
      ticket: 'PROJ-2',
      verbs: ['START', 'REVIEW'],
      merged: 'UNKNOWN',
    });
  });

  it('starts an unlinked session on n when session answers that the ticket allows no verb', async () => {
    const ready = await readyApp();
    await startedSession(ready, 'n');
    expect(ready.client.calls.map((call) => call.method)).toContain('session/startUnlinked');
  });

  it('offers the verbs of a ticket on n with the cursor on the one session preselects, and starts it in its worktree', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
    await newSessionAnswered(ready.client, { kind: 'verbChoice', verbs: ['START', 'REVIEW'], preselected: 'REVIEW' });
    expect(screen.getByRole('button', { name: /start$/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /review/ })).toHaveAttribute('aria-current', 'true');
    expect(keyBar()).toHaveTextContent('enter start');
    await ready.user.keyboard('{Enter}');
    expect(ready.client.calls.find((call) => call.method === 'session/startTicket')?.params).toEqual({
      root: '/p',
      ticket: 'PROJ-2',
      branch: 'proj-2-second',
      verb: 'REVIEW',
      size: { width: 80, height: 24 },
    });
  });

  it('scrolls the preselected verb into view when the picker opens below a long ticket', async () => {
    const scrolled = vi.spyOn(Element.prototype, 'scrollIntoView');
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
    await newSessionAnswered(ready.client, { kind: 'verbChoice', verbs: ['START', 'REVIEW'], preselected: 'REVIEW' });
    expect(scrolled.mock.contexts).toContain(screen.getByRole('button', { name: /review/ }));
    scrolled.mockRestore();
  });

  it('leaves the verb picker on escape, starting nothing', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
    await newSessionAnswered(ready.client, { kind: 'verbChoice', verbs: ['START', 'REVIEW'], preselected: 'START' });
    await ready.user.keyboard('{Escape}');
    expect(screen.queryByRole('button', { name: /review/ })).not.toBeInTheDocument();
    expect(ready.client.calls.map((call) => call.method)).not.toContain('session/startTicket');
  });

  it('shows no tab for a refused start, and the sidecar\'s warning on the status line', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('N');
    await act(async () => {
      ready.client.answer('session/startUnlinked', { kind: 'notStartedHost' });
      ready.client.push('warning/changed', { kind: 'warning', text: 'hosted session: the directory is gone.', count: 1 });
      await Promise.resolve();
    });
    expect(screen.queryByRole('tablist', { name: 'hosted sessions' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('⚠ hosted session: the directory is gone.');
  });

  it('counts the warnings raised, as the TUI does', async () => {
    const { client } = await readyApp();
    act(() => client.push('warning/changed', { kind: 'warning', text: 'resume: gone', count: 3 }));
    expect(screen.getByRole('status')).toHaveTextContent('⚠ 3 · resume: gone');
  });

  it('shows a start that failed as an alert whose retry starts it again', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('N');
    await act(async () => {
      ready.client.fail('session/startUnlinked', new Error('no java'));
      await Promise.resolve();
    });
    expect(screen.getByRole('alert')).toHaveTextContent('no java');
    await ready.user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(ready.client.calls.filter((call) => call.method === 'session/startUnlinked')).toHaveLength(2);
  });

  it('shows n failing to reach session as an alert whose retry asks again', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('n');
    await act(async () => {
      ready.client.fail('session/newSessionFor', new Error('sidecar gone'));
      await settle();
    });
    expect(screen.getByRole('alert')).toHaveTextContent('sidecar gone');
    await ready.user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(ready.client.calls.filter((call) => call.method === 'session/newSessionFor')).toHaveLength(2);
  });

  it('resumes a past session of the ticket in the ticket\'s worktree', async () => {
    const ready = await readyApp();
    const past = pastFixture({ id: 'old', title: 'Earlier work', ticket: 'PROJ-1' });
    act(() => ready.client.push('session/changed', sessionsFixture([], { past: [past], tickets: [ticketSessionsFixture('PROJ-1', { past: [past] })] })));
    await ready.user.keyboard('{Enter}');
    await ready.user.click(screen.getByRole('button', { name: /Earlier work/ }));
    expect(ready.client.calls.find((call) => call.method === 'session/resumePast')?.params).toEqual({
      sessions: { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] },
      session: 'old',
      recordedDirectory: '/p/wt',
      listing: { kind: 'ticketListingPayload', branch: 'proj-1-first' },
      size: { width: 80, height: 24 },
    });
  });

  it('lists no past session linked to no ticket under the selected ticket', async () => {
    const ready = await readyApp();
    const free = pastFixture({ id: 'free', title: 'Loose end' });
    act(() => ready.client.push('session/changed', sessionsFixture([], { past: [free], unlinked: { past: [free] } })));
    await ready.user.keyboard('{Enter}');
    expect(screen.queryByRole('button', { name: /Loose end/ })).toBeNull();
  });

  it('focuses the tab of a hosted session of the ticket on enter', async () => {
    const ready = await readyApp();
    act(() => {
      const hosted = hostedFixture('h1', { ticket: 'PROJ-1' });
      ready.client.push('session/changed', sessionsFixture([], { hosted: [hosted], tickets: [ticketSessionsFixture('PROJ-1', { hosted: [hosted] })] }));
    });
    await ready.user.keyboard('{Enter}');
    await ready.user.click(screen.getByRole('button', { name: /starting/ }));
    expect(keyBar()).toHaveTextContent('ctrl+shift+c copy');
  });
});

describe('a hosted session\'s pane', () => {
  it('closes the tab when the session exits, and a non-zero code is the sidecar\'s warning on the status line', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    await act(async () => {
      ready.client.push('session/exited', { session: 'h1', code: 2 });
      ready.client.push('warning/changed', { kind: 'warning', text: 'hosted session: claude exited with code 2.', count: 1 });
      await Promise.resolve();
    });
    expect(screen.queryByRole('tablist', { name: 'hosted sessions' })).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('claude exited with code 2');
    expect(keyBar()).toHaveTextContent('enter open');
  });

  it('takes its tabs from the hosted sessions the watch lists, with their lamp', async () => {
    const { client } = await readyApp();
    const waiting = sessionFixture({ id: 'live1', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'edit' } }, ticket: 'PROJ-1' });
    act(() => client.push('session/changed', { ...sessionsFixture([waiting]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1', live: waiting })] }));
    const tab = within(dockTabs()).getByRole('tab');
    expect(tab).toHaveTextContent('PROJ-1');
    expect(within(tab).getByRole('img', { name: 'waiting for permission' })).toBeInTheDocument();
  });

  it('shows the shift+drag hint while the focused pane\'s claude reports the mouse, and not after', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    expect(keyBar()).not.toHaveTextContent('shift+drag select');
    await output(ready.client, 'h1', CLAUDE_START);
    expect(keyBar()).toHaveTextContent('shift+drag select');
    await output(ready.client, 'h1', CLAUDE_STOPS_REPORTING_THE_MOUSE);
    expect(keyBar()).not.toHaveTextContent('shift+drag select');
  });

  it('changes tab by a click', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' }), hostedFixture('h2', { ticket: 'PROJ-2' })] }));
    await ready.user.click(within(dockTabs()).getByRole('tab', { name: /PROJ-2/ }));
    expect(within(dockTabs()).getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('changes tab by the reserved chords, as the TUI does, the previous one and the next', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' }), hostedFixture('h2', { ticket: 'PROJ-2' })] }));
    await ready.user.click(within(dockTabs()).getByRole('tab', { name: /PROJ-1/ }));
    await ready.user.keyboard('{Alt>}l{/Alt}');
    expect(within(dockTabs()).getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
    await ready.user.keyboard('{Alt>}h{/Alt}');
    expect(within(dockTabs()).getByRole('tab', { name: /PROJ-1/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('takes a reserved chord typed into the terminal and keeps it from claude', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' }), hostedFixture('h2', { ticket: 'PROJ-2' })] }));
    await ready.user.click(within(dockTabs()).getByRole('tab', { name: /PROJ-1/ }));
    ready.client.sent.length = 0;
    const textarea = screen.getByRole('group', { name: 'session terminal' }).querySelector('textarea');
    textarea?.focus();
    await ready.user.keyboard('{Alt>}l{/Alt}');
    expect(within(dockTabs()).getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
    expect(ready.client.sent.filter((sent) => sent.input === 'pty/input')).toEqual([]);
  });

  it('types into the focused pane as pty/input bytes', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    ready.client.sent.length = 0;
    await ready.user.keyboard('j');
    const typed = ready.client.sent.filter((sent) => sent.input === 'pty/input');
    expect(typed).toHaveLength(1);
    expect(typed[0]?.params).toEqual({ session: 'h1', data: toBase64(bytesOfText('j')) });
  });
});

const watching = (client: Started['client']) => client.sent.filter((sent) => sent.input === 'session/watching').map((sent) => sent.params);

describe('what the sidecar is told is watched', () => {
  it('is none while no pane is focused', async () => {
    const { client } = await readyApp();
    expect(watching(client).at(-1)).toEqual({ kind: 'noWatchedSessionPayload' });
  });

  it('is the selected session while its pane and the window both have focus', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.platform.setFocused(true));
    expect(watching(ready.client).at(-1)).toEqual({ kind: 'watchedSessionPayload', session: 'h1' });
  });

  it('is none once the window loses focus, and the session again once it regains it', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.platform.setFocused(true));
    act(() => ready.platform.setFocused(false));
    expect(watching(ready.client).at(-1)).toEqual({ kind: 'noWatchedSessionPayload' });
    act(() => ready.platform.setFocused(true));
    expect(watching(ready.client).at(-1)).toEqual({ kind: 'watchedSessionPayload', session: 'h1' });
  });

  it('is none once focus leaves the pane for the board', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    act(() => ready.platform.setFocused(true));
    await ready.user.keyboard('{Alt>}h{/Alt}');
    expect(watching(ready.client).at(-1)).toEqual({ kind: 'noWatchedSessionPayload' });
  });

  it('is told again when the sidecar restarts', async () => {
    const ready = await readyApp();
    const before = watching(ready.client).length;
    act(() => {
      ready.client.setStatus(new Exited(1, []));
      ready.client.setStatus(new Ready());
    });
    expect(watching(ready.client).length).toBeGreaterThan(before);
  });
});

describe('the dock\'s layout', () => {
  it('zooms the dock over the window on the reserved chord and leaves it again', async () => {
    const ready = await readyApp();
    await startedSession(ready);
    await ready.user.keyboard('{Alt>}z{/Alt}');
    expect(screen.queryByRole('heading', { name: 'Board' })).not.toBeInTheDocument();
    await ready.user.keyboard('{Alt>}z{/Alt}');
    expect(screen.getByRole('heading', { name: 'Board' })).toBeInTheDocument();
  });
});


describe('a packaged build', () => {
  it('swallows F5 and Ctrl+R in a pane, where xterm.js sends them to claude', async () => {
    const ready = await readyApp(true);
    await startedSession(ready);
    expect(pressInPane('F5', { keyCode: 116 }).defaultPrevented).toBe(true);
    expect(pressInPane('r', { ctrlKey: true, keyCode: 82 }).defaultPrevented).toBe(true);
  });

  it('shows no context menu over a pane', async () => {
    const ready = await readyApp(true);
    await startedSession(ready);
    const event = new MouseEvent('contextmenu', { cancelable: true, bubbles: true });
    screen.getByRole('group', { name: 'session terminal' }).dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
  });
});

const reviewDiff = {
  kind: 'readDiff',
  files: [
    {
      path: 'src/a.ts',
      status: { kind: 'modifiedFile' },
      hunks: [{ header: '@@ -1 +1,2 @@', lines: [{ kind: 'contextLine', content: 'one', oldNumber: 1, newNumber: 1 }, { kind: 'addedLine', content: 'deux', newNumber: 2 }] }],
    },
  ],
};

const summaryAnswered = async (ready: Awaited<ReturnType<typeof readyApp>>, summary = '') => {
  await act(async () => {
    ready.client.answerAll('review/summaryToSubmit', summary);
    ready.client.answerAll('review/commentsInDiff', { threads: [], pending: [], underLines: [], listed: [], lastOnLines: [] });
    await settle();
  });
};

const PULL_REQUEST = { number: 7, title: 'Add the thing', head: 'proj-1-first', draft: false, checks: [] };

const reviewOpened = async (answers: { pending?: unknown; threads?: unknown; diff?: unknown } = {}) => {
  const ready = await readyApp();
  await act(async () => {
    ready.client.answer('review/watch', {
      kind: 'openPullRequests',
      pullRequests: [{ number: 7, title: 'Add the thing', head: 'proj-1-first', draft: false, checks: [] }],
    });
    await settle();
  });
  await ready.user.keyboard('p');
  await act(async () => {
    ready.client.answer('review/pullRequestOn', { kind: 'headedPullRequest', pullRequest: PULL_REQUEST });
    await settle();
  });
  await act(async () => {
    ready.client.answer('review/pendingOf', answers.pending ?? { kind: 'noPendingReview' });
    ready.client.answer('review/threadsOf', answers.threads ?? { kind: 'readThreads', threads: [] });
    ready.client.answer('review/diffOf', answers.diff ?? reviewDiff);
    await settle();
  });
  await act(async () => {
    ready.client.answerAll('review/commentsInDiff', { threads: [], pending: [], underLines: [], listed: [], lastOnLines: [] });
    ready.client.answerAll('review/summaryToSubmit', '');
    await settle();
  });
  return ready;
};

describe('the Review screen', () => {
  it('minimises the dock on M, focus staying on the diff, and keeps the review open when a strip tab restores it', async () => {
    const ready = await reviewOpened();
    act(() => ready.app.hosting.dock.set(new Dock().opened('h1')));
    act(() => ready.app.focusOn(Focus.Diff));
    await ready.user.keyboard('M');
    expect(ready.app.layout.get().dockMinimised).toBe(true);
    expect(ready.app.layout.get().focus).toBe(Focus.Diff);
    await ready.user.click(within(dockTabs()).getByRole('tab'));
    expect(ready.app.review.isOpen(), 'a click on the minimised strip restores the dock, it does not leave the review').toBe(true);
    expect(screen.getByRole('region', { name: 'Files' })).toBeInTheDocument();
    expect(ready.app.layout.get().focus).toBe(Focus.Dock);
  });

  it('moves a focused dock\'s focus to the diff when M minimises it in a review', async () => {
    const ready = await reviewOpened();
    act(() => ready.app.hosting.dock.set(new Dock().opened('h1')));
    act(() => ready.app.focusOn(Focus.Dock));
    act(() => ready.app.toggleDockMinimised());
    expect(ready.app.layout.get().focus).toBe(Focus.Diff);
  });

  it('opens on p for a ticket whose branch heads an open pull request, with Files, Comments and Review beside the diff', async () => {
    await reviewOpened();
    expect(screen.queryByRole('heading', { name: 'Board' })).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Files' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Comments' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Review' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'PR #7 Add the thing' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'src/a.ts' })).toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('c comment');
  });

  it('is shown loading before the reads answer', async () => {
    const ready = await readyApp();
    await act(async () => {
      ready.client.answer('review/watch', {
        kind: 'openPullRequests',
        pullRequests: [{ number: 7, title: 'Add the thing', head: 'proj-1-first', draft: false, checks: [] }],
      });
      await settle();
    });
    await ready.user.keyboard('p');
    await act(async () => {
      ready.client.answer('review/pullRequestOn', { kind: 'headedPullRequest', pullRequest: PULL_REQUEST });
      await settle();
    });
    expect(screen.getByRole('region', { name: 'PR #7 Add the thing' })).toHaveTextContent('Reading the diff');
  });

  it('does not open for a ticket without an open pull request', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('p');
    expect(screen.getByRole('heading', { name: 'Board' })).toBeInTheDocument();
  });

  it('goes back to the Board on escape', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('{Escape}');
    expect(screen.getByRole('heading', { name: 'Board' })).toBeInTheDocument();
  });

  it('shows the refusal of a range across hunks on the status line', async () => {
    const ready = await reviewOpened();
    const lines = screen.getAllByRole('button').filter((button) => button.hasAttribute('data-line'));
    fireEvent.mouseDown(lines[0]!);
    fireEvent.mouseUp(lines[1]!);
    await act(async () => {
      ready.client.answer('review/anchorOf', { kind: 'refusedAnchor', reason: 'a range cannot span two hunks' });
      await settle();
    });
    expect(screen.getByRole('banner')).toHaveTextContent('a range cannot span two hunks');
  });

  it('writes a comment from c: the popup opens, ctrl+s saves it, and the pending review the sidecar answers is shown', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('c');
    await act(async () => {
      ready.client.answer('review/anchorOf', { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 1 } });
      await settle();
    });
    await ready.user.keyboard('Rename this');
    await ready.user.keyboard('{Control>}s{/Control}');
    expect(ready.client.calls.at(-1)).toEqual({
      method: 'review/comment',
      params: { root: '/p', number: 7, path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 1 }, body: 'Rename this' },
    });
    await act(async () => {
      ready.client.answer('review/comment', { kind: 'writtenReview', pending: { kind: 'openPendingReview', summary: '', comments: [], headMove: { kind: 'heldHead' } } });
      await settle();
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Review' })).toHaveTextContent('0 pending');
  });

  it('shows why a comment was not saved, in the popup', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('c');
    await act(async () => {
      ready.client.answer('review/anchorOf', { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 1 } });
      await settle();
    });
    await ready.user.keyboard('x{Control>}s{/Control}');
    await act(async () => {
      ready.client.answer('review/comment', { kind: 'refusedWrite', reason: 'GitHub is down' });
      await settle();
    });
    expect(screen.getByRole('dialog')).toHaveTextContent('not saved (GitHub is down)');
  });

  it('submits the review as a Comment: S, the summary, ctrl+s, y', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('S');
    await summaryAnswered(ready);
    await ready.user.keyboard('Looks good');
    await ready.user.keyboard('{Control>}s{/Control}');
    await summaryAnswered(ready, 'Looks good');
    expect(screen.getByRole('dialog', { name: 'Submit review as Comment?' })).toBeInTheDocument();
    await ready.user.keyboard('y');
    await summaryAnswered(ready, 'Looks good');
    expect(ready.client.calls.at(-1)).toEqual({ method: 'review/submit', params: { root: '/p', number: 7, summary: 'Looks good' } });
    await act(async () => {
      ready.client.answer('review/submit', { kind: 'writtenReview', pending: { kind: 'noPendingReview' } });
      await settle();
    });
    expect(screen.getByRole('heading', { name: 'Board' })).toBeInTheDocument();
  });

  it('shows why the submit was refused on the status line, and stays in the review', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('S');
    await summaryAnswered(ready);
    await ready.user.keyboard('{Control>}s{/Control}');
    await summaryAnswered(ready);
    await ready.user.keyboard('y');
    await summaryAnswered(ready);
    await act(async () => {
      ready.client.answer('review/submit', { kind: 'refusedWrite', reason: 'nothing to submit' });
      await settle();
    });
    expect(screen.getByRole('banner')).toHaveTextContent('not submitted (nothing to submit)');
    expect(screen.getByRole('region', { name: 'PR #7 Add the thing' })).toBeInTheDocument();
  });

  it('takes j and k, in the diff, as the cursor, and h to Files', async () => {
    const ready = await reviewOpened();
    await ready.user.keyboard('j');
    const lines = screen.getAllByRole('button').filter((button) => button.hasAttribute('data-line'));
    expect(lines[1]).toHaveAttribute('aria-current', 'true');
    await ready.user.keyboard('h');
    await ready.user.keyboard('{Escape}');
    expect(screen.getByRole('heading', { name: 'Board' })).toBeInTheDocument();
  });
});

const captureBox = () => screen.getByRole('textbox', { name: 'Ticket title' });

const answerCapture = (started: Started, result: unknown) =>
  act(async () => {
    started.client.answer('board/capture', result);
    await settle();
  });

describe('capturing a draft ticket', () => {
  it('opens a titled popup on c with the focus on its input and only its own keys on the key bar', async () => {
    const { user } = await readyApp();
    await user.keyboard('c');
    expect(screen.getByRole('dialog', { name: 'New ticket' })).toBeInTheDocument();
    expect(captureBox()).toHaveFocus();
    expect(Array.from(keyBar().children).map((key) => key.textContent)).toEqual(['enter create', 'esc cancel']);
  });

  it("opens on the Board pane's New ticket button, with the focus on its input", async () => {
    const { user, app } = await readyApp();
    act(() => app.focusOn(Focus.Docs));
    await user.click(screen.getByRole('button', { name: 'New ticket' }));
    expect(screen.getByRole('dialog', { name: 'New ticket' })).toBeInTheDocument();
    expect(captureBox()).toHaveFocus();
    expect(app.layout.get().focus).toBe(Focus.Board);
  });

  it("keeps a click behind the popup from reaching a ticket or a window tab, and from taking the input's focus", async () => {
    const { user, app } = await readyApp();
    await user.keyboard('c');
    await user.click(screen.getByRole('button', { name: /PROJ-2/ }));
    await user.click(screen.getByRole('tab', { name: /^Board/ }));
    expect(app.board.selection.selected.get()).toBeUndefined();
    expect(screen.getByRole('tab', { name: 'Base' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('dialog', { name: 'New ticket' })).toBeInTheDocument();
    expect(captureBox()).toHaveFocus();
  });

  it('lets clicks reach the window again once the popup has closed', async () => {
    const { user, app } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('{Escape}');
    await user.click(screen.getByRole('button', { name: /PROJ-2/ }));
    expect(app.board.selection.selected.get()).toBe('PROJ-2');
  });

  it('does not open from a panel other than the Board', async () => {
    const { user, app } = await readyApp();
    act(() => app.focusOn(Focus.Docs));
    await user.keyboard('c');
    expect(screen.queryByRole('dialog', { name: 'New ticket' })).toBeNull();
  });

  it('leaves every typed key to the input, the window taking none of them', async () => {
    const { user, app } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('cRqjk Ab{Backspace}');
    expect(captureBox()).toHaveValue('cRqjk A');
    expect(app.board.selection.selected.get()).toBeUndefined();
    expect(screen.getAllByRole('dialog')).toHaveLength(1);
  });

  it('leaves word deletion, select all and paste to the webview by not preventing their default', async () => {
    const { user } = await readyApp(true);
    await user.keyboard('c');
    const chords: KeyboardEventInit[] = [{ key: 'Backspace', ctrlKey: true }, { key: 'a', ctrlKey: true }, { key: 'v', ctrlKey: true }, { key: 'ArrowLeft', ctrlKey: true, shiftKey: true }];
    const prevented = chords.map((init) => {
      const event = new KeyboardEvent('keydown', { ...init, cancelable: true, bubbles: true });
      captureBox().dispatchEvent(event);
      return event.defaultPrevented;
    });
    expect(prevented).toEqual([false, false, false, false]);
  });

  it('leaves an esc that ends an input method composition to the composition, keeping the popup', async () => {
    const { user } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('A title');
    act(() => {
      captureBox().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', isComposing: true, bubbles: true, cancelable: true }));
    });
    expect(screen.getByRole('dialog', { name: 'New ticket' })).toBeInTheDocument();
    expect(captureBox()).toHaveValue('A title');
  });

  it('closes on esc without sending anything', async () => {
    const { user, client } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('A title{Escape}');
    expect(screen.queryByRole('dialog', { name: 'New ticket' })).toBeNull();
    expect(client.calls.filter((call) => call.method === 'board/capture')).toHaveLength(0);
  });

  it('sends the title once on enter, says it is creating, and ignores a second enter and esc', async () => {
    const { user, client } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('A title{Enter}{Enter}{Escape}');
    expect(client.calls.filter((call) => call.method === 'board/capture')).toEqual([{ method: 'board/capture', params: { root: '/p', title: 'A title' } }]);
    expect(within(screen.getByRole('dialog', { name: 'New ticket' })).getByRole('status')).toHaveTextContent('creating…');
    expect(screen.getByRole('dialog', { name: 'New ticket' })).toBeInTheDocument();
  });

  it('keeps the popup and its text on a refusal, says why, and sends again only on another enter', async () => {
    const started = await readyApp();
    await started.user.keyboard('c');
    await started.user.keyboard('A title{Enter}');
    await answerCapture(started, { kind: 'refusedTicketCapture', reason: 'gh is not signed in' });
    expect(screen.getByRole('alert')).toHaveTextContent('⚠ gh is not signed in');
    expect(captureBox()).toHaveValue('A title');
    expect(started.client.calls.filter((call) => call.method === 'board/capture')).toHaveLength(1);
    await started.user.keyboard('{Enter}');
    expect(started.client.calls.filter((call) => call.method === 'board/capture')).toHaveLength(2);
  });

  it('keeps the popup and says why when the call itself fails', async () => {
    const started = await readyApp();
    await started.user.keyboard('c');
    await started.user.keyboard('A title{Enter}');
    await act(async () => {
      started.client.fail('board/capture', new Error('the sidecar is not running'));
      await settle();
    });
    expect(screen.getByRole('alert')).toHaveTextContent('⚠ the sidecar is not running');
    expect(captureBox()).toHaveValue('A title');
  });

  it('closes on a captured answer and selects the new ticket on the board the answer carries', async () => {
    const started = await readyApp();
    await started.user.keyboard('c');
    await started.user.keyboard('A title{Enter}');
    const read = boardFixture([...board.groups.flatMap((group) => group.tickets), ticketFixture({ id: 'PROJ-3', title: 'A title' })]);
    await answerCapture(started, { kind: 'capturedOnBoard', id: 'PROJ-3', board: read });
    expect(screen.queryByRole('dialog', { name: 'New ticket' })).toBeNull();
    expect(screen.getByRole('button', { name: /PROJ-3/ })).toHaveAttribute('aria-current', 'true');
  });

  it('answers enter and esc from the window when focus has left the input, and nothing else', async () => {
    const started = await readyApp();
    await started.user.keyboard('c');
    await started.user.keyboard('A title');
    act(() => (document.activeElement as HTMLElement).blur());
    await started.user.keyboard('j');
    expect(started.app.board.selection.selected.get()).toBeUndefined();
    await started.user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'New ticket' })).toBeNull();
  });
});

describe('quitting the window', () => {
  it('closes the window at once on q while no hosted session is busy or waiting', async () => {
    const { platform, user } = await readyApp();
    await user.keyboard('q');
    expect(platform.closes).toBe(1);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('asks before closing while a hosted session is busy or waiting, and stays on esc', async () => {
    const { client, platform, user } = await readyApp();
    act(() => client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { busyOrWaiting: true }), hostedFixture('h2')] }));
    await user.keyboard('q');
    expect(within(screen.getByRole('dialog', { name: 'Quit lazyclaude?' })).getByText('1 hosted session is busy or waiting, and quitting ends it.')).toBeInTheDocument();
    expect(platform.closes).toBe(0);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(platform.closes).toBe(0);
  });

  it('closes the window once y answers the question', async () => {
    const { client, platform, user } = await readyApp();
    act(() => client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { busyOrWaiting: true }), hostedFixture('h2', { busyOrWaiting: true })] }));
    await user.keyboard('q');
    expect(screen.getByText('2 hosted sessions are busy or waiting, and quitting ends them.')).toBeInTheDocument();
    await user.keyboard('y');
    expect(platform.closes).toBe(1);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('answers the question with its buttons as with its keys', async () => {
    const { client, platform, user } = await readyApp();
    act(() => client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { busyOrWaiting: true })] }));
    await user.keyboard('q');
    await user.click(screen.getByRole('button', { name: 'Stay' }));
    expect(platform.closes).toBe(0);
    await user.keyboard('q');
    await user.click(screen.getByRole('button', { name: 'Quit' }));
    expect(platform.closes).toBe(1);
  });

  it('quits from the status line button as q does', async () => {
    const { platform, user } = await readyApp();
    await user.click(screen.getByRole('button', { name: 'Quit lazyclaude' }));
    expect(platform.closes).toBe(1);
  });

  it('shows a window that refused to close as a notice', async () => {
    const { platform, user } = await readyApp();
    platform.closeRefusal = 'not allowed';
    await user.keyboard('q');
    await settle();
    expect(screen.getByText(/the window did not close \(not allowed\)/)).toBeInTheDocument();
  });

  it('types q into the capture popup rather than quitting', async () => {
    const { platform, user } = await readyApp();
    await user.keyboard('c');
    await user.keyboard('q');
    expect(platform.closes).toBe(0);
  });
});
