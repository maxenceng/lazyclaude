import type { Client, SessionsView, Store } from '../protocol';
import { createStore } from '../protocol';

export interface SessionStore {
  readonly sessions: Store<SessionsView | undefined>;
  watch(root: string, ticketIds: string[]): Promise<void>;
}

/** The live sessions behind the lamps; undefined until the watch answers, and when it cannot. */
export function createSessionStore(client: Client): SessionStore {
  const sessions = createStore<SessionsView | undefined>(undefined);
  client.subscribe('session/changed', (view) => sessions.set(view));
  return {
    sessions,
    async watch(root, ticketIds) {
      try {
        sessions.set(await client.call('session/watch', { root, ticketIds }));
      } catch {
        sessions.set(undefined);
      }
    },
  };
}
