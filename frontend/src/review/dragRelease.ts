import { type LineSpan, nearestLine } from './nearestLine';
import type { OpenReview } from './openReview';

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

const onLine = (event: MouseEvent): boolean => event.target instanceof Element && event.target.closest(LINE) !== null;

const LEFT = 0;

/**
 * The end of a drag that began on a diff line: the left button let go anywhere in the window marks the range to the line nearest to it.
 * A left press on no diff line, or the window losing focus, ends the drag unmarked, so a release the window never saw cannot mark later.
 */
export class DragRelease implements EventListenerObject {
  constructor(
    private readonly review: OpenReview,
    private readonly diff: () => Element | null,
  ) {
    window.addEventListener('mouseup', this);
    window.addEventListener('mousedown', this);
    window.addEventListener('blur', this);
  }

  remove(): void {
    window.removeEventListener('mouseup', this);
    window.removeEventListener('mousedown', this);
    window.removeEventListener('blur', this);
  }

  handleEvent(event: Event): void {
    if (event.type === 'blur') {
      this.review.dropDrag();
    } else if (event instanceof MouseEvent && event.button === LEFT) {
      this.pointer(event);
    }
  }

  private pointer(event: MouseEvent): void {
    if (event.type === 'mousedown') {
      if (!onLine(event)) {
        this.review.dropDrag();
      }
      return;
    }
    const root = this.diff();
    const line = root === null ? undefined : lineAt(root, event);
    if (line !== undefined) {
      void this.review.releaseLine(line);
    }
  }
}
