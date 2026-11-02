import { createBoardStore } from './board/boardStore';
import { ticketIds } from './board/rows';
import { createSelection } from './board/selection';
import { bindingsOf } from './keys/chords';
import { type ConfigurationState, FaultedConfiguration, ReadConfiguration, ReadingConfiguration } from './keys/configurationState';
import type { Client } from './protocol';
import { createStore } from './shared/store';
import { createSessionStore } from './session/sessionStore';

/** The stores one window reads, and the rule that loads them whenever the sidecar becomes ready. */
export const createApp = (client: Client) => {
  const board = createBoardStore(client);
  const selection = createSelection();
  const sessions = createSessionStore(client);

  const configuration = createStore<ConfigurationState>(new ReadingConfiguration());
  const reserved = () => configuration.get().reserved();

  const loadConfiguration = async () => {
    configuration.set(new ReadingConfiguration());
    try {
      configuration.set(new ReadConfiguration(bindingsOf(await client.call('session/configuration', {}))));
    } catch (error) {
      configuration.set(new FaultedConfiguration(error instanceof Error ? error.message : String(error)));
    }
  };

  let watched = false;
  board.state.subscribe(() => {
    const view = board.state.get().shown();
    if (watched || view === undefined) {
      return;
    }
    watched = true;
    // deferred: the session watch links sessions to the tickets known now, not to ones created later — close it when a new ticket's session shows no lamp.
    void sessions.watch(view.root, ticketIds(view));
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
      let ready = client.status.get().isReady();
      if (ready) {
        void load();
      }
      client.status.subscribe(() => {
        const nowReady = client.status.get().isReady();
        if (nowReady && !ready) {
          void load();
        }
        ready = nowReady;
      });
    },
  };
};

export type App = ReturnType<typeof createApp>;
