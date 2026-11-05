import { type LineSpan, nearestLine } from './nearestLine';
import type { ReviewSession } from './reviewSession';

const LINE = '[data-line]';

const spansIn = (root: Element): LineSpan[] =>
  [...root.querySelectorAll<HTMLElement>(LINE)].map((element) => {
    const box = element.getBoundingClientRect();
    return { line: Number(element.dataset.line), top: box.top, bottom: box.bottom };
  });

/** The diff line a pointer let go on: the one it is over, else the one nearest to its height in the diff. */
const lineAt = (root: Element, event: MouseEvent): number | undefined => {
  const over = event.target instanceof Element ? event.target.closest<HTMLElement>(LINE) : null;
  return over === null ? nearestLine(spansIn(root), event.clientY) : Number(over.dataset.line);
};

/** The end of a drag that began on a diff line: the left button let go anywhere in the window marks the range to the line nearest to it. */
export class DragRelease implements EventListenerObject {
  constructor(
    private readonly session: ReviewSession,
    private readonly diff: () => Element | null,
  ) {
    window.addEventListener('mouseup', this);
  }

  remove(): void {
    window.removeEventListener('mouseup', this);
  }

  handleEvent(event: Event): void {
    const root = this.diff();
    if (event instanceof MouseEvent && event.button === 0 && root !== null) {
      const line = lineAt(root, event);
      if (line !== undefined) {
        void this.session.releaseLine(line);
      }
    }
  }
}
