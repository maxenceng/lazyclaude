import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LampColour } from '../design-system';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { BoardPane, TicketPane } from './BoardScreen';
import { BoardStore } from './boardStore';
import { boardFixture, ticketFixture } from './fixtures';
import { Selection } from './selection';

const setup = (markerOf: (ticket: string) => LampColour | undefined = () => undefined) => {
  const client = fakeClient();
  const board = new BoardStore(client);
  const selection = new Selection();
  const onOpen = vi.fn();
  const onLink = vi.fn();
  const loaded = board.load();
  render(
    <>
      <BoardPane board={board} selection={selection} focused markerOf={markerOf} onOpen={onOpen} onFocus={() => {}} onRetry={() => void board.load()} />
      <TicketPane board={board} selection={selection} focused={false} onLink={onLink} onFocus={() => {}} />
    </>,
  );
  return { client, board, selection, onOpen, onLink, loaded };
};

const tickets = boardFixture([
  ticketFixture({ id: 'PROJ-1', title: 'Shows the Board', status: 'IN_PROGRESS' }),
  ticketFixture({ id: 'PROJ-2', title: 'Runs a session', status: 'TODO', body: { kind: 'body', value: 'Details of the ticket.' } }),
  ticketFixture({ id: 'PROJ-3', title: 'Already shipped', status: 'DONE' }),
]);

const answer = async (client: ReturnType<typeof fakeClient>, loaded: Promise<void>, result: unknown) => {
  await act(async () => {
    client.answerNext(result);
    await loaded;
  });
};

describe('the Board', () => {
  it('says it is reading while the watch has not answered', () => {
    setup();
    expect(screen.getByRole('status')).toHaveTextContent('Reading the board');
  });

  it('says the board has no tickets when it is empty', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, boardFixture([]));
    expect(screen.getByText('No tickets on the board.')).toBeInTheDocument();
  });

  it('says why the board is unavailable, by its kind', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, { kind: 'unavailableBoard', root: '/p', reason: 'gh is not signed in' });
    expect(screen.getByText('The board cannot be read')).toBeInTheDocument();
    expect(screen.getByText('gh is not signed in')).toBeInTheDocument();
  });

  it('says why a directory is not a project, by its kind', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, { kind: 'noProject', reason: 'no .lazyclaude here' });
    expect(screen.getByText('This directory is not a project')).toBeInTheDocument();
    expect(screen.getByText('no .lazyclaude here')).toBeInTheDocument();
  });

  it('lists the tickets under their statuses, in-progress first and done last', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tickets);
    const rows = screen.getAllByRole('button').map((row) => row.textContent);
    expect(rows).toEqual(['PROJ-1Shows the Board', 'PROJ-2Runs a session', 'PROJ-3Already shipped']);
    expect(within(screen.getByRole('region', { name: 'Board' })).getByText('in progress')).toBeInTheDocument();
  });

  it('lists the ten latest closed done tickets under a heading that counts all of them', async () => {
    const { client, loaded } = setup();
    const done = Array.from({ length: 11 }, (_, at) => ticketFixture({ id: `PROJ-${String(at + 10)}`, title: `Closed ${String(at)}`, status: 'DONE' }));
    await answer(client, loaded, boardFixture(done));
    const board = within(screen.getByRole('region', { name: 'Board' }));
    expect(board.getByText('done').parentElement).toHaveTextContent('done11');
    expect(board.getByText('Closed 9')).toBeInTheDocument();
    expect(board.queryByText('Closed 10')).not.toBeInTheDocument();
  });

  it('shows the heading and a zero count of a status without tickets', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, { ...tickets, groups: [...tickets.groups, { status: 'IN_REVIEW', tickets: [] }] });
    expect(within(screen.getByRole('region', { name: 'Board' })).getByText('in review').parentElement).toHaveTextContent('in review0');
  });

  it('shows the unreadable tickets with their reasons under their own heading', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, { ...tickets, unreadable: [{ id: 'PROJ-9', reason: 'no title' }] });
    const board = within(screen.getByRole('region', { name: 'Board' }));
    expect(board.getByText('unreadable').parentElement).toHaveTextContent('unreadable1');
    expect(board.getByText('PROJ-9 — no title')).toBeInTheDocument();
  });

  it('shows no unreadable heading when none is unreadable', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tickets);
    expect(screen.queryByText('unreadable')).not.toBeInTheDocument();
  });

  it('typesets a ticket\'s body as markdown and hands its links to the page', async () => {
    const { client, loaded, onLink } = setup();
    await answer(client, loaded, boardFixture([ticketFixture({ id: 'PROJ-1', body: { kind: 'body', value: 'Read [the guide](docs/guide.md).' } })]));
    await userEvent.click(screen.getByRole('link', { name: 'the guide' }));
    expect(onLink).toHaveBeenCalledWith('docs/guide.md');
  });

  it('selects the ticket that heads the board and details it', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tickets);
    expect(screen.getByRole('button', { name: /PROJ-1/ })).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('heading', { level: 3, name: 'Shows the Board' })).toBeInTheDocument();
  });

  it('details the ticket text of the selected ticket', async () => {
    const { client, loaded, selection } = setup();
    await answer(client, loaded, tickets);
    act(() => selection.select('PROJ-2'));
    expect(screen.getByText('Details of the ticket.')).toBeInTheDocument();
  });

  it('shows a board/changed after the first board', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tickets);
    act(() => client.push('board/changed', boardFixture([ticketFixture({ id: 'PROJ-7', title: 'Arrived later' })])));
    expect(screen.getByRole('button', { name: /PROJ-7/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /PROJ-1/ })).not.toBeInTheDocument();
  });

  it('selects a ticket on the first click and opens it on the second', async () => {
    const { client, loaded, onOpen } = setup();
    await answer(client, loaded, tickets);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /PROJ-2/ }));
    expect(screen.getByRole('button', { name: /PROJ-2/ })).toHaveAttribute('aria-current', 'true');
    expect(onOpen).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: /PROJ-2/ }));
    expect(onOpen).toHaveBeenCalledWith('PROJ-2');
  });

  it('lights the lamp beside a ticket whose session waits on you', async () => {
    const { client, loaded } = setup((ticket) => (ticket === 'PROJ-2' ? LampColour.Permission : undefined));
    await answer(client, loaded, tickets);
    expect(within(screen.getByRole('button', { name: /PROJ-2/ })).getByRole('img')).toHaveAccessibleName('waiting for permission');
    expect(within(screen.getByRole('button', { name: /PROJ-1/ })).queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows a JSON-RPC error as an alert with its message and retries on Retry', async () => {
    const { client, loaded } = setup();
    await act(async () => {
      client.failNext(new RpcError(-32603, 'internal error'));
      await loaded;
    });
    expect(screen.getByRole('alert')).toHaveTextContent('internal error');
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(client.calls).toHaveLength(2);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the board');
  });
});
