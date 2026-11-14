import { describe, expect, it } from 'vitest';
import { fakeClient } from '../test/fakeClient';
import { BoardController } from './boardController';
import { boardFixture } from './fixtures';

describe('the project of an empty board', () => {
  it('is known, so a first draft can be captured, though no ticket is on it', async () => {
    const client = fakeClient();
    const board = new BoardController(client, () => Promise.resolve(undefined));
    const loading = board.load();
    client.answerNext(boardFixture([]));
    await loading;
    expect(board.shown()).toBeUndefined();
    expect(board.root()).toBe('/p');
  });
});
