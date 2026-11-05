import { describe, expect, it } from 'vitest';
import { nearestLine } from './nearestLine';

const spans = [
  { line: 0, top: 0, bottom: 20 },
  { line: 1, top: 20, bottom: 40 },
  { line: 2, top: 70, bottom: 90 },
];

describe('the diff line nearest to a height', () => {
  it('is the line whose rows the height falls in', () => {
    expect(nearestLine(spans, 25)).toBe(1);
  });

  it('is the nearer of two lines when the height falls between them, as under a thread', () => {
    expect(nearestLine(spans, 45)).toBe(1);
    expect(nearestLine(spans, 66)).toBe(2);
  });

  it('is the first line above the first and the last line below the last', () => {
    expect(nearestLine(spans, -50)).toBe(0);
    expect(nearestLine(spans, 500)).toBe(2);
  });

  it('is none when there are no lines', () => {
    expect(nearestLine([], 10)).toBeUndefined();
  });
});
