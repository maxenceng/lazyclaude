import { describe, expect, it } from 'vitest';
import type { ReviewCheckOutcome } from '../protocol';
import { pullRequestFixture } from './fixtures';
import { Ci } from '../design-system';
import { ciOf, cellsOf, ciResultOf, hasMark, worstFirst, worstResult } from './pullRequestList';

const checked = (number: number, outcome: ReviewCheckOutcome, draft = false) =>
  pullRequestFixture({ number, draft, checks: [{ name: 'build', outcome }] });

describe('the pull request list', () => {
  it('is empty when no pull request is open', () => {
    expect(worstFirst([])).toEqual([]);
  });

  it('puts failed first, then pending, then passed, and drafts after them', () => {
    const list = [checked(1, 'PASSED'), checked(2, 'FAILED', true), checked(3, 'PENDING'), checked(4, 'FAILED'), checked(5, 'PASSED', true)];
    expect(worstFirst(list).map((pullRequest) => pullRequest.number)).toEqual([4, 3, 1, 2, 5]);
  });

  it('keeps the order it was given among equals, and puts one with no checks after the passed', () => {
    const none = pullRequestFixture({ number: 9, checks: [] });
    const list = [none, checked(1, 'PASSED'), checked(2, 'PASSED')];
    expect(worstFirst(list).map((pullRequest) => pullRequest.number)).toEqual([1, 2, 9]);
  });

  it('reads the CI result off the first check, which review lists worst first', () => {
    expect(ciResultOf(pullRequestFixture({ checks: [{ name: 'a', outcome: 'PENDING' }, { name: 'b', outcome: 'PASSED' }] }))).toBe('PENDING');
    expect(ciResultOf(pullRequestFixture({ checks: [] }))).toBeUndefined();
  });

  it('marks a draft pull request and one without checks', () => {
    expect(hasMark(checked(1, 'PASSED'))).toBe(false);
    expect(hasMark(checked(1, 'PASSED', true))).toBe(true);
    expect(hasMark(pullRequestFixture({ checks: [] }))).toBe(true);
  });

  it('finds the worst result of the list, failed over pending, and none for a list with no check', () => {
    expect(worstResult([checked(1, 'PASSED'), checked(2, 'PENDING'), checked(3, 'FAILED')])).toBe('FAILED');
    expect(worstResult([checked(1, 'PASSED'), checked(2, 'PENDING')])).toBe('PENDING');
    expect(worstResult([pullRequestFixture({ checks: [] })])).toBeUndefined();
  });

  it('leaves a draft out of the worst result, as the list sinks it below every other', () => {
    expect(worstResult([checked(1, 'PASSED'), checked(2, 'FAILED', true)])).toBe('PASSED');
    expect(worstResult([checked(2, 'FAILED', true)])).toBeUndefined();
  });

  it('draws an outcome as the glyph the TUI draws for it', () => {
    expect(ciOf('FAILED')).toBe(Ci.Failed);
    expect(ciOf('PENDING')).toBe(Ci.Pending);
    expect(ciOf('PASSED')).toBe(Ci.Passed);
    expect(ciOf(undefined)).toBeUndefined();
  });

  it('draws a pull request in a ticket\'s row as a mark and its result', () => {
    expect(cellsOf(checked(1, 'FAILED'))).toEqual({ marked: false, ci: Ci.Failed });
    expect(cellsOf(pullRequestFixture({ checks: [] }))).toEqual({ marked: true, ci: undefined });
    expect(cellsOf(checked(1, 'PASSED', true))).toEqual({ marked: true, ci: Ci.Passed });
  });
});
