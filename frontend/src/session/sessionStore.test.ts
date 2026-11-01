import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { sessionFixture, sessionsFixture } from './fixtures';
import { createSessionStore } from './sessionStore';

describe('the session store', () => {
  it('watches the sessions of the project and its tickets', () => {
    const client = fakeClient();
    void createSessionStore(client).watch('/p', ['PROJ-1']);
    expect(client.calls).toEqual([{ method: 'session/watch', params: { root: '/p', ticketIds: ['PROJ-1'] } }]);
  });

  it('shows the sessions the watch answers, then each session/changed', async () => {
    const client = fakeClient();
    const store = createSessionStore(client);
    const watching = store.watch('/p', []);
    expect(store.sessions.get()).toBeUndefined();
    const first = sessionsFixture([sessionFixture({ id: 'a' })]);
    client.answerNext(first);
    await watching;
    expect(store.sessions.get()).toBe(first);
    const next = sessionsFixture([]);
    client.push('session/changed', next);
    expect(store.sessions.get()).toBe(next);
  });

  it('shows no lamps when the watch is refused', async () => {
    const client = fakeClient();
    const store = createSessionStore(client);
    const watching = store.watch('/p', []);
    client.failNext(new RpcError(-32603, 'boom'));
    await watching;
    expect(store.sessions.get()).toBeUndefined();
  });
});
