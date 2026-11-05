import { describe, expect, it } from 'vitest';
import { LampColour } from './markers';
import { hostedFixture, pastFixture, sessionFixture, sessionsFixture } from './fixtures';
import { HostedRow, LiveRow, NewRow, PastRow, rowsFor, SessionRowKind, UnlinkedRow } from './sessionRows';

describe('the sessions listed under a ticket', () => {
  it('lists only the row that starts a new session while no session was read', () => {
    expect(rowsFor(undefined, 'PROJ-1')).toEqual([new NewRow()]);
  });

  it('lists the hosted sessions of the ticket first, with their lamp, then its other live ones, its past ones and the new row', () => {
    const waiting = sessionFixture({ id: 'h1', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-1' });
    const view = sessionsFixture([waiting, sessionFixture({ id: 'l1', ticket: 'PROJ-1' }), sessionFixture({ id: 'other', ticket: 'PROJ-2' })], {
      hosted: [hostedFixture('h1', { ticket: 'PROJ-1', live: waiting }), hostedFixture('h2', { ticket: 'PROJ-2' })],
      past: [pastFixture({ id: 'p1', title: 'Fix it', ticket: 'PROJ-1' }), pastFixture({ id: 'p2', ticket: 'PROJ-2' })],
    });
    const rows = rowsFor(view, 'PROJ-1');
    expect(rows.map((row) => row.kind)).toEqual([SessionRowKind.Hosted, SessionRowKind.Live, SessionRowKind.Past, SessionRowKind.New]);
    expect(rows[0]).toEqual(new HostedRow('h1', 'session', LampColour.Input));
    expect(rows[1]).toEqual(new LiveRow('session', LampColour.Off, '1m'));
    expect((rows[2] as PastRow).session.id).toBe('p1');
  });

  it('lists a past session linked to no ticket after the new row, to resume where it was', () => {
    const view = sessionsFixture([], { past: [pastFixture({ id: 'free' })] });
    const rows = rowsFor(view, 'PROJ-1');
    expect(rows.map((row) => row.kind)).toEqual([SessionRowKind.New, SessionRowKind.Unlinked]);
    expect((rows[1] as UnlinkedRow).session.id).toBe('free');
  });

  it('lists the sessions linked to no ticket when no ticket is selected, without the new row', () => {
    const view = sessionsFixture([], { past: [pastFixture({ id: 'free' })] });
    expect(rowsFor(view, undefined).map((row) => row.kind)).toEqual([SessionRowKind.Unlinked]);
  });

  it('lists a hosted session whose live record is not read yet as starting', () => {
    const view = sessionsFixture([], { hosted: [hostedFixture('h1', { ticket: 'PROJ-1' })] });
    expect(rowsFor(view, 'PROJ-1')[0]).toEqual(new HostedRow('h1', undefined, LampColour.Off));
  });

  it('lists no more than five of the sessions linked to no ticket', () => {
    const past = ['a', 'b', 'c', 'd', 'e', 'f'].map((id) => pastFixture({ id }));
    expect(rowsFor(sessionsFixture([], { past }), undefined)).toHaveLength(5);
  });
});
