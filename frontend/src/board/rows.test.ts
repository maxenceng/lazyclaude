import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './fixtures';
import {
  columnsOf,
  cursorTicketInColumns,
  inProgressCount,
  moveAcross,
  moveInColumn,
  moveSelection,
  orderedGroups,
  shownTickets,
  selectedIn,
  selectedInColumns,
  statusesInOrder,
  ticketIds,
  ticketTotal,
} from './rows';

const view = boardFixture([
  ticketFixture({ id: 'PROJ-1', status: 'DONE' }),
  ticketFixture({ id: 'PROJ-2', status: 'TODO' }),
  ticketFixture({ id: 'PROJ-3', status: 'IN_PROGRESS' }),
  ticketFixture({ id: 'PROJ-4', status: 'IN_REVIEW' }),
  ticketFixture({ id: 'PROJ-5', status: 'TODO' }),
]);

describe('the board rows', () => {
  it('puts the in-progress tickets first and done last', () => {
    expect(orderedGroups(view).map((group) => group.status)).toEqual(['IN_PROGRESS', 'IN_REVIEW', 'TODO', 'DONE']);
  });

  it('keeps a status that has no ticket, as the TUI shows its heading and count', () => {
    const withEmpty = { ...view, groups: [...view.groups, { status: 'DRAFT' as const, tickets: [] }] };
    expect(orderedGroups(withEmpty).map((group) => group.status)).toContain('DRAFT');
  });

  it('lists the ids of the tickets shown, live groups first and the done ones last', () => {
    expect(ticketIds(view)).toEqual(['PROJ-3', 'PROJ-4', 'PROJ-2', 'PROJ-5', 'PROJ-1']);
  });

  it('lists only the ten latest closed done tickets, in the board order, while the group keeps them all', () => {
    const done = Array.from({ length: 11 }, (_, at) => ticketFixture({ id: `PROJ-${String(at + 10)}`, status: 'DONE' }));
    const group = orderedGroups(boardFixture(done)).find((candidate) => candidate.status === 'DONE') ?? { status: 'DONE' as const, tickets: [] };
    expect(shownTickets(group).map((ticket) => ticket.id)).toEqual(done.slice(0, 10).map((ticket) => ticket.id));
    expect(group.tickets).toHaveLength(11);
    expect(ticketIds(boardFixture(done))).not.toContain('PROJ-20');
  });

  it('moves the cursor from the last live row into done and stops on the last done row', () => {
    expect(moveSelection(view, 'PROJ-5', 1)).toBe('PROJ-1');
    expect(moveSelection(view, 'PROJ-1', 1)).toBe('PROJ-1');
    expect(moveSelection(view, 'PROJ-1', -1)).toBe('PROJ-5');
  });

  it('starts the selection on the first ticket shown, the one that heads the board', () => {
    expect(selectedIn(view, undefined)).toBe('PROJ-3');
  });

  it('keeps the selected ticket while it is on the board and falls to the first when it left', () => {
    expect(selectedIn(view, 'PROJ-2')).toBe('PROJ-2');
    expect(selectedIn(view, 'PROJ-99')).toBe('PROJ-3');
  });

  it('moves from the first ticket when none is selected yet', () => {
    expect(moveSelection(view, undefined, 1)).toBe('PROJ-4');
  });

  it('moves the selection down and up through the groups', () => {
    expect(moveSelection(view, 'PROJ-4', 1)).toBe('PROJ-2');
    expect(moveSelection(view, 'PROJ-2', -1)).toBe('PROJ-4');
  });

  it('stays on the first and last tickets at the ends', () => {
    expect(moveSelection(view, 'PROJ-3', -1)).toBe('PROJ-3');
    expect(moveSelection(view, 'PROJ-1', 1)).toBe('PROJ-1');
  });

  it('selects nothing on a board without tickets', () => {
    expect(moveSelection(boardFixture([]), undefined, 1)).toBeUndefined();
  });

  it('lays the board out as every status in lifecycle order, draft first, the ones with no ticket kept', () => {
    expect(columnsOf(view).map((column) => [column.status, column.tickets.length, column.total])).toEqual([
      ['DRAFT', 0, 0],
      ['TODO', 2, 2],
      ['IN_PROGRESS', 1, 1],
      ['IN_REVIEW', 1, 1],
      ['DONE', 1, 1],
    ]);
  });

  it('shows the ten latest closed done tickets, in the order the board gives, and counts them all', () => {
    const done = Array.from({ length: 12 }, (_, at) => ticketFixture({ id: `PROJ-${String(at + 10)}`, status: 'DONE' }));
    const column = columnsOf(boardFixture(done)).find((candidate) => candidate.status === 'DONE');
    expect(column?.tickets.map((ticket) => ticket.id)).toEqual(done.slice(0, 10).map((ticket) => ticket.id));
    expect(column?.total).toBe(12);
  });

  it('keeps the cursor on a done card, which Base lists as well', () => {
    expect(selectedInColumns(view, 'PROJ-1')).toBe('PROJ-1');
    expect(selectedIn(view, 'PROJ-1')).toBe('PROJ-1');
    expect(cursorTicketInColumns(view, 'PROJ-1')?.id).toBe('PROJ-1');
  });

  it('moves down and up inside one column and stops at its ends', () => {
    expect(moveInColumn(view, 'PROJ-2', 1)).toBe('PROJ-5');
    expect(moveInColumn(view, 'PROJ-5', 1)).toBe('PROJ-5');
    expect(moveInColumn(view, 'PROJ-5', -1)).toBe('PROJ-2');
    expect(moveInColumn(view, 'PROJ-2', -1)).toBe('PROJ-2');
  });

  it('moves across to the next column that holds a ticket, at the same row or the last one it has', () => {
    expect(moveAcross(view, 'PROJ-3', 1)).toBe('PROJ-4');
    expect(moveAcross(view, 'PROJ-2', 1)).toBe('PROJ-3');
    expect(moveAcross(view, 'PROJ-5', 1)).toBe('PROJ-3');
    expect(moveAcross(view, 'PROJ-4', -1)).toBe('PROJ-3');
  });

  it('stays in its column when no column across holds a ticket', () => {
    expect(moveAcross(view, 'PROJ-2', -1)).toBe('PROJ-2');
    expect(moveAcross(view, 'PROJ-1', 1)).toBe('PROJ-1');
  });

  it('moves across into the done column', () => {
    expect(moveAcross(view, 'PROJ-4', 1)).toBe('PROJ-1');
  });

  it('moves from where Base\'s cursor is when the cursor is on none', () => {
    expect(moveAcross(view, undefined, 1)).toBe('PROJ-4');
    expect(moveInColumn(view, undefined, 1)).toBe('PROJ-3');
  });

  it('counts the tickets in progress, none on a board with none', () => {
    expect(inProgressCount(view)).toBe(1);
    expect(inProgressCount(boardFixture([ticketFixture({ status: 'TODO' })]))).toBe(0);
  });
});

describe('the board\'s totals', () => {
  it('counts every ticket in every status, the finished ones past the ten shown included', () => {
    const done = Array.from({ length: 12 }, (_, at) => ticketFixture({ id: `PROJ-${at + 10}`, status: 'DONE' }));
    expect(ticketTotal(view)).toBe(5);
    expect(ticketTotal(boardFixture([...done, ticketFixture({ id: 'PROJ-1', status: 'TODO' })]))).toBe(13);
    expect(ticketTotal(boardFixture([]))).toBe(0);
  });

  it('lists the statuses in the order Base shows them', () => {
    expect(statusesInOrder()).toEqual(['IN_PROGRESS', 'IN_REVIEW', 'TODO', 'DRAFT', 'DONE']);
  });
});
