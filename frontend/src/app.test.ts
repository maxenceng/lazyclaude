import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './board/fixtures';
import { createApp } from './app';
import { FaultedConfiguration } from './keys/configurationState';
import { Connecting, Exited, Ready } from './protocol';
import { fakeClient } from './test/fakeClient';
import { testPlatform } from './testPlatform';

describe('the app', () => {
  it('reads nothing until the sidecar is ready', () => {
    const client = fakeClient(new Connecting());
    createApp(client, testPlatform()).start();
    expect(client.calls).toEqual([]);
  });

  it('loads the board when the sidecar becomes ready', () => {
    const client = fakeClient(new Connecting());
    createApp(client, testPlatform()).start();
    client.setStatus(new Ready());
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch']);
  });

  it('loads the board at once when the page reloaded onto a ready sidecar', () => {
    const client = fakeClient(new Ready());
    createApp(client, testPlatform()).start();
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch']);
  });

  it('watches the sessions of the project once the board shows tickets', async () => {
    const client = fakeClient(new Connecting());
    createApp(client, testPlatform()).start();
    client.setStatus(new Ready());
    client.answerNext(boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls[3]).toEqual({ method: 'session/watch', params: { root: '/p', ticketIds: ['PROJ-1', 'PROJ-2'] } });
  });

  it('loads the board again after a restart reaches ready', () => {
    const client = fakeClient(new Ready());
    createApp(client, testPlatform()).start();
    client.setStatus(new Exited(1, []));
    client.setStatus(new Ready());
    expect(client.calls.map((call) => call.method)).toEqual(['board/watch', 'session/configuration', 'warning/watch', 'board/watch', 'session/configuration', 'warning/watch']);
  });

  it('asks for the configuration once ready and reserves the chords it answers', async () => {
    const client = fakeClient(new Ready());
    const app = createApp(client, testPlatform());
    app.start();
    expect(app.reserved()).toEqual([]);
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
    expect(app.reserved()).toHaveLength(1);
  });

  it('holds the fault when the configuration cannot be read, reserving no chord', async () => {
    const client = fakeClient(new Ready());
    const app = createApp(client, testPlatform());
    app.start();
    client.answerNext(boardFixture([]));
    client.failNext(new Error('refused'));
    await Promise.resolve();
    await Promise.resolve();
    expect(app.configuration.get()).toEqual(new FaultedConfiguration('refused'));
    expect(app.reserved()).toEqual([]);
  });

  it('watches the sessions once a board that started empty fills through a change', async () => {
    const client = fakeClient(new Ready());
    createApp(client, testPlatform()).start();
    client.answerNext(boardFixture([]));
    await Promise.resolve();
    await Promise.resolve();
    client.push('board/changed', boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    expect(client.calls.map((call) => call.method)).toContain('session/watch');
  });

  it('watches the sessions after a retry that reads the board', async () => {
    const client = fakeClient(new Ready());
    const app = createApp(client, testPlatform());
    app.start();
    client.fail('board/watch', new Error('gh timed out'));
    client.answer('session/configuration', { chords: [], desktopNotifications: true, mouse: true });
    await Promise.resolve();
    await Promise.resolve();
    void app.load();
    client.answer('board/watch', boardFixture([ticketFixture({ id: 'PROJ-1' })]));
    await Promise.resolve();
    await Promise.resolve();
    expect(client.calls.map((call) => call.method)).toContain('session/watch');
  });
});
