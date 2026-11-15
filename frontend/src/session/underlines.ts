import type { IBufferRange, IDisposable, Terminal } from '@xterm/xterm';
import { terminalLinkUnderline } from '../design-system';

/** What draws a link's underline over the one-row stretches it fills, until disposed. */
export interface UnderlineLayer {
  draw(stretches: IBufferRange[]): IDisposable;
}

/** The marks of one drawn underline, removed together. */
class DrawnUnderline implements IDisposable {
  constructor(private readonly marks: HTMLElement[]) {}

  dispose(): void {
    this.marks.forEach((mark) => mark.remove());
  }
}

/**
 * Underlines drawn as thin marks over xterm's screen, placed by the cell size the screen's measure gives. xterm's own
 * decorations would do, but it refuses them on the alternate screen, which is where Claude Code draws.
 */
export class ScreenUnderlines implements UnderlineLayer {
  constructor(private readonly terminal: Pick<Terminal, 'element' | 'cols' | 'rows' | 'buffer'>) {}

  draw(stretches: IBufferRange[]): IDisposable {
    const screen = this.terminal.element?.querySelector('.xterm-screen');
    if (!(screen instanceof HTMLElement)) {
      return new DrawnUnderline([]);
    }
    const width = screen.clientWidth / this.terminal.cols;
    const height = screen.clientHeight / this.terminal.rows;
    const top = this.terminal.buffer.active.viewportY;
    const marks = stretches
      .map((stretch) => ({ stretch, row: stretch.start.y - 1 - top }))
      .filter(({ row }) => row >= 0 && row < this.terminal.rows)
      .map(({ stretch, row }) => {
        const mark = document.createElement('div');
        mark.className = terminalLinkUnderline;
        mark.style.left = `${(stretch.start.x - 1) * width}px`;
        mark.style.top = `${(row + 1) * height}px`;
        mark.style.width = `${(stretch.end.x - stretch.start.x + 1) * width}px`;
        screen.append(mark);
        return mark;
      });
    return new DrawnUnderline(marks);
  }
}
