import { describe, expect, it } from 'vitest';
import { pullRequestFixture } from './fixtures';
import { PullRequestSelection } from './pullRequestSelection';

const list = [pullRequestFixture({ number: 4 }), pullRequestFixture({ number: 2 }), pullRequestFixture({ number: 9 })];

describe('the pull request cursor', () => {
  it('stands on the first row until it is moved', () => {
    expect(new PullRequestSelection().cursorIn(list)?.number).toBe(4);
  });

  it('stands on nothing in an empty list', () => {
    const selection = new PullRequestSelection();
    selection.move([], 1);
    expect(selection.cursorIn([])).toBeUndefined();
  });

  it('moves down and up the rows and holds at the ends', () => {
    const selection = new PullRequestSelection();
    selection.move(list, 1);
    expect(selection.cursorIn(list)?.number).toBe(2);
    selection.move(list, 5);
    expect(selection.cursorIn(list)?.number).toBe(9);
    selection.move(list, -5);
    expect(selection.cursorIn(list)?.number).toBe(4);
  });

  it('falls back to the first row when the pull request it stood on closed', () => {
    const selection = new PullRequestSelection();
    selection.select(9);
    expect(selection.cursorIn([pullRequestFixture({ number: 4 })])?.number).toBe(4);
  });
});
