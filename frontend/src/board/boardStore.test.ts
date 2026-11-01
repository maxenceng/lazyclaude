import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { boardFixture, ticketFixture } from './fixtures';
import { createBoardStore } from './boardStore';

describe('the board store', () => {
  it('is loading until the watch answers', () => {
    const store = createBoardStore(fakeClient());
    void store.load();
    expect(store.state.get()).toEqual({ kind: 'loading' });
  });

  it('asks board/watch from the project directory the sidecar runs in', () => {
    const client = fakeClient();
    void createBoardStore(client).load();
    expect(client.calls).toEqual([{ method: 'board/watch', params: { from: '.' } }]);
  });

  it('shows the board the watch answers', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    const view = boardFixture([ticketFixture({ id: 'PROJ-1' })]);
    client.answerNext(view);
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'populated', view });
  });

  it('shows each board/changed after it', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext(boardFixture([]));
    await loaded;
    const next = boardFixture([ticketFixture({ id: 'PROJ-2' })]);
    client.push('board/changed', next);
    expect(store.state.get()).toEqual({ kind: 'populated', view: next });
  });

  it('is empty when the board has no ticket', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext(boardFixture([]));
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'empty' });
  });

  it('is empty when its statuses are all without a ticket', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext({ ...boardFixture([]), groups: [{ status: 'TODO', tickets: [] }] });
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'empty' });
  });

  it('is not empty when only unreadable tickets are on it', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext({ ...boardFixture([]), unreadable: [{ id: 'PROJ-9', reason: 'no title' }] });
    await loaded;
    expect(store.state.get().kind).toBe('populated');
  });

  it('shows the reason of an unavailable board', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext({ kind: 'unavailableBoard', root: '/p', reason: 'gh is not signed in' });
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'unavailable', cause: 'unavailableBoard', reason: 'gh is not signed in' });
  });

  it('shows the reason a directory belongs to no project', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.answerNext({ kind: 'noProject', reason: 'not a lazyclaude project' });
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'unavailable', cause: 'noProject', reason: 'not a lazyclaude project' });
  });

  it('shows a JSON-RPC error as a fault with its message', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const loaded = store.load();
    client.failNext(new RpcError(-32603, 'internal error'));
    await loaded;
    expect(store.state.get()).toEqual({ kind: 'fault', message: 'internal error' });
  });

  it('loads again after a fault when asked', async () => {
    const client = fakeClient();
    const store = createBoardStore(client);
    const first = store.load();
    client.failNext(new RpcError(-32603, 'internal error'));
    await first;
    void store.load();
    expect(store.state.get()).toEqual({ kind: 'loading' });
    expect(client.calls).toHaveLength(2);
  });
});
