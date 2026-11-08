import { describe, expect, it } from 'vitest';
import { stylesheet } from './stylesheet';

const tokens = stylesheet('tokens.css');
const DECLARATION = (name: string): RegExp => new RegExp(`--${name}:\\s*([^;]+);`);

const token = (name: string): string => {
  const found = DECLARATION(name).exec(tokens);
  if (found?.[1] === undefined) {
    throw new Error(`no token --${name}`);
  }
  return found[1].trim();
};

describe('the reading type scale', () => {
  it('starts at the 15px of a half tile', () => {
    expect(token('reading-min')).toBe('15px');
  });

  it('stops at a ceiling of 18px', () => {
    expect(token('reading-max')).toBe('18px');
  });

  it('grows with the width of the pane, held between the floor and the ceiling', () => {
    expect(token('reading-size')).toBe(
      'clamp(var(--reading-min), calc(var(--reading-min) + (100cqi - var(--reading-half-pane)) * var(--reading-rate)), var(--reading-max))',
    );
  });

  it('reaches the ceiling at a full-screen pane', () => {
    const half = parseFloat(token('reading-half-pane'));
    const full = parseFloat(token('reading-full-pane'));
    const rate = parseFloat(token('reading-rate'));
    expect(15 + (full - half) * rate).toBeCloseTo(18, 1);
  });
});
