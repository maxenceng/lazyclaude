const MODAL_SURFACE = '[data-modal]';
const POINTER_EVENTS = ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'click', 'dblclick', 'auxclick', 'wheel'] as const;
const LISTENING = { capture: true, passive: false } as const;

/**
 * Keeps the mouse out of what is behind an open modal (ADR 0051): a pointer event outside every `[data-modal]` surface is stopped
 * before any element sees it, so a popup drawn without a scrim is as modal to clicks as it is to keys. A mousedown is also
 * prevented, so the click does not take focus from the popup's input either.
 */
export class ModalClicks implements EventListenerObject {
  constructor(private readonly isModal: () => boolean) {
    POINTER_EVENTS.forEach((type) => window.addEventListener(type, this, LISTENING));
  }

  remove(): void {
    POINTER_EVENTS.forEach((type) => window.removeEventListener(type, this, LISTENING));
  }

  handleEvent(event: Event): void {
    if (!this.isModal() || (event.target instanceof Element && event.target.closest(MODAL_SURFACE) !== null)) {
      return;
    }
    event.stopPropagation();
    event.preventDefault();
  }
}
