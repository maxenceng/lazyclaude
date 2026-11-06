import { describe, expect, it } from 'vitest';
import { boardFixture, ticketFixture } from './fixtures';
import { Selection } from './selection';

const view = boardFixture([ticketFixture({ id: 'PROJ-1' }), ticketFixture({ id: 'PROJ-2' })]);

describe('the board selection', () => {
  it('moves down from the first ticket, which is where the cursor starts', () => {
    const selection = new Selection();
    selection.move(view, 1);
    expect(selection.selected.get()).toBe('PROJ-2');
  });

  it('moves down and up one ticket', () => {
    const selection = new Selection();
    selection.move(view, 1);
    expect(selection.selected.get()).toBe('PROJ-2');
    selection.move(view, -1);
    expect(selection.selected.get()).toBe('PROJ-1');
  });

  it('selects the ticket it is told to', () => {
    const selection = new Selection();
    selection.select('PROJ-2');
    expect(selection.selected.get()).toBe('PROJ-2');
  });

  it('moves within a column and across columns for the status columns', () => {
    const columns = boardFixture([
      ticketFixture({ id: 'PROJ-1', status: 'TODO' }),
      ticketFixture({ id: 'PROJ-2', status: 'TODO' }),
      ticketFixture({ id: 'PROJ-3', status: 'IN_REVIEW' }),
    ]);
    const selection = new Selection();
    selection.selected.set('PROJ-1');
    selection.moveAcross(columns, 1);
    expect(selection.selected.get()).toBe('PROJ-3');
    selection.moveAcross(columns, -1);
    expect(selection.selected.get()).toBe('PROJ-1');
    selection.moveInColumn(columns, 1);
    expect(selection.selected.get()).toBe('PROJ-2');
  });
});
