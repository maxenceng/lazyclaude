import type { BoardBoardLookupView, Client } from '../protocol';
import { Store } from '../shared/store';
import { type BoardState, EmptyBoard, FaultedBoard, LoadingBoard, PopulatedBoard, UnavailableBoard, UnavailableCause } from './boardState';

const PROJECT_DIRECTORY = '.';

const stateOf = (view: BoardBoardLookupView): BoardState => {
  switch (view.kind) {
    case 'board':
      return view.groups.every((group) => group.tickets.length === 0) ? new EmptyBoard(view.root) : new PopulatedBoard(view);
    case 'unavailableBoard':
      return new UnavailableBoard(UnavailableCause.UnavailableBoard, view.reason);
    case 'noProject':
      return new UnavailableBoard(UnavailableCause.NoProject, view.reason);
  }
};

/** The Board's one read: `board/watch`'s answer, then every `board/changed` after it. */
export class BoardStore {
  readonly state: Store<BoardState> = new Store<BoardState>(new LoadingBoard());

  constructor(private readonly client: Client) {
    client.subscribe('board/changed', (view) => this.show(view));
  }

  /** Shows a board the sidecar read for another answer, as a capture's. */
  show(view: BoardBoardLookupView): void {
    this.state.set(stateOf(view));
  }

  async load(): Promise<void> {
    this.state.set(new LoadingBoard());
    try {
      this.state.set(stateOf(await this.client.call('board/watch', { from: PROJECT_DIRECTORY })));
    } catch (error) {
      this.state.set(new FaultedBoard(error instanceof Error ? error.message : String(error)));
    }
  }
}
