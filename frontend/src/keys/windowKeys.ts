import { type Action, actionOf } from './bindings';
import type { Binding } from './chords';

const TYPING_SURFACE = '[data-terminal], [data-typing]';

interface Options {
  packaged?: boolean;
  reserved?: () => readonly Binding[];
}

/**
 * One `keydown` handler for the window: every key the TUI binds is prevented and reported, until `remove`.
 * A key typed into a terminal or a comment box is theirs: its pane's own handler keeps the reserved chords from `claude` (ADR 0058).
 * A packaged build prevents every other key too, so none moves focus, scrolls, reloads or opens the inspector, and
 * prevents the right-click menu with its Reload; a development build leaves both to the webview.
 */
export class WindowKeys implements EventListenerObject {
  constructor(
    private readonly onAction: (action: Action) => void,
    private readonly options: Options = {},
  ) {
    window.addEventListener('keydown', this);
    window.addEventListener('contextmenu', this);
  }

  remove(): void {
    window.removeEventListener('keydown', this);
    window.removeEventListener('contextmenu', this);
  }

  handleEvent(event: Event): void {
    if (event instanceof KeyboardEvent) {
      this.key(event);
    } else if (this.options.packaged === true) {
      event.preventDefault();
    }
  }

  private key(event: KeyboardEvent): void {
    if (event.target instanceof Element && event.target.closest(TYPING_SURFACE) !== null) {
      return;
    }
    const action = actionOf(event, this.options.reserved?.());
    if (action !== undefined) {
      event.preventDefault();
      this.onAction(action);
    } else if (this.options.packaged === true) {
      event.preventDefault();
    }
  }
}
