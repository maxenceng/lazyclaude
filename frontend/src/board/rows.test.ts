import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './fixtures';
import { columnsOf, inProgressCount, isFolded, moveAcross, moveInColumn, moveSelection, orderedGroups, selectedIn, ticketIds } from './rows';

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

  it('folds the finished status by default and no other', () => {
    expect(orderedGroups(view).map((group) => [group.status, isFolded(group.status)])).toEqual([
      ['IN_PROGRESS', false],
      ['IN_REVIEW', false],
      ['TODO', false],
      ['DONE', true],
    ]);
  });

  it('lists the ids of the tickets shown, which leaves out the folded ones', () => {
    expect(ticketIds(view)).toEqual(['PROJ-3', 'PROJ-4', 'PROJ-2', 'PROJ-5']);
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
    expect(moveSelection(view, 'PROJ-5', 1)).toBe('PROJ-5');
  });

  it('selects nothing on a board without tickets', () => {
    expect(moveSelection(boardFixture([]), undefined, 1)).toBeUndefined();
  });

  it('lays the board out as every status in order, the ones with no ticket kept and done narrow', () => {
    expect(columnsOf(view).map((column) => [column.status, column.tickets.length, column.narrow])).toEqual([
      ['IN_PROGRESS', 1, false],
      ['IN_REVIEW', 1, false],
      ['TODO', 2, false],
      ['DRAFT', 0, false],
      ['DONE', 1, true],
    ]);
  });

  it('moves down and up inside one column and stops at its ends', () => {
    expect(moveInColumn(view, 'PROJ-2', 1)).toBe('PROJ-5');
    expect(moveInColumn(view, 'PROJ-5', 1)).toBe('PROJ-5');
    expect(moveInColumn(view, 'PROJ-5', -1)).toBe('PROJ-2');
    expect(moveInColumn(view, 'PROJ-2', -1)).toBe('PROJ-2');
  });

  it('moves across to the next column that holds a ticket, at the same row or the last one it has', () => {
    expect(moveAcross(view, 'PROJ-3', 1)).toBe('PROJ-4');
    expect(moveAcross(view, 'PROJ-4', 1)).toBe('PROJ-2');
    expect(moveAcross(view, 'PROJ-5', -1)).toBe('PROJ-4');
    expect(moveAcross(view, 'PROJ-2', -1)).toBe('PROJ-4');
  });

  it('stays in its column when no column across holds a ticket', () => {
    expect(moveAcross(view, 'PROJ-3', -1)).toBe('PROJ-3');
    expect(moveAcross(view, 'PROJ-5', 1)).toBe('PROJ-5');
  });

  it('moves from the first ticket when the cursor is on none', () => {
    expect(moveAcross(view, undefined, 1)).toBe('PROJ-4');
    expect(moveInColumn(view, undefined, 1)).toBe('PROJ-3');
  });

  it('counts the tickets in progress, none on a board with none', () => {
    expect(inProgressCount(view)).toBe(1);
    expect(inProgressCount(boardFixture([ticketFixture({ status: 'TODO' })]))).toBe(0);
  });
});
