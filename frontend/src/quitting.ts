import type { SessionSessionsView } from './protocol';
import { Store } from './shared/store';

/** What leaving the window needs: how many sessions are hosted, closing the window, and where a refused close is said. */
export interface QuittingDeps {
  hosted: () => number;
  closeWindow: () => Promise<void>;
  refused: (reason: string) => void;
}

/**
 * Leaving the window as the TUI's `q` does: at once while no session is hosted, else once the person says yes, since a quit
 * ends every hosted session, idle or not (ADR 0048). The window's own close button does not ask.
 */
export class Quitting {
  /** How many sessions were hosted when the quit was asked, while the question is open. */
  readonly asking: Store<number | undefined> = new Store<number | undefined>(undefined);

  constructor(private readonly deps: QuittingDeps) {}

  request(): void {
    const hosted = this.deps.hosted();
    if (hosted === 0) {
      this.close();
    } else {
      this.asking.set(hosted);
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

/** The sessions hosted, as the sidecar lists them; while the session watch has not answered, the window's own panes. */
export const hostedOf = (sessions: SessionSessionsView | undefined, hostedPanes: number): number =>
  sessions === undefined ? hostedPanes : sessions.hosted.length;
