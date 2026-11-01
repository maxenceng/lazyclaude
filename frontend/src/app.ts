import { createBoardStore } from './board/boardStore';
import { ticketIds } from './board/rows';
import { createSelection } from './board/selection';
import { bindingsOf, type Binding } from './keys/chords';
import type { Client } from './protocol';
import { createStore } from './shared/store';
import { createSessionStore } from './session/sessionStore';

export type ConfigurationState =
  | { kind: 'reading' }
  | { kind: 'read'; bindings: readonly Binding[] }
  | { kind: 'fault'; message: string };

/** The stores one window reads, and the rule that loads them whenever the sidecar becomes ready. */
export function createApp(client: Client) {
  const board = createBoardStore(client);
  const selection = createSelection();
  const sessions = createSessionStore(client);

  const configuration = createStore<ConfigurationState>({ kind: 'reading' });
  const reserved = () => {
    const state = configuration.get();
    return state.kind === 'read' ? state.bindings : [];
  };

  const loadConfiguration = async () => {
    configuration.set({ kind: 'reading' });
    try {
      configuration.set({ kind: 'read', bindings: bindingsOf(await client.call('session/configuration', {})) });
    } catch (error) {
      configuration.set({ kind: 'fault', message: error instanceof Error ? error.message : String(error) });
    }
  };

  let watched = false;
  board.state.subscribe(() => {
    const state = board.state.get();
    if (watched || state.kind !== 'populated') {
      return;
    }
    watched = true;
    // deferred: the session watch links sessions to the tickets known now, not to ones created later — close it when a new ticket's session shows no lamp.
    void sessions.watch(state.view.root, ticketIds(state.view));
  });

  const load = async () => {
    watched = false;
    const loading = board.load();
    void loadConfiguration();
    await loading;
  };

  return {
    client,
    board,
    selection,
    sessions,
    configuration,
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
