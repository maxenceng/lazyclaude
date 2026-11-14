import { act, render } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DiffLine } from '../design-system';
import { DiffPane } from './ReviewScreen';
import { added, diffFixture, fileFixture, hunk, kept, removed } from './fixtures';
import { Harness } from './reviewHarness';

vi.mock('../design-system', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../design-system')>();
  return { ...actual, DiffLine: vi.fn(actual.DiffLine) };
});

const drawnLines = (): number => vi.mocked(DiffLine).mock.calls.length;

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const opened = async (): Promise<Harness> => {
  const harness = new Harness();
  await harness.readAll({ diff });
  render(<DiffPane review={harness.review} focused onFocus={() => {}} onRetry={() => {}} onClose={() => {}} />);
  vi.mocked(DiffLine).mockClear();
  return harness;
};

describe('the diff redrawn', () => {
  it('draws no diff line again while a key is typed in the comment box', async () => {
    const harness = await opened();
    await harness.openSummary();
    vi.mocked(DiffLine).mockClear();
    act(() => harness.review.typeInBox('a'));
    expect(drawnLines(), 'a keystroke redrawing every line is what made a long diff lag').toBe(0);
  });

  it('draws again only the line the cursor leaves and the one it reaches', async () => {
    const harness = await opened();
    act(() => harness.review.moveCursor(1));
    expect(drawnLines()).toBe(2);
  });

  it('draws no diff line again when a file is selected in the rail', async () => {
    const harness = await opened();
    act(() => harness.review.selectFile(1));
    expect(drawnLines()).toBe(0);
  });
});
