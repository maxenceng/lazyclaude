import { describe, expect, it } from 'vitest';
import type { SessionLiveSessionView, SessionSessionsView } from '../protocol';
import { Store } from '../shared/store';
import { fakeClient } from '../test/fakeClient';
import { Closing } from './closing';
import { hostedFixture, sessionFixture, sessionsFixture } from './fixtures';
import { Dock } from './dock';
import type { Hosting } from './hosting';

type State = SessionLiveSessionView['state'];

const closingOf = (hosted: SessionSessionsView['hosted'], tabs: string[] = ['h1']) => {
  const client = fakeClient();
  const dock = new Store(new Dock(tabs).select(tabs[0] ?? ''));
  const sessions = new Store<SessionSessionsView | undefined>({ ...sessionsFixture([]), hosted });
  const closing = new Closing({ client, hosting: { dock } as Hosting, sessions });
  return { client, dock, sessions, closing };
};

const live = (state: State) => [hostedFixture('h1', { live: sessionFixture({ id: 'h1', state }) })];

describe('closing the selected session', () => {
  it('closes a session at its turn at once', () => {
    const { client, closing } = closingOf(live({ kind: 'yourTurn' }));
    closing.request();
    expect(client.sent).toEqual([{ input: 'session/close', params: { session: 'h1' } }]);
    expect(closing.isAsking()).toBe(false);
  });

  it('closes a session still starting at once', () => {
    const { client, closing } = closingOf([hostedFixture('h1')]);
    closing.request();
    expect(client.sent).toHaveLength(1);
  });

  it.each([{ kind: 'busy' }, { kind: 'unknown' }, { kind: 'waitingForPermission' }, { kind: 'waitingForInput' }] as State[])('asks first about a session in its %j state', (state) => {
    const { client, closing } = closingOf(live(state));
    closing.request();
    expect(closing.asking.get()).toBe('h1');
    expect(client.sent).toEqual([]);
  });

  it('sends the close once Close answers, and only then', () => {
    const { client, closing } = closingOf(live({ kind: 'busy' }));
    closing.request();
    closing.answer(true);
    expect(client.sent).toEqual([{ input: 'session/close', params: { session: 'h1' } }]);
    expect(closing.isAsking()).toBe(false);
  });

  it('keeps the session running on any other answer', () => {
    const { client, closing } = closingOf(live({ kind: 'busy' }));
    closing.request();
    closing.answer(false);
    expect(client.sent).toEqual([]);
    expect(closing.isAsking()).toBe(false);
  });

  it('drops the question when its session leaves the dock', () => {
    const { dock, closing } = closingOf(live({ kind: 'busy' }));
    closing.request();
    dock.set(dock.get().closed('h1'));
    expect(closing.isAsking()).toBe(false);
  });

  it('does nothing without a session', () => {
    const { client, closing } = closingOf([], []);
    closing.request();
    expect(client.sent).toEqual([]);
    expect(closing.isAsking()).toBe(false);
  });

  it('closes a session the listing does not show yet at once, as one starting', () => {
    const { closing } = closingOf([]);
    closing.request();
    expect(closing.isAsking()).toBe(false);
  });
});
