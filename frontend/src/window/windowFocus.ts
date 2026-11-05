import { createStore, type Store } from '../shared/store';

export interface FocusSource {
  isFocused(): Promise<boolean>;
  onFocusChanged(handler: (focused: boolean) => void): Promise<() => void>;
}

/**
 * Whether the window has focus (ADR 0058): `isFocused()` at start, then every change. A window not yet known to have focus is
 * taken not to, so a waiting session is never held back from notifying by a guess.
 */
export class WindowFocus {
  readonly focused: Store<boolean> = createStore(false);
  private heard = false;
  private stopped = false;
  private unlisten: (() => void) | undefined;

  constructor(private readonly source: FocusSource) {
    void source.onFocusChanged((next) => this.changed(next)).then((stop) => this.listening(stop));
  }

  dispose(): void {
    this.stopped = true;
    this.unlisten?.();
  }

  private changed(next: boolean): void {
    this.heard = true;
    this.focused.set(next);
  }

  private async listening(stop: () => void): Promise<void> {
    if (this.stopped) {
      stop();
      return;
    }
    this.unlisten = stop;
    const initial = await this.source.isFocused();
    if (!this.heard) {
      this.focused.set(initial);
    }
  }
}
