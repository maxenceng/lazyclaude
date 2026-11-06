import type { Client, SessionSessionsView } from '../protocol';
import { Store } from '../shared/store';

/** The live sessions behind the lamps; undefined until the watch answers, and when it cannot. */
export class SessionStore {
  readonly sessions: Store<SessionSessionsView | undefined> = new Store<SessionSessionsView | undefined>(undefined);

  constructor(private readonly client: Client) {
    client.subscribe('session/changed', (view) => this.sessions.set(view));
  }

  async watch(root: string, ticketIds: string[]): Promise<void> {
    try {
      this.sessions.set(await this.client.call('session/watch', { root, ticketIds }));
    } catch {
      this.sessions.set(undefined);
    }
  }
}
