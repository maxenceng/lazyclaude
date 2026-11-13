import { describe, expect, it } from 'vitest';
import { ageOf, AgeUnit } from './age';

const NOW = Date.parse('2026-03-29T12:00:00Z');
const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

describe('the age of a session', () => {
  it('is now for under a minute, and for a time that has not come', () => {
    expect(ageOf('2026-03-29T11:59:30Z', NOW)).toEqual({ unit: AgeUnit.Now, count: 0 });
    expect(ageOf('2026-03-29T12:05:00Z', NOW)).toEqual({ unit: AgeUnit.Now, count: 0 });
  });

  it.each([
    [12 * MINUTE, AgeUnit.Minutes, 12],
    [59 * MINUTE, AgeUnit.Minutes, 59],
    [3 * HOUR + 20 * MINUTE, AgeUnit.Hours, 3],
    [23 * HOUR, AgeUnit.Hours, 23],
    [2 * DAY, AgeUnit.Days, 2],
  ])('counts %d ms in the largest whole unit', (elapsed, unit, count) => {
    expect(ageOf(new Date(NOW - elapsed).toISOString(), NOW)).toEqual({ unit, count });
  });

  it('is elapsed time, so a day across the clocks changing is still 24 hours', () => {
    expect(ageOf('2026-03-28T12:00:00Z', NOW)).toEqual({ unit: AgeUnit.Days, count: 1 });
  });

  it('is none for a time that cannot be read', () => {
    expect(ageOf('not a time', NOW)).toBeUndefined();
  });
});
