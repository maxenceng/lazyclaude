import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { createApp } from './app';
import { boardFixture, ticketFixture } from './board/fixtures';
import { sessionFixture, sessionsFixture } from './session/fixtures';
import { fakeClient } from './test/fakeClient';

const board = boardFixture([
  ticketFixture({ id: 'PROJ-1', title: 'First', status: 'IN_PROGRESS' }),
  ticketFixture({ id: 'PROJ-2', title: 'Second', status: 'TODO' }),
]);

async function readyApp() {
  const client = fakeClient({ kind: 'ready' });
  const app = createApp(client);
  app.start();
  const onOpen = vi.fn();
  render(<App app={app} packaged={false} onOpen={onOpen} />);
  await act(async () => {
    client.answerNext(board);
    await Promise.resolve();
  });
  return { client, onOpen };
}

describe('the window', () => {
  it('takes j and k to move the cursor and enter to open the ticket under it', async () => {
    const { onOpen } = await readyApp();
    const user = userEvent.setup();
    await user.keyboard('j');
    expect(screen.getByRole('button', { name: /PROJ-2/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('k');
    expect(screen.getByRole('button', { name: /PROJ-1/ })).toHaveAttribute('aria-current', 'true');
    await user.keyboard('{Enter}');
    expect(onOpen).toHaveBeenCalledWith('PROJ-1');
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
    const client = fakeClient({ kind: 'exited', code: 1, stderr: ['boom'] });
    render(<App app={createApp(client)} packaged={false} onOpen={() => {}} />);
    expect(screen.getByRole('alert')).toHaveTextContent('The backend stopped');
  });
});
