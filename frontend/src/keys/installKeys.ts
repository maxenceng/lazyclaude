import { type Action, actionOf } from './bindings';
import type { Binding } from './chords';

interface Options {
  packaged?: boolean;
  reserved?: () => readonly Binding[];
}

/**
 * One `keydown` handler for the window: every key the TUI binds is prevented and reported; returns how to remove it.
 * A packaged build prevents every other key too, so none moves focus, scrolls, reloads or opens the inspector; a
 * development build leaves them to the webview.
 */
export function installKeys(onAction: (action: Action) => void, options: Options = {}): () => void {
  const handler = (event: KeyboardEvent) => {
    const action = actionOf(event, options.reserved?.());
    if (action !== undefined) {
      event.preventDefault();
      onAction(action);
    } else if (options.packaged === true) {
      event.preventDefault();
    }
  };
  window.addEventListener('keydown', handler);
  return () => window.removeEventListener('keydown', handler);
}
