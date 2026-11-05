import { Terminal } from '@xterm/xterm';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { bytesOfText, RpcError, toBase64 } from '../protocol';
import { createStore } from '../shared/store';
import { fakeClient } from '../test/fakeClient';
import { Hosting, type HostingDeps } from './hosting';
import { StorageModeMemory } from './modeMemory';
import { CLAUDE_START, CLAUDE_STOPS_REPORTING_THE_MOUSE } from './recordings';
import { sessionFixture, sessionsFixture } from './fixtures';
import type { SessionsView } from '../protocol';

const encoded = (text: string) => toBase64(bytesOfText(text));
const written = (term: Terminal) => new Promise<void>((done) => term.write('', done));

const hostedIn = (...ids: string[]): SessionsView => ({
  ...sessionsFixture([]),
  hosted: ids.map((id) => ({ id, pid: 1, live: sessionFixture({ id }), busyOrWaiting: false, ticket: null })),
});

const hosting = (overrides: Partial<HostingDeps> = {}) => {
  const client = fakeClient();
  const sessions = createStore<SessionsView | undefined>(undefined);
  const memory = new StorageModeMemory(sessionStorage);
  const deps: HostingDeps = {
    client,
    memory,
    sessions,
    newTerminal: () => ({ terminal: new Terminal(), fit: { fit: vi.fn() } }),
    reserved: () => undefined,
    clipboard: { readText: () => Promise.resolve(''), writeText: () => Promise.resolve() },
    ...overrides,
  };
  return { client, sessions, memory, hosted: new Hosting(deps) };
};

