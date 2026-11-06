import type { BoardBoardView, BoardStatus } from '../protocol';

const STATUS_ORDER: readonly BoardStatus[] = ['IN_PROGRESS', 'IN_REVIEW', 'TODO', 'DRAFT', 'DONE'];

export type Group = BoardBoardView['groups'][number];

/** The groups as the Board shows them: what is under way heads it, done closes it, every status keeps its heading. */
export const orderedGroups = (view: BoardBoardView): Group[] => {
  return [...view.groups].sort((a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status));
};

/** The finished status is folded to its heading and count; a folded group shows no ticket. */
export const isFolded = (status: BoardStatus): boolean => {
  return status === 'DONE';
};

/** The ids of the tickets on show, in order: the folded ones are not, so the cursor never lands on one. */
export const ticketIds = (view: BoardBoardView): string[] => {
  return orderedGroups(view)
    .filter((group) => !isFolded(group.status))
    .flatMap((group) => group.tickets.map((ticket) => ticket.id));
};

/** The ticket the cursor is on: the selected one while it is on the board, else the one that heads it. */
export const selectedIn = (view: BoardBoardView, selected: string | undefined): string | undefined => {
  const ids = ticketIds(view);
  return selected !== undefined && ids.includes(selected) ? selected : ids[0];
};

/** The ticket `delta` rows from the cursor, held at the ends. */
export const moveSelection = (view: BoardBoardView, selected: string | undefined, delta: number): string | undefined => {
  const ids = ticketIds(view);
  const at = ids.indexOf(selectedIn(view, selected) ?? '');
  return at === -1 ? undefined : ids[Math.min(ids.length - 1, Math.max(0, at + delta))];
};
