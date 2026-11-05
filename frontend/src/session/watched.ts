import type { WatchedPayload } from '../protocol';

/** ADR 0058: the selected pane's session while that pane and the window both have focus, else none. */
export const watchedPayload = (selected: string | undefined, paneFocused: boolean, windowFocused: boolean): WatchedPayload => {
  return selected !== undefined && paneFocused && windowFocused
    ? { kind: 'watchedSessionPayload', session: selected }
    : { kind: 'noWatchedSessionPayload' };
};
