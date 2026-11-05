import type { BoardView, Client, TicketView } from '../protocol';
import { type BoardStore, createBoardStore } from './boardStore';
import { selectedIn, ticketIds } from './rows';
import { createSelection, type Selection } from './selection';

/** The Board: its one read, the ticket the cursor is on, and the moves of that cursor. */
export class BoardController {
  readonly store: BoardStore;
  readonly selection: Selection;

  constructor(client: Client) {
    this.store = createBoardStore(client);
    this.selection = createSelection();
  }

  shown(): BoardView | undefined {
    return this.store.state.get().shown();
  }

  root(): string | undefined {
    return this.shown()?.root;
  }

  ticketIds(): string[] {
    const view = this.shown();
    return view === undefined ? [] : ticketIds(view);
  }

  selectedTicket(): TicketView | undefined {
    const view = this.shown();
    const id = view === undefined ? undefined : selectedIn(view, this.selection.selected.get());
    return view?.groups.flatMap((group) => group.tickets).find((ticket) => ticket.id === id);
  }

  /** Moves the cursor `delta` tickets; false while there is no board to move over. */
  move(delta: number): boolean {
    const view = this.shown();
    if (view === undefined) {
      return false;
    }
    this.selection.move(view, delta);
    return true;
  }

  load(): Promise<void> {
    return this.store.load();
  }
}
