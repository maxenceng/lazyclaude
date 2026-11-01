import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './board/fixtures';
import { createApp } from './app';
import { fakeClient } from './test/fakeClient';

describe('the app', () => {
  it('reads nothing until the sidecar is ready', () => {
    const client = fakeClient({ kind: 'connecting' });
    createApp(client).start();
    expect(client.calls).toEqual([]);
  });

  it('loads the board when the sidecar becomes ready', () => {
    const client = fakeClient({ kind: 'connecting' });
    createApp(client).start();
    client.setStatus({ kind: 'ready' });
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration']);
  });

  it('loads the board at once when the page reloaded onto a ready sidecar', () => {
    const client = fakeClient({ kind: 'ready' });
    createApp(client).start();
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration']);
  });

  it('watches the sessions of the project once the board shows tickets', async () => {
    const client = fakeClient({ kind: 'connecting' });
    createApp(client).start();
    client.setStatus({ kind: 'ready' });
    client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls[2]).toEqual({ method: 'session/watch', params: { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] } });
  });

  it('loads the board again after a restart reaches ready', () => {
    const client = fakeClient({ kind: 'ready' });
    createApp(client).start();
    client.setStatus({ kind: 'exited', code: 1, stderr: [] });
    client.setStatus({ kind: 'ready' });
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'board/watch', 'session/configuration']);
  });

  it('asks for the configuration once ready and reserves the chords it answers', async () => {
    const client = fakeClient({ kind: 'ready' });
    const app = createApp(client);
    app.start();
    expect(app.reserved.get()).toEqual([]);
    expect(client.calls.map((call) => call.method)).toContain('session/configuration');
    const index = client.calls.findIndex((call) => call.method === 'session/configuration');
    expect(client.calls[index]?.params).toEqual({});
    client.answerNext(boardFixture([]));
    client.answerNext({
      chords: [{ action: 'ZOOM', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 120 } } }],
      desktopNotifications: true,
      mouse: true,
    });
    await Promise.resolve();
    await Promise.resolve();
    expect(app.reserved.get()).toHaveLength(1);
  });

  it('reserves no chord when the configuration cannot be read', async () => {
    const client = fakeClient({ kind: 'ready' });
    const app = createApp(client);
    app.start();
    client.answerNext(boardFixture([]));
    client.failNext(new Error('refused'));
    await Promise.resolve();
    await Promise.resolve();
    expect(app.reserved.get()).toEqual([]);
  });
});
