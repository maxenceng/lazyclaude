import type { Client, SessionSessionsView } from '../protocol';
import { Store } from '../shared/store';
import type { Hosting } from './hosting';
import type { LiveState } from './markers';

export interface ClosingDeps {
  client: Client;
  hosting: Hosting;
  sessions: Store<SessionSessionsView | undefined>;
}

/** The states a person might lose work in: closing a session Busy, Waiting or Unknown asks first, while one Starting or at Your turn closes at once. */
const ASKED: readonly LiveState[] = ['busy', 'waitingForPermission', 'waitingForInput', 'unknown'];

/**
 * Closing the selected hosted session, as the dock's close button and the reserved close chord do: at once when the session is
 * Starting or at Your turn, else once the question is answered Close. Any other answer keeps it running, and so does its own
 * exit, which takes the question away with the tab.
 */
export class Closing {
  /** The session the question is about, while it is open. */
  readonly asking: Store<string | undefined> = new Store<string | undefined>(undefined);

  constructor(private readonly deps: ClosingDeps) {
    deps.hosting.dock.subscribe(() => this.settled());
  }

  isAsking(): boolean {
    return this.asking.get() !== undefined;
  }

  request(): void {
    const session = this.deps.hosting.dock.get().selectedTab();
    if (session === undefined || this.isAsking()) {
      return;
    }
    if (this.mustAsk(session)) {
      this.asking.set(session);
    } else {
      this.send(session);
    }
  }

  /** Close ends the session; anything else keeps it. */
  answer(close: boolean): void {
    const session = this.asking.get();
    this.asking.set(undefined);
    if (close && session !== undefined) {
      this.send(session);
    }
  }

  private mustAsk(session: string): boolean {
    const state = this.deps.sessions.get()?.hosted.find((hosted) => hosted.id === session)?.live?.state.kind;
    return state !== undefined && ASKED.includes(state);
  }

  private send(session: string): void {
    this.deps.client.send('session/close', { session });
  }

  private settled(): void {
    const session = this.asking.get();
    if (session !== undefined && !this.deps.hosting.dock.get().tabs.includes(session)) {
      this.asking.set(undefined);
    }
  }
}
