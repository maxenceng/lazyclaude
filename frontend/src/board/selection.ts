import type { BoardView, Store } from '../protocol';
import { createStore } from '../protocol';
import { moveSelection } from './rows';

export interface Selection {
  readonly selected: Store<string | undefined>;
  select(ticket: string): void;
  move(view: BoardView, delta: number): void;
}

/** The ticket the cursor is on, held by id so a changed board keeps it where it can. */
export function createSelection(): Selection {
  const selected = createStore<string | undefined>(undefined);
  return {
    selected,
    select: (ticket) => selected.set(ticket),
    move: (view, delta) => selected.set(moveSelection(view, selected.get(), delta)),
  };
}
