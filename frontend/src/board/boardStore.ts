import type { BoardLookupView, Client } from '../protocol';
import { createStore, type Store } from '../shared/store';
import { type BoardState, EmptyBoard, FaultedBoard, LoadingBoard, PopulatedBoard, UnavailableBoard, UnavailableCause } from './boardState';

export interface BoardStore {
  readonly state: Store<BoardState>;
  load(): Promise<void>;
}

const PROJECT_DIRECTORY = '.';

const stateOf = (view: BoardLookupView): BoardState => {
  switch (view.kind) {
    case 'board':
      return view.groups.every((group) => group.tickets.length === 0) && view.unreadable.length === 0 ? new EmptyBoard() : new PopulatedBoard(view);
    case 'unavailableBoard':
      return new UnavailableBoard(UnavailableCause.UnavailableBoard, view.reason);
    case 'noProject':
      return new UnavailableBoard(UnavailableCause.NoProject, view.reason);
  }
};

/** The Board's one read: `board/watch`'s answer, then every `board/changed` after it. */
export const createBoardStore = (client: Client): BoardStore => {
  const state = createStore<BoardState>(new LoadingBoard());
  client.subscribe('board/changed', (view) => state.set(stateOf(view)));
  return {
    state,
    async load() {
      state.set(new LoadingBoard());
      try {
        state.set(stateOf(await client.call('board/watch', { from: PROJECT_DIRECTORY })));
      } catch (error) {
        state.set(new FaultedBoard(error instanceof Error ? error.message : String(error)));
      }
    },
  };
};
