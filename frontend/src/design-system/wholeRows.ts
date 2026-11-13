import { useEffect, useRef } from 'react';

const ROW = '[data-row]';

/** One row's vertical extent inside the scrolled content. */
export interface RowSpan {
  top: number;
  bottom: number;
}

/**
 * The height a scroller takes so that no row is drawn cut: the bottom-most row edge within `available` of the scroll position, or
 * all of `available` when the content's end is in view or no edge fits (a lone row taller than the pane is cut either way).
 */
export const wholeRowsHeight = (available: number, scrollTop: number, rows: readonly RowSpan[]): number => {
  const end = Math.max(0, ...rows.map((row) => row.bottom));
  if (end - scrollTop <= available) {
    return available;
  }
  const fitting = rows.map((row) => row.bottom - scrollTop).filter((edge) => edge <= available && edge > 0);
  return fitting.length === 0 ? available : Math.max(...fitting);
};

/** Caps one scroller to the rows that fit, whenever it, its parent or its content changes, or it scrolls. */
class WholeRows implements EventListenerObject {
  private readonly sizes = new ResizeObserver(() => this.fit());
  private readonly changes = new MutationObserver(() => this.fit());

  constructor(private readonly scroller: HTMLElement) {
    this.fit();
    this.sizes.observe(scroller);
    if (scroller.parentElement !== null) {
      this.sizes.observe(scroller.parentElement);
    }
    this.changes.observe(scroller, { childList: true, subtree: true });
    scroller.addEventListener('scroll', this, { passive: true });
  }

  remove(): void {
    this.sizes.disconnect();
    this.changes.disconnect();
    this.scroller.removeEventListener('scroll', this);
  }

  handleEvent(): void {
    this.fit();
  }

  private fit(): void {
    this.scroller.style.maxHeight = '';
    const spans = Array.from(this.scroller.querySelectorAll<HTMLElement>(ROW), (row) => ({ top: row.offsetTop, bottom: row.offsetTop + row.offsetHeight }));
    this.scroller.style.maxHeight = `${wholeRowsHeight(this.scroller.clientHeight, this.scroller.scrollTop, spans)}px`;
  }
}

/** Shows whole rows only: the ref'd scroller is capped to the rows that fit, recomputed as it scrolls, resizes, or its content changes. */
export const useWholeRows = <T extends HTMLElement>() => {
  const ref = useRef<T>(null);
  useEffect(() => {
    const scroller = ref.current;
    if (scroller === null) {
      return;
    }
    const rows = new WholeRows(scroller);
    return () => rows.remove();
  }, []);
  return ref;
};
