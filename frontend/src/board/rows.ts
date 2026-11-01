import type { BoardView, TicketStatus } from '../protocol';

const STATUS_ORDER: readonly TicketStatus[] = ['IN_PROGRESS', 'IN_REVIEW', 'TODO', 'DRAFT', 'DONE'];

export type Group = BoardView['groups'][number];

/** The groups as the Board shows them: what is under way heads it, done closes it, every status keeps its heading. */
export function orderedGroups(view: BoardView): Group[] {
  return [...view.groups].sort((a, b) => STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status));
}

/** The finished status is folded to its heading and count; a folded group shows no ticket. */
export function isFolded(status: TicketStatus): boolean {
  return status === 'DONE';
}

/** The ids of the tickets on show, in order: the folded ones are not, so the cursor never lands on one. */
export function ticketIds(view: BoardView): string[] {
  return orderedGroups(view)
    .filter((group) => !isFolded(group.status))
    .flatMap((group) => group.tickets.map((ticket) => ticket.id));
}

/** The ticket the cursor is on: the selected one while it is on the board, else the one that heads it. */
export function selectedIn(view: BoardView, selected: string | undefined): string | undefined {
  const ids = ticketIds(view);
  return selected !== undefined && ids.includes(selected) ? selected : ids[0];
}

/** The ticket `delta` rows from the cursor, held at the ends. */
export function moveSelection(view: BoardView, selected: string | undefined, delta: number): string | undefined {
  const ids = ticketIds(view);
  const at = ids.indexOf(selectedIn(view, selected) ?? '');
  return at === -1 ? undefined : ids[Math.min(ids.length - 1, Math.max(0, at + delta))];
}

export function statusLabel(status: TicketStatus): string {
  return status.replace('_', ' ').toLowerCase();
}
