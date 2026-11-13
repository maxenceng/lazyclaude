import { describe, expect, it } from 'vitest';
import type { BoardBoardAfterCaptureView } from '../protocol';
import { type FakeClient, fakeClient } from '../test/fakeClient';
import { BoardController } from './boardController';
import { boardFixture, ticketFixture } from './fixtures';

const before = boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]);
const after = boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' }), ticketFixture({ id: 'PROJ-9' })]);

const flush = async () => {
  await Promise.resolve();
  await Promise.resolve();
};

const capturing = async ({ board, client }: { board: BoardController; client: FakeClient }, read: BoardBoardAfterCaptureView) => {
  board.capture.open();
  board.capture.edit('A ticket');
  board.capture.create();
  client.answer('board/capture', { kind: 'capturedOnBoard', id: 'PROJ-9', board: read });
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
  it('is selected on the board the captured answer carries', async () => {
    const started = await setup();
    await capturing(started, after);
    expect(started.board.ticketIds()).toContain('PROJ-9');
    expect(started.board.selection.selected.get()).toBe('PROJ-9');
  });

  it('is not moved off by a stale push after the answer', async () => {
    const started = await setup();
    await capturing(started, after);
    started.client.push('board/changed', after);
    expect(started.board.selection.selected.get()).toBe('PROJ-9');
  });

  it('keeps the board shown and the cursor where it was when the answer carries no board', async () => {
    const started = await setup();
    await capturing(started, { kind: 'noBoardAfterCapture' });
    expect(started.board.ticketIds()).toEqual(['PROJ-1', 'PROJ-2']);
    expect(started.board.selection.selected.get()).toBe('PROJ-1');
  });

  it('is not selected by a push that arrived before the answer', async () => {
    const started = await setup();
    started.board.capture.open();
    started.board.capture.edit('A ticket');
    started.board.capture.create();
    started.client.push('board/changed', after);
    expect(started.board.selection.selected.get()).toBe('PROJ-1');
  });

  it('is not made by a refusal', async () => {
    const started = await setup();
    started.board.capture.open();
    started.board.capture.create();
    started.client.answer('board/capture', { kind: 'refusedTicketCapture', reason: 'no' });
    await flush();
    started.client.push('board/changed', after);
    expect(started.board.selection.selected.get()).toBe('PROJ-1');
  });
});

describe('the project of an empty board', () => {
  it('is known, so a first draft can be captured, though no ticket is on it', async () => {
    const client = fakeClient();
    const board = new BoardController(client);
    const loading = board.load();
    client.answerNext(boardFixture([]));
    await loading;
    expect(board.shown()).toBeUndefined();
    expect(board.root()).toBe('/p');
  });
});