describe('the hosted sessions', () => {
  beforeEach(() => sessionStorage.clear());

  it('starts an unlinked session at the pane size, and opens its tab', async () => {
    const { client, hosted } = hosting();
    const starting = hosted.startUnlinked('/p');
    expect(client.calls).toEqual([{ method: 'session/startUnlinked', params: { root: '/p', size: { width: 80, height: 24 } } }]);
    client.answerNext({ kind: 'startedHost', id: 's1' });
    await starting;
    expect(hosted.dock.get().tabs).toEqual(['s1']);
    expect(hosted.emulatorOf('s1')).toBeDefined();
  });

  it('opens no tab for a session the sidecar did not start, the refusal being its warning', async () => {
    const { client, hosted } = hosting();
    const starting = hosted.startUnlinked('/p');
    client.answerNext({ kind: 'notStartedHost' });
    await starting;
    expect(hosted.dock.get().tabs).toEqual([]);
  });

  it('starts a ticket\'s session with its verb', () => {
    const { client, hosted } = hosting();
    void hosted.startTicket('/p', 'PROJ-1', 'proj-1', 'START');
    expect(client.calls).toEqual([
      { method: 'session/startTicket', params: { root: '/p', ticket: 'PROJ-1', branch: 'proj-1', verb: 'START', size: { width: 80, height: 24 } } },
    ]);
  });

  it('resumes a past session from its row, leaving where to the sidecar', () => {
    const { client, hosted } = hosting();
    const sessions = { root: '/p', ticketIds: ['PROJ-1'] };
    void hosted.resumePast(sessions, 'old', '/p/wt', { kind: 'ticketListingPayload', branch: 'proj-1' });
    expect(client.calls).toEqual([
      {
        method: 'session/resumePast',
        params: { sessions, session: 'old', recordedDirectory: '/p/wt', listing: { kind: 'ticketListingPayload', branch: 'proj-1' }, size: { width: 80, height: 24 } },
      },
    ]);
  });

  it('keeps a failed start for the screen to show with a way to try again', async () => {
    const { client, hosted } = hosting();
    const starting = hosted.startUnlinked('/p');
    client.failNext(new RpcError(-32000, 'no java'));
    await starting;
    expect(hosted.failure.get()?.message).toBe('no java');
    const retried = hosted.failure.get()?.retry();
    client.answerNext({ kind: 'startedHost', id: 's1' });
    await retried;
    expect(hosted.failure.get()).toBeUndefined();
    expect(hosted.dock.get().tabs).toEqual(['s1']);
  });

  it('routes pty/output to the emulator of its own session, and to no other', async () => {
    const { client, sessions, hosted } = hosting();
    sessions.set(hostedIn('s1', 's2'));
    client.push('pty/output', { session: 's1', data: encoded('mine') });
    const term = hosted.emulatorOf('s1')?.terminal;
    await written(term!);
    expect(term?.buffer.active.getLine(0)?.translateToString(true)).toBe('mine');
    expect(hosted.emulatorOf('s2')?.terminal.buffer.active.getLine(0)?.translateToString(true)).toBe('');
  });

  it('keeps the output that comes before the start is answered', async () => {
    const { client, hosted } = hosting();
    const starting = hosted.startUnlinked('/p');
    client.push('pty/output', { session: 's1', data: encoded('early') });
    client.answerNext({ kind: 'startedHost', id: 's1' });
    await starting;
    const term = hosted.emulatorOf('s1')?.terminal;
    await written(term!);
    expect(term?.buffer.active.getLine(0)?.translateToString(true)).toBe('early');
  });

  it('keeps output that came before its tab when the list is read in between', async () => {
    const { client, sessions, hosted } = hosting();
    client.push('pty/output', { session: 's2', data: encoded('early') });
    sessions.set(hostedIn('s1'));
    const term = hosted.emulatorOf('s2')?.terminal;
    await written(term!);
    expect(term?.buffer.active.getLine(0)?.translateToString(true)).toBe('early');
  });

  it('takes its tabs from the hosted sessions the watch lists', () => {
    const { sessions, hosted } = hosting();
    sessions.set(hostedIn('s1', 's2'));
    expect(hosted.dock.get().tabs).toEqual(['s1', 's2']);
    sessions.set(hostedIn('s2'));
    expect(hosted.dock.get().tabs).toEqual(['s2']);
    expect(hosted.emulatorOf('s1')).toBeUndefined();
  });

  it('closes the tab of a session that exited, though the list still has it, and forgets its modes', async () => {
    const { client, sessions, memory, hosted } = hosting();
    sessions.set(hostedIn('s1', 's2'));
    client.push('pty/output', { session: 's1', data: encoded(CLAUDE_START) });
    await written(hosted.emulatorOf('s1')!.terminal);
    expect(memory.recall('s1')).toBeDefined();
    client.push('session/exited', { session: 's1', code: 3 });
    expect(hosted.dock.get().tabs).toEqual(['s2']);
    expect(memory.recall('s1')).toBeUndefined();
    sessions.set(hostedIn('s1', 's2'));
    expect(hosted.dock.get().tabs).toEqual(['s2']);
  });

  it('writes no late output of an exited session into a ghost emulator', () => {
    const { client, sessions, hosted } = hosting();
    sessions.set(hostedIn('s1'));
    client.push('session/exited', { session: 's1', code: 0 });
    client.push('pty/output', { session: 's1', data: encoded('late') });
    expect(hosted.emulatorOf('s1')).toBeUndefined();
  });

  it('restores a listed session\'s kept modes and asks for a repaint once its pane fits it, as a reloaded page does', async () => {
    const { client, sessions, memory, hosted } = hosting();
    sessions.set(hostedIn('s1'));
    client.push('pty/output', { session: 's1', data: encoded(CLAUDE_START) });
    await written(hosted.emulatorOf('s1')!.terminal);
    const reloaded = hosting({ memory });
    reloaded.sessions.set(hostedIn('s1'));
    const term = reloaded.hosted.emulatorOf('s1')!.terminal;
    await written(term);
    expect(term.modes.bracketedPasteMode).toBe(true);
    reloaded.hosted.emulatorOf('s1')!.attach(document.createElement('div'));
    expect(reloaded.client.sent.filter((sent) => sent.input === 'session/repaint')).toEqual([{ input: 'session/repaint', params: { session: 's1' } }]);
  });

  it('selects a tab, and moves between tabs as the reserved chords do', () => {
    const { sessions, hosted } = hosting();
    sessions.set(hostedIn('s1', 's2', 's3'));
    hosted.select('s2');
    expect(hosted.dock.get().selectedTab()).toBe('s2');
    expect(hosted.selectNext()).toBe(true);
    expect(hosted.dock.get().selectedTab()).toBe('s3');
    hosted.select('s1');
    expect(hosted.selectPrevious()).toBe(false);
    hosted.select('s2');
    expect(hosted.selectPrevious()).toBe(true);
    expect(hosted.dock.get().selectedTab()).toBe('s1');
  });

  it('tells whether the selected tab\'s claude reports the mouse, for the shift+drag hint', async () => {
    const { client, sessions, hosted } = hosting();
    sessions.set(hostedIn('s1', 's2'));
    expect(hosted.mouseReported.get()).toBe(false);
    client.push('pty/output', { session: 's1', data: encoded(CLAUDE_START) });
    await written(hosted.emulatorOf('s1')!.terminal);
    expect(hosted.mouseReported.get()).toBe(true);
    hosted.select('s2');
    expect(hosted.mouseReported.get()).toBe(false);
    hosted.select('s1');
    expect(hosted.mouseReported.get()).toBe(true);
    client.push('pty/output', { session: 's1', data: encoded(CLAUDE_STOPS_REPORTING_THE_MOUSE) });
    await written(hosted.emulatorOf('s1')!.terminal);
    expect(hosted.mouseReported.get()).toBe(false);
  });

  it('tells no mouse is reported once the tab closes', async () => {
    const { client, sessions, hosted } = hosting();
    sessions.set(hostedIn('s1'));
    client.push('pty/output', { session: 's1', data: encoded(CLAUDE_START) });
    await written(hosted.emulatorOf('s1')!.terminal);
    client.push('session/exited', { session: 's1', code: 0 });
    expect(hosted.mouseReported.get()).toBe(false);
  });

  it('says whether a start opened a tab', async () => {
    const { client, hosted } = hosting();
    const refused = hosted.startUnlinked('/p');
    client.answerNext({ kind: 'notStartedHost' });
    expect(await refused).toBe(false);
    const started = hosted.startUnlinked('/p');
    client.answerNext({ kind: 'startedHost', id: 's1' });
    expect(await started).toBe(true);
  });
});
