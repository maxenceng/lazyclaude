import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Column, Columns } from './Columns';
import { Pane } from './Pane';
import { useWholeRows, wholeRowsHeight } from './wholeRows';

const ROWS = [
  { top: 0, bottom: 20 },
  { top: 20, bottom: 40 },
  { top: 40, bottom: 60 },
  { top: 60, bottom: 80 },
];

describe('the height that shows whole rows only', () => {
  it('stops at the last row edge that fits, so a row that would be cut is left out', () => {
    expect(wholeRowsHeight(50, 0, ROWS)).toBe(40);
  });

  it('takes the whole space when every row fits', () => {
    expect(wholeRowsHeight(100, 0, ROWS)).toBe(100);
  });

  it('counts from where the pane is scrolled to', () => {
    expect(wholeRowsHeight(50, 20, ROWS)).toBe(40);
    expect(wholeRowsHeight(45, 30, ROWS)).toBe(30);
  });

  it('takes the whole space when the end of the content is in view, however it ends', () => {
    expect(wholeRowsHeight(70, 20, ROWS)).toBe(70);
  });

  it('takes the whole space when not even one row fits, rather than none', () => {
    expect(wholeRowsHeight(10, 0, ROWS)).toBe(10);
  });

  it('takes the whole space when there are no rows to cut', () => {
    expect(wholeRowsHeight(50, 0, [])).toBe(50);
  });
});

const Probe = () => {
  const ref = useWholeRows<HTMLDivElement>();
  return (
    <div ref={ref} data-testid="scroller">
      <div data-row="" />
      <div data-row="" />
      <div data-row="" />
    </div>
  );
};

const lay = () => {
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(50);
  vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(60);
  vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockReturnValue(20);
  const tops = [0, 20, 40];
  let next = 0;
  vi.spyOn(HTMLElement.prototype, 'offsetTop', 'get').mockImplementation(() => tops[next++ % tops.length] ?? 0);
};

describe('a scroller that shows whole rows', () => {
  afterEach(() => vi.restoreAllMocks());


  it('limits its height to the rows that fit', () => {
    lay();
    const { getByTestId } = render(<Probe />);
    expect(getByTestId('scroller').style.maxHeight).toBe('40px');
  });

  it('is how a pane body and a status column draw their rows', () => {
    lay();
    const { container } = render(
      <>
        <Pane title="Board" focused={false}>
          <div data-row="" />
          <div data-row="" />
          <div data-row="" />
        </Pane>
        <Columns>
          <Column label="todo">
            <div data-row="" />
            <div data-row="" />
            <div data-row="" />
          </Column>
        </Columns>
      </>,
    );
    expect(container.querySelector<HTMLElement>('[data-pane-body]')?.style.maxHeight).toBe('40px');
    expect(screen.getByRole('group', { name: 'todo' }).style.maxHeight).toBe('40px');
  });
});
