import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { createApp } from './app';
import { boardFixture, ticketFixture } from './board/fixtures';
import { Exited, Ready } from './protocol';
import { bytesOfText, toBase64 } from './protocol';
import { hostedFixture, pastFixture, sessionFixture, sessionsFixture } from './session/fixtures';
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
  mouse: true,
};

const settle = () => act(async () => {
  await new Promise((resolve) => setTimeout(resolve, 0));
});

const readyApp = async () => {
  const client = fakeClient(new Ready());
  const platform = testPlatform();
  const app = createApp(client, platform);
  app.start();
  render(<App app={app} packaged={false} />);
  await act(async () => {
    client.answer('board/watch', board);
    client.answer('session/configuration', configuration);
    client.answer('warning/watch', { kind: 'noWarning' });
    await Promise.resolve();
  });
  await settle();
  return { client, app, platform, user: userEvent.setup() };
};

type Started = Awaited<ReturnType<typeof readyApp>>;

const startedSession = async ({ client, user }: Started, key = 'N') => {
  await user.keyboard(key);
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
    render(<App app={createApp(client, testPlatform())} packaged={false} />);
    expect(screen.getByRole('alert')).toHaveTextContent('The backend stopped');
  });

  it('shows a configuration it cannot read as an alert whose retry reads it again', async () => {
    const client = fakeClient(new Ready());
    const app = createApp(client, testPlatform());
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
    expect(screen.getByRole('tab', { selected: true })).toHaveTextContent('starting');
    expect(screen.getByRole('group', { name: 'session terminal' })).toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('ctrl+shift+c copy');
    expect(keyBar()).toHaveTextContent('ctrl+shift+v paste');
  });

  it('starts an unlinked session on n for a ticket with no verb', async () => {
    const ready = await readyApp();
    await startedSession(ready, 'n');
    expect(ready.client.calls.map((call) => call.method)).toContain('session/startUnlinked');
  });

  it('offers the verbs of a ticket on n and starts the chosen one in its worktree', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
    expect(screen.getByRole('button', { name: /start$/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /review/ })).toBeInTheDocument();
    expect(keyBar()).toHaveTextContent('enter start');
    await ready.user.keyboard('j');
    await ready.user.keyboard('{Enter}');
    expect(ready.client.calls.find((call) => call.method === 'session/startTicket')?.params).toEqual({
      root: '/p',
      ticket: 'PROJ-2',
      branch: 'proj-2-second',
      verb: 'REVIEW',
      size: { width: 80, height: 24 },
    });
  });

  it('leaves the verb picker on escape, starting nothing', async () => {
    const ready = await readyApp();
    await ready.user.keyboard('j');
    await ready.user.keyboard('n');
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
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
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

  it('resumes a past session of the ticket in the ticket\'s worktree', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', sessionsFixture([], { past: [pastFixture({ id: 'old', title: 'Earlier work', ticket: 'PROJ-1' })] })));
    await ready.user.keyboard('{Enter}');
    await ready.user.click(screen.getByRole('button', { name: /Earlier work/ }));
    expect(ready.client.calls.find((call) => call.method === 'session/resumeInTicketWorktree')?.params).toEqual({
      session: 'old',
      recordedDirectory: '/p/wt',
      root: '/p',
      branch: 'proj-1-first',
      size: { width: 80, height: 24 },
    });
  });

  it('resumes a past session linked to no ticket where it was recorded', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', sessionsFixture([], { past: [pastFixture({ id: 'free', title: 'Loose end' })] })));
    await ready.user.keyboard('{Enter}');
    await ready.user.click(screen.getByRole('button', { name: /Loose end/ }));
    expect(ready.client.calls.find((call) => call.method === 'session/resume')?.params).toEqual({
      root: '/p',
      session: 'free',
      recordedDirectory: '/p/wt',
      size: { width: 80, height: 24 },
    });
  });

  it('focuses the tab of a hosted session of the ticket on enter', async () => {
    const ready = await readyApp();
    act(() => {
      ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' })] });
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
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('claude exited with code 2');
    expect(keyBar()).toHaveTextContent('enter open');
  });

  it('takes its tabs from the hosted sessions the watch lists, with their lamp', async () => {
    const { client } = await readyApp();
    const waiting = sessionFixture({ id: 'live1', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'edit' } }, ticket: 'PROJ-1' });
    act(() => client.push('session/changed', { ...sessionsFixture([waiting]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1', live: waiting })] }));
    const tab = screen.getByRole('tab');
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
    await ready.user.click(screen.getByRole('tab', { name: /PROJ-2/ }));
    expect(screen.getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('changes tab by the reserved chords, as the TUI does, the previous one and the next', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' }), hostedFixture('h2', { ticket: 'PROJ-2' })] }));
    await ready.user.click(screen.getByRole('tab', { name: /PROJ-1/ }));
    await ready.user.keyboard('{Alt>}l{/Alt}');
    expect(screen.getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
    await ready.user.keyboard('{Alt>}h{/Alt}');
    expect(screen.getByRole('tab', { name: /PROJ-1/ })).toHaveAttribute('aria-selected', 'true');
  });

  it('takes a reserved chord typed into the terminal and keeps it from claude', async () => {
    const ready = await readyApp();
    act(() => ready.client.push('session/changed', { ...sessionsFixture([]), hosted: [hostedFixture('h1', { ticket: 'PROJ-1' }), hostedFixture('h2', { ticket: 'PROJ-2' })] }));
    await ready.user.click(screen.getByRole('tab', { name: /PROJ-1/ }));
    ready.client.sent.length = 0;
    const textarea = screen.getByRole('group', { name: 'session terminal' }).querySelector('textarea');
    textarea?.focus();
    await ready.user.keyboard('{Alt>}l{/Alt}');
    expect(screen.getByRole('tab', { name: /PROJ-2/ })).toHaveAttribute('aria-selected', 'true');
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

describe('what the sidecar is told is watched', () => {
  const watching = (client: Started['client']) => client.sent.filter((sent) => sent.input === 'session/watching').map((sent) => sent.params);

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
