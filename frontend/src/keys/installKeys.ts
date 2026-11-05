import { type Action, actionOf } from './bindings';
import type { Binding } from './chords';

const TERMINAL = '[data-terminal]';

interface Options {
  packaged?: boolean;
  reserved?: () => readonly Binding[];
}

/**
 * One `keydown` handler for the window: every key the TUI binds is prevented and reported; returns how to remove it.
 * A key typed into a terminal is the terminal's: its pane's own handler keeps the reserved chords from `claude` (ADR 0058).
 * A packaged build prevents every other key too, so none moves focus, scrolls, reloads or opens the inspector, and
 * prevents the right-click menu with its Reload; a development build leaves both to the webview.
 */
export const installKeys = (onAction: (action: Action) => void, options: Options = {}): () => void => {
  const handler = (event: KeyboardEvent) => {
    if (event.target instanceof Element && event.target.closest(TERMINAL) !== null) {
      return;
    }
    const action = actionOf(event, options.reserved?.());
    if (action !== undefined) {
      event.preventDefault();
      onAction(action);
    } else if (options.packaged === true) {
      event.preventDefault();
    }
  };
  const menu = (event: MouseEvent) => {
    if (options.packaged === true) {
      event.preventDefault();
    }
  };
  window.addEventListener('keydown', handler);
  window.addEventListener('contextmenu', menu);
  return () => {
    window.removeEventListener('keydown', handler);
    window.removeEventListener('contextmenu', menu);
  };
};
