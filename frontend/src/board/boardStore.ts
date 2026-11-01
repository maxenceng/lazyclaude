import type { BoardLookupView, BoardView, Client, Store } from '../protocol';
import { createStore } from '../protocol';

export type BoardState =
  | { kind: 'loading' }
  | { kind: 'empty' }
  | { kind: 'unavailable'; cause: 'noProject' | 'unavailableBoard'; reason: string }
  | { kind: 'fault'; message: string }
  | { kind: 'populated'; view: BoardView };

export interface BoardStore {
  readonly state: Store<BoardState>;
  load(): Promise<void>;
}

const PROJECT_DIRECTORY = '.';

function stateOf(view: BoardLookupView): BoardState {
  switch (view.kind) {
    case 'board':
      return view.groups.every((group) => group.tickets.length === 0) && view.unreadable.length === 0 ? { kind: 'empty' } : { kind: 'populated', view };
    case 'unavailableBoard':
    case 'noProject':
      return { kind: 'unavailable', cause: view.kind, reason: view.reason };
  }
}

/** The Board's one read: `board/watch`'s answer, then every `board/changed` after it. */
export function createBoardStore(client: Client): BoardStore {
  const state = createStore<BoardState>({ kind: 'loading' });
  client.subscribe('board/changed', (view) => state.set(stateOf(view)));
  return {
    state,
    async load() {
      state.set({ kind: 'loading' });
      try {
        state.set(stateOf(await client.call('board/watch', { from: PROJECT_DIRECTORY })));
      } catch (error) {
        state.set({ kind: 'fault', message: error instanceof Error ? error.message : String(error) });
      }
    },
  };
}
