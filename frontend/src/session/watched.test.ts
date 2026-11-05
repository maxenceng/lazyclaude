import { describe, expect, it } from 'vitest';
import { fakeClient } from '../test/fakeClient';
import { WatchingReporter, watchedPayload } from './watched';

describe('the session the person is watching', () => {
  it('is the selected pane\'s session while the pane and the window both have focus', () => {
    expect(watchedPayload('s1', true, true)).toEqual({ kind: 'watchedSessionPayload', session: 's1' });
  });

  it('is none when the pane lacks focus', () => {
    expect(watchedPayload('s1', false, true)).toEqual({ kind: 'noWatchedSessionPayload' });
  });

  it('is none when the window lacks focus', () => {
    expect(watchedPayload('s1', true, false)).toEqual({ kind: 'noWatchedSessionPayload' });
  });

  it('is none when no pane is selected', () => {
    expect(watchedPayload(undefined, true, true)).toEqual({ kind: 'noWatchedSessionPayload' });
  });
});

describe('telling the sidecar the watched session', () => {
  it('sends a change once, and the same answer again only after forgetting what was told', () => {
    const client = fakeClient();
    const reporter = new WatchingReporter(client);
    reporter.tell('s1', true, true);
    reporter.tell('s1', true, true);
    expect(client.sent).toHaveLength(1);
    reporter.forget();
    reporter.tell('s1', true, true);
    expect(client.sent.map((sent) => sent.input)).toEqual(['session/watching', 'session/watching']);
  });
});
