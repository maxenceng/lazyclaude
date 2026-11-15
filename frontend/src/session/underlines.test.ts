import { Terminal } from '@xterm/xterm';
import { describe, expect, it } from 'vitest';
import { ScreenUnderlines } from './underlines';

const opened = (): { terminal: Terminal; screen: HTMLElement } => {
  const host = document.createElement('div');
  document.body.append(host);
  const terminal = new Terminal({ cols: 10, rows: 5 });
  terminal.open(host);
  const screen = host.querySelector('.xterm-screen');
  if (!(screen instanceof HTMLElement)) {
    throw new Error('xterm drew no screen');
  }
  Object.defineProperty(screen, 'clientWidth', { value: 80 });
  Object.defineProperty(screen, 'clientHeight', { value: 100 });
  return { terminal, screen };
};

const marksOn = (screen: HTMLElement): string[][] =>
  [...screen.querySelectorAll<HTMLElement>(':scope > div')]
    .filter((mark) => mark.style.left !== '')
    .map((mark) => [mark.style.left, mark.style.top, mark.style.width]);

describe('the underlines drawn over a terminal screen', () => {
  it('marks each stretch under its cells, by the cell size the screen measures', () => {
    const { terminal, screen } = opened();
    new ScreenUnderlines(terminal).draw([
      { start: { x: 3, y: 1 }, end: { x: 10, y: 1 } },
      { start: { x: 2, y: 2 }, end: { x: 4, y: 2 } },
    ]);
    expect(marksOn(screen), 'cells are 8 by 20 pixels: 80 wide over 10 columns, 100 high over 5 rows').toEqual([
      ['16px', '20px', '64px'],
      ['8px', '40px', '24px'],
    ]);
  });

  it('removes every mark of an underline once it is disposed', () => {
    const { terminal, screen } = opened();
    new ScreenUnderlines(terminal).draw([{ start: { x: 1, y: 1 }, end: { x: 2, y: 1 } }]).dispose();
    expect(marksOn(screen)).toEqual([]);
  });

  it('marks nothing for a terminal not yet on screen', () => {
    expect(() => new ScreenUnderlines(new Terminal()).draw([{ start: { x: 1, y: 1 }, end: { x: 2, y: 1 } }]).dispose()).not.toThrow();
  });
});
