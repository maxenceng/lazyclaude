import type { BoardBoardView, BoardStatus, BoardTicketView } from '../protocol';

const STATUS_ORDER: readonly BoardStatus[] = ['IN_PROGRESS', 'IN_REVIEW', 'TODO', 'DRAFT', 'DONE'];

/** The Board tab's columns run the lifecycle left to right, so a ticket moves rightwards as it advances. */
const COLUMN_ORDER: readonly BoardStatus[] = ['DRAFT', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];

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

/** How many done tickets the Board tab shows: the latest closed, as the board orders them; the column's count stays the whole of it. */
const DONE_SHOWN = 10;

export interface Column {
  status: BoardStatus;
  tickets: BoardTicketView[];
  total: number;
}

/** The Board as status columns in lifecycle order: every status keeps its column, empty or not, and the finished one shows only its latest closed tickets. */
export const columnsOf = (view: BoardBoardView): Column[] => {
  return COLUMN_ORDER.map((status) => {
    const tickets = view.groups.find((group) => group.status === status)?.tickets ?? [];
    return { status, tickets: isFolded(status) ? tickets.slice(0, DONE_SHOWN) : tickets, total: tickets.length };
  });
};

/** The ticket the Board tab's cursor is on: the selected one while a column shows it, else the one Base's cursor is on, so a switch of tab keeps the ticket. */
export const selectedInColumns = (view: BoardBoardView, selected: string | undefined): string | undefined => {
  const ids = columnsOf(view).flatMap((column) => column.tickets.map((ticket) => ticket.id));
  return selected !== undefined && ids.includes(selected) ? selected : selectedIn(view, selected);
};

const placeOf = (columns: Column[], selected: string | undefined): { column: number; row: number } | undefined => {
  const column = columns.findIndex((candidate) => candidate.tickets.some((ticket) => ticket.id === selected));
  return column === -1 ? undefined : { column, row: columns[column]?.tickets.findIndex((ticket) => ticket.id === selected) ?? 0 };
};

/** The ticket `delta` rows from the cursor inside its column, held at the column's ends. */
export const moveInColumn = (view: BoardBoardView, selected: string | undefined, delta: number): string | undefined => {
  const columns = columnsOf(view);
  const at = placeOf(columns, selectedInColumns(view, selected));
  const tickets = at === undefined ? [] : (columns[at.column]?.tickets ?? []);
  return at === undefined ? undefined : tickets[Math.min(tickets.length - 1, Math.max(0, at.row + delta))]?.id;
};

/** The ticket in the next column `direction` away that holds one, at the cursor's row or the last it has; the cursor stays when none does. */
export const moveAcross = (view: BoardBoardView, selected: string | undefined, direction: 1 | -1): string | undefined => {
  const columns = columnsOf(view);
  const current = selectedInColumns(view, selected);
  const at = placeOf(columns, current);
  if (at === undefined) {
    return current;
  }
  const across = columns.slice(direction === 1 ? at.column + 1 : 0, direction === 1 ? undefined : at.column);
  const target = direction === 1 ? across.find((column) => column.tickets.length > 0) : across.findLast((column) => column.tickets.length > 0);
  return target === undefined ? current : target.tickets[Math.min(target.tickets.length - 1, at.row)]?.id;
};

/** The ticket the Board tab's cursor is on, as its preview shows it. */
export const cursorTicketInColumns = (view: BoardBoardView, selected: string | undefined): BoardTicketView | undefined => {
  const id = selectedInColumns(view, selected);
  return columnsOf(view)
    .flatMap((column) => column.tickets)
    .find((ticket) => ticket.id === id);
};

/** Whether the Board tab may go to this ticket on Base: Base folds the done ones, so a done card is only previewed. */
export const goesToBase = (ticket: BoardTicketView): boolean => !isFolded(ticket.status);

/** How many tickets are in progress, which the Board tab reports as its finding. */
export const inProgressCount = (view: BoardBoardView): number => view.groups.find((group) => group.status === 'IN_PROGRESS')?.tickets.length ?? 0;
