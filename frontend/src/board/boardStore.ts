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

  private show(view: BoardBoardLookupView): void {
    this.state.set(stateOf(view));
  }

  /** Reads the board again where it stands: what is shown stays until the answer replaces it, and a failed read leaves it. */
  async refresh(): Promise<void> {
    try {
      this.state.set(stateOf(await this.client.call('board/watch', { from: PROJECT_DIRECTORY })));
    } catch {
      return;
    }
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
