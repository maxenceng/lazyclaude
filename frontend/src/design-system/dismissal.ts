import { type RefObject, useEffect } from 'react';

const LEAVING = 'Escape';

/** Dismisses what holds `inside` when the mouse goes down outside it, and when `esc` is pressed, before the window's own keys see it. */
export class Dismissal implements EventListenerObject {
  constructor(
    private readonly inside: HTMLElement,
    private readonly onDismiss: () => void,
    escape: boolean,
  ) {
    window.addEventListener('mousedown', this, true);
    if (escape) {
      window.addEventListener('keydown', this, true);
    }
  }

  remove(): void {
    window.removeEventListener('mousedown', this, true);
    window.removeEventListener('keydown', this, true);
  }

  handleEvent(event: Event): void {
    if (event instanceof KeyboardEvent) {
      if (event.key === LEAVING) {
        event.stopPropagation();
        this.onDismiss();
      }
    } else if (!(event.target instanceof Node && this.inside.contains(event.target))) {
      this.onDismiss();
    }
  }
}

/** While `active`, a click outside `ref`'s element calls `onDismiss`, and so does `esc` when `escape` is set. */
export const useDismissal = (ref: RefObject<HTMLElement | null>, active: boolean, onDismiss: () => void, escape: boolean) => {
  useEffect(() => {
    const inside = ref.current;
    if (!active || inside === null) {
      return;
    }
    const dismissal = new Dismissal(inside, onDismiss, escape);
    return () => dismissal.remove();
  }, [ref, active, onDismiss, escape]);
};
