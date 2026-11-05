import { describe, expect, it } from 'vitest';
import { watchedPayload } from './watched';

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
