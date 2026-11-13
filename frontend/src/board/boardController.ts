import type { BoardBoardAfterCaptureView, BoardBoardView, Client, BoardTicketView } from '../protocol';
import { BoardStore } from './boardStore';
import { CaptureController } from './captureController';
import { columnsOf, selectedIn, ticketIds } from './rows';
import { Selection } from './selection';

/** The Board: its one read, the ticket the cursor is on, and the moves of that cursor. */
export class BoardController {
  readonly store: BoardStore;
  readonly selection: Selection;
  readonly capture: CaptureController;

  constructor(client: Client) {
    this.store = new BoardStore(client);
    this.selection = new Selection();
    this.capture = new CaptureController({ client, root: () => this.root(), onCaptured: (id, board) => this.showCaptured(id, board) });
  }

  shown(): BoardBoardView | undefined {
    return this.store.state.get().shown();
  }

  root(): string | undefined {
    return this.store.state.get().root();
  }

  ticketIds(): string[] {
    const view = this.shown();
    return view === undefined ? [] : ticketIds(view);
  }

  /** The branches of the tickets on show, done cut to its latest closed, which `review` joins to their pull requests. */
  branches(): string[] {
    const view = this.shown();
    return view === undefined ? [] : columnsOf(view).flatMap((column) => column.tickets.map((ticket) => ticket.branch));
  }

  selectedTicket(): BoardTicketView | undefined {
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

  /** The new ticket is selected on the board its capture was answered with; with no board read, on the board shown, if it is there. */
  private showCaptured(id: string, board: BoardBoardAfterCaptureView): void {
    if (board.kind !== 'noBoardAfterCapture') {
      this.store.show(board);
    }
    if (this.ticketIds().includes(id)) {
      this.selection.select(id);
    }
  }
}
