import type { Client, SessionWatchedPayload } from '../protocol';

/** ADR 0058: the selected pane's session while that pane and the window both have focus, else none. */
export const watchedPayload = (selected: string | undefined, paneFocused: boolean, windowFocused: boolean): SessionWatchedPayload => {
  return selected !== undefined && paneFocused && windowFocused
    ? { kind: 'watchedSessionPayload', session: selected }
    : { kind: 'noWatchedSessionPayload' };
};

/** Tells the sidecar which session the user looks at, once per change: the same answer twice is sent once. */
export class WatchingReporter {
  private told: string | undefined;

  constructor(private readonly client: Client) {}

  tell(selected: string | undefined, paneFocused: boolean, windowFocused: boolean): void {
    const payload = watchedPayload(selected, paneFocused, windowFocused);
    const text = JSON.stringify(payload);
    if (text !== this.told) {
      this.told = text;
      this.client.send('session/watching', payload);
    }
  }

  /** Forgets what was told, so the next tell is sent: a reloaded sidecar has heard nothing yet. */
  forget(): void {
    this.told = undefined;
  }
}
