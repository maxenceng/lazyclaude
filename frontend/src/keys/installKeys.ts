import { type Action, actionOf } from './bindings';
import type { Binding } from './chords';

interface Options {
  packaged?: boolean;
  reserved?: () => readonly Binding[];
}

function reloads(event: KeyboardEvent): boolean {
  return event.key === 'F5' || (event.ctrlKey && event.key.toLowerCase() === 'r');
}

/** One `keydown` handler for the window: every key the TUI binds is prevented and reported; returns how to remove it. */
export function installKeys(onAction: (action: Action) => void, options: Options = {}): () => void {
  const handler = (event: KeyboardEvent) => {
    const action = actionOf(event, options.reserved?.());
    if (action !== undefined) {
      event.preventDefault();
      onAction(action);
    } else if (options.packaged === true && reloads(event)) {
      event.preventDefault();
    }
  };
  window.addEventListener('keydown', handler);
  return () => window.removeEventListener('keydown', handler);
}
