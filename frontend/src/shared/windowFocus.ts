import { createStore, type Store } from './store';

export interface FocusSource {
  isFocused(): Promise<boolean>;
  onFocusChanged(handler: (focused: boolean) => void): Promise<() => void>;
}

export interface WindowFocus {
  readonly focused: Store<boolean>;
  dispose(): void;
}

/**
 * Whether the window has focus (ADR 0058): `isFocused()` at start, then every change. A window not yet known to have focus is
 * taken not to, so a waiting session is never held back from notifying by a guess.
 */
export const createWindowFocus = (source: FocusSource): WindowFocus => {
  const focused = createStore(false);
  let heard = false;
  let stopped = false;
  let unlisten: (() => void) | undefined;
  void source
    .onFocusChanged((next) => {
      heard = true;
      focused.set(next);
    })
    .then(async (stop) => {
      if (stopped) {
        stop();
        return;
      }
      unlisten = stop;
      const initial = await source.isFocused();
      if (!heard) {
        focused.set(initial);
      }
    });
  return {
    focused,
    dispose() {
      stopped = true;
      unlisten?.();
    },
  };
};
