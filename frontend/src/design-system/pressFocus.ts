import { type RefObject, useEffect, useRef } from 'react';

const ACTIONS = '[data-pane-actions]';

/**
 * Hands a pane focus when the mouse goes down anywhere inside it but its title row's actions, which decide focus themselves (hiding
 * the ticket gives it to the dock). A pointer shortcut only: the window's keys already move focus between panes, so the pane stays
 * a region rather than becoming a control.
 */
class PressFocus implements EventListenerObject {
  constructor(
    private readonly pane: HTMLElement,
    private readonly onFocus: RefObject<() => void>,
  ) {
    pane.addEventListener('mousedown', this);
  }

  remove(): void {
    this.pane.removeEventListener('mousedown', this);
  }

  handleEvent(event: Event): void {
    if (!(event.target instanceof Element && event.target.closest(ACTIONS) !== null)) {
      this.onFocus.current();
    }
  }
}

/** While `onFocus` is given, a press inside `ref`'s element calls it; the latest `onFocus` is the one called. */
export const usePressFocus = (ref: RefObject<HTMLElement | null>, onFocus: (() => void) | undefined) => {
  const latest = useRef<() => void>(() => undefined);
  useEffect(() => {
    latest.current = onFocus ?? (() => undefined);
  });
  const given = onFocus !== undefined;
  useEffect(() => {
    const pane = ref.current;
    if (!given || pane === null) {
      return;
    }
    const press = new PressFocus(pane, latest);
    return () => press.remove();
  }, [ref, given]);
};
