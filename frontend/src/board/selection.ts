import type { BoardView } from '../protocol';
import { Store } from '../shared/store';
import { moveSelection } from './rows';

/** The ticket the cursor is on, held by id so a changed board keeps it where it can. */
export class Selection {
  readonly selected: Store<string | undefined> = new Store<string | undefined>(undefined);

  select(ticket: string): void {
    this.selected.set(ticket);
  }

  move(view: BoardView, delta: number): void {
    this.selected.set(moveSelection(view, this.selected.get(), delta));
  }
}
