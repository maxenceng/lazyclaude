import { createBoardStore } from './board/boardStore';
import { ticketIds } from './board/rows';
import { createSelection } from './board/selection';
import { bindingsOf, type Binding } from './keys/chords';
import { createStore, type Client } from './protocol';
import { createSessionStore } from './session/sessionStore';

/** The stores one window reads, and the rule that loads them whenever the sidecar becomes ready. */
export function createApp(client: Client) {
  const board = createBoardStore(client);
  const selection = createSelection();
  const sessions = createSessionStore(client);

  const reserved = createStore<readonly Binding[]>([]);

  const loadConfiguration = async () => {
    try {
      reserved.set(bindingsOf(await client.call('session/configuration', {})));
    } catch {
      reserved.set([]);
    }
  };

  const load = async () => {
    const loading = board.load();
    void loadConfiguration();
    await loading;
    const state = board.state.get();
    if (state.kind === 'populated') {
      // deferred: the session watch links sessions to the tickets known now, not to ones created later — close it when a new ticket's session shows no lamp.
      await sessions.watch(state.view.root, ticketIds(state.view));
    }
  };

  return {
    client,
    board,
    selection,
    sessions,
    reserved,
    load,
    start() {
      let ready = client.status.get().kind === 'ready';
      if (ready) {
        void load();
      }
      client.status.subscribe(() => {
        const nowReady = client.status.get().kind === 'ready';
        if (nowReady && !ready) {
          void load();
        }
        ready = nowReady;
      });
    },
  };
}

export type App = ReturnType<typeof createApp>;
