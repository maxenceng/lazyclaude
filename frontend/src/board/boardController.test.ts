import { describe, expect, it } from 'vitest';
import { type FakeClient, fakeClient } from '../test/fakeClient';
import { BoardController } from './boardController';
import { boardFixture, ticketFixture } from './fixtures';

const before = boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]);
const after = boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' }), ticketFixture({ id: 'PROJ-9' })]);

const flush = async () => {
  for (let turn = 0; turn < 4; turn += 1) {
    await Promise.resolve();
  }
};

const capturing = async ({ board, client }: { board: BoardController; client: FakeClient }) => {
  board.capture.open();
  board.capture.edit('A ticket');
  board.capture.create();
  client.answer('board/capture', { kind: 'capturedTicket', id: 'PROJ-9' });
  await flush();
};

const setup = async () => {
  const client = fakeClient();
  const board = new BoardController(client);
  const loading = board.load();
  client.answerNext(before);
  await loading;
  board.selection.select('PROJ-1');
  return { client, board };
};

describe('the new ticket selected after a capture', () => {
  it('is selected by the first board push after the captured answer', async () => {
    const { client, board } = await setup();
    await capturing({ client, board });
    expect(board.selection.selected.get()).toBe('PROJ-1');
    client.push('board/changed', after);
    expect(board.selection.selected.get()).toBe('PROJ-9');
  });

  it('is spent by that push even when the ticket is not in it, so a later push does not move the cursor', async () => {
    const { client, board } = await setup();
    await capturing({ client, board });
    client.push('board/changed', before);
    client.push('board/changed', after);
    expect(board.selection.selected.get()).toBe('PROJ-1');
  });

  it('is dropped when the user moves the selection before the push', async () => {
    const { client, board } = await setup();
    await capturing({ client, board });
    board.move(1);
    client.push('board/changed', after);
    expect(board.selection.selected.get()).toBe('PROJ-2');
  });

  it('is not made by a push that arrived before the answer', async () => {
    const { client, board } = await setup();
    board.capture.open();
    board.capture.edit('A ticket');
    board.capture.create();
    client.push('board/changed', after);
    client.answer('board/capture', { kind: 'capturedTicket', id: 'PROJ-9' });
    await flush();
    expect(board.selection.selected.get()).toBe('PROJ-1');
  });

  it('is dropped when the board is read afresh', async () => {
    const { client, board } = await setup();
    await capturing({ client, board });
    const reload = board.load();
    client.answerNext(after);
    await reload;
    client.push('board/changed', after);
    expect(board.selection.selected.get()).toBe('PROJ-1');
  });

  it('is not made by a refusal', async () => {
    const { client, board } = await setup();
    board.capture.open();
    board.capture.create();
    client.answer('board/capture', { kind: 'refusedTicketCapture', reason: 'no' });
    await flush();
    client.push('board/changed', after);
    expect(board.selection.selected.get()).toBe('PROJ-1');
  });
});
