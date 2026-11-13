import { describe, expect, it } from 'vitest';
import { Elapsed, ageOf } from './age';

const NOW = new Date('2026-11-13T12:00:00Z');

describe('the age of a pull request', () => {
  it.each([
    ['2026-11-13T11:59:40Z', Elapsed.Now, 0],
    ['2026-11-13T11:55:00Z', Elapsed.Minutes, 5],
    ['2026-11-13T10:00:00Z', Elapsed.Hours, 2],
    ['2026-11-13T10:01:00Z', Elapsed.Hours, 1],
    ['2026-11-12T11:00:00Z', Elapsed.Days, 1],
    ['2026-10-01T12:00:00Z', Elapsed.Days, 43],
  ])('opened at %s is %s %s', (openedAt, unit, count) => {
    expect(ageOf(openedAt, NOW)).toEqual({ unit, count });
  });

  it('counts elapsed time, so a night the clocks went back is a day of 25 hours, not two', () => {
    const now = new Date('2026-10-26T12:00:00Z');
    expect(ageOf('2026-10-25T12:00:00Z', now)).toEqual({ unit: Elapsed.Days, count: 1 });
    expect(ageOf('2026-10-25T13:00:00Z', now)).toEqual({ unit: Elapsed.Hours, count: 23 });
  });

  it('is now for an instant ahead of the clock', () => {
    expect(ageOf('2026-11-13T12:05:00Z', NOW)).toEqual({ unit: Elapsed.Now, count: 0 });
  });

  it('is unknown for text that is no instant', () => {
    expect(ageOf('yesterday', NOW)).toEqual({ unit: Elapsed.Unknown, count: 0 });
  });
});
