import type { SessionSessionsView } from './protocol';
import { Store } from './shared/store';

/** What leaving the window needs: how many hosted sessions are busy or waiting, closing the window, and where a refused close is said. */
export interface QuittingDeps {
  busyOrWaiting: () => number;
  closeWindow: () => Promise<void>;
  refused: (reason: string) => void;
}

/**
 * Leaving the window as the TUI's `q` does: at once while no hosted session is busy or waiting, else once the person says yes,
 * since a quit ends every hosted session (ADR 0048). The window's own close button does not ask.
 */
export class Quitting {
  /** How many hosted sessions were busy or waiting when the quit was asked, while the question is open. */
  readonly asking: Store<number | undefined> = new Store<number | undefined>(undefined);

  constructor(private readonly deps: QuittingDeps) {}

  request(): void {
    const busy = this.deps.busyOrWaiting();
    if (busy === 0) {
      this.close();
    } else {
      this.asking.set(busy);
    }
  }

  isAsking(): boolean {
    return this.asking.get() !== undefined;
  }

  /** Yes quits; anything else keeps the window and its sessions. */
  answer(yes: boolean): void {
    this.asking.set(undefined);
    if (yes) {
      this.close();
    }
  }

  private close(): void {
    this.deps.closeWindow().catch((error: unknown) => this.deps.refused(error instanceof Error ? error.message : String(error)));
  }
}

/** The hosted sessions busy or waiting; while the session watch has not answered, every hosted pane counts, so a quit still asks. */
export const busyOrWaitingOf = (sessions: SessionSessionsView | undefined, hostedPanes: number): number =>
  sessions === undefined ? hostedPanes : sessions.hosted.filter((session) => session.busyOrWaiting).length;
