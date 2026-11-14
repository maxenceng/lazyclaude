import { describe, expect, it } from 'vitest';
import { hostedFixture, sessionFixture, sessionsFixture } from './fixtures';
import { LampColour } from './markers';
import { tabOfLive, tabOfNextColour } from './statusLineTargets';

const turn = { kind: 'yourTurn' } as const;
const busy = { kind: 'busy' } as const;

const view = (...entries: [string, string, typeof turn | typeof busy][]) => {
  const live = entries.map(([, liveId, state]) => sessionFixture({ id: liveId, state }));
  return sessionsFixture(live, { hosted: entries.map(([tab], index) => hostedFixture(tab, { live: live[index] })) });
};

describe('the tab a status line tile reaches', () => {
  it('is the tab hosting the live session', () => {
    expect(tabOfLive(view(['a', 'a', turn], ['b', 'b', turn]), 'b')).toBe('b');
  });

  it('is the tab whose live session replaced its launch id after a clear', () => {
    expect(tabOfLive(view(['launch', 'cleared', turn]), 'cleared')).toBe('launch');
  });

  it('is none for a live session this window does not host', () => {
    expect(tabOfLive(sessionsFixture([sessionFixture({ id: 'x' })]), 'x')).toBeUndefined();
  });
});

describe('the tab a status line count reaches', () => {
  const three = view(['a', 'a', turn], ['b', 'b', busy], ['c', 'c', turn]);

  it('is the next tab of that colour after the selected one', () => {
    expect(tabOfNextColour(three, ['a', 'b', 'c'], 'a', LampColour.Turn)).toBe('c');
  });

  it('wraps past the last tab', () => {
    expect(tabOfNextColour(three, ['a', 'b', 'c'], 'c', LampColour.Turn)).toBe('a');
  });

  it('is the first one after the selected tab when it is not that colour', () => {
    expect(tabOfNextColour(three, ['a', 'b', 'c'], 'b', LampColour.Turn)).toBe('c');
  });

  it('is the only one of that colour on every click, selected or not', () => {
    const one = view(['a', 'a', turn], ['b', 'b', busy]);
    expect(tabOfNextColour(one, ['a', 'b'], 'a', LampColour.Turn)).toBe('a');
    expect(tabOfNextColour(one, ['a', 'b'], 'b', LampColour.Turn)).toBe('a');
  });

  it('is none when no hosted session has that colour', () => {
    expect(tabOfNextColour(three, ['a', 'b', 'c'], 'a', LampColour.Permission)).toBeUndefined();
  });

  it('is none when every session of that colour is unhosted', () => {
    const unhosted = sessionsFixture([sessionFixture({ id: 'x', state: turn })]);
    expect(tabOfNextColour(unhosted, [], undefined, LampColour.Turn)).toBeUndefined();
  });

  it('follows the dock tab order rather than the order sessions are listed', () => {
    expect(tabOfNextColour(three, ['c', 'b', 'a'], 'c', LampColour.Turn)).toBe('a');
  });

  it('skips a tab that is open but not yet read', () => {
    expect(tabOfNextColour(three, ['a', 'new', 'c'], 'a', LampColour.Turn)).toBe('c');
  });
});
