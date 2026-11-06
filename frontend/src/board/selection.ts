import type { BoardBoardView } from '../protocol';
import { Store } from '../shared/store';
import { moveAcross, moveInColumn, moveSelection } from './rows';

/** The ticket the cursor is on, held by id so a changed board keeps it where it can. */
export class Selection {
  readonly selected: Store<string | undefined> = new Store<string | undefined>(undefined);

  select(ticket: string): void {
    this.selected.set(ticket);
  }

  move(view: BoardBoardView, delta: number): void {
    this.selected.set(moveSelection(view, this.selected.get(), delta));
  }

  moveInColumn(view: BoardBoardView, delta: number): void {
    this.selected.set(moveInColumn(view, this.selected.get(), delta));
  }

  moveAcross(view: BoardBoardView, direction: 1 | -1): void {
    this.selected.set(moveAcross(view, this.selected.get(), direction));
  }
}
