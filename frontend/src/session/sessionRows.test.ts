import { describe, expect, it } from 'vitest';
import { LampColour } from './markers';
import { hostedFixture, pastFixture, sessionFixture, sessionsFixture, ticketSessionsFixture } from './fixtures';
import { HostedRow, LiveRow, NewRow, PastRow, rowsFor, SessionRowKind, UnlinkedRow } from './sessionRows';

describe('the sessions listed under a ticket', () => {
  it('lists only the row that starts a new session while no session was read', () => {
    expect(rowsFor(undefined, 'PROJ-1')).toEqual([new NewRow()]);
  });

  it('lists the hosted sessions the ticket lists first, with their lamp, then its other live ones, its past ones and the new row', () => {
    const waiting = sessionFixture({ id: 'h1', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-1' });
    const live = sessionFixture({ id: 'l1', ticket: 'PROJ-1' });
    const hosted = hostedFixture('h1', { ticket: 'PROJ-1', live: waiting });
    const past = pastFixture({ id: 'p1', title: 'Fix it', ticket: 'PROJ-1' });
    const view = sessionsFixture([waiting, live, sessionFixture({ id: 'other', ticket: 'PROJ-2' })], {
      hosted: [hosted, hostedFixture('h2', { ticket: 'PROJ-2' })],
      tickets: [ticketSessionsFixture('PROJ-1', { live: [waiting, live], past: [past], hosted: [hosted] }), ticketSessionsFixture('PROJ-2', { hosted: [hostedFixture('h2', { ticket: 'PROJ-2' })] })],
    });
    const rows = rowsFor(view, 'PROJ-1');
    expect(rows.map((row) => row.kind)).toEqual([SessionRowKind.Hosted, SessionRowKind.Live, SessionRowKind.Past, SessionRowKind.New]);
    expect(rows[0]).toEqual(new HostedRow('h1', 'session', LampColour.Input));
    expect(rows[1]).toEqual(new LiveRow('session', LampColour.Off));
    expect((rows[2] as PastRow).session.id).toBe('p1');
  });

  it('lists no session linked to no ticket under a selected ticket, an interrupted one included', () => {
    const unlinked = [pastFixture({ id: 'free' }), pastFixture({ id: 'cut', interrupted: true })];
    const view = sessionsFixture([], { past: unlinked, unlinked: { past: unlinked } });
    expect(rowsFor(view, 'PROJ-1')).toEqual([new NewRow()]);
  });

  it('lists the sessions linked to no ticket when no ticket is selected, without the new row, to resume where they were', () => {
    const view = sessionsFixture([], { past: [pastFixture({ id: 'free' })], unlinked: { past: [pastFixture({ id: 'free' })] } });
    const rows = rowsFor(view, undefined);
    expect(rows.map((row) => row.kind)).toEqual([SessionRowKind.Unlinked]);
    expect((rows[0] as UnlinkedRow).session.id).toBe('free');
  });

  it('lists a hosted session whose live record is not read yet as starting', () => {
    const hosted = hostedFixture('h1', { ticket: 'PROJ-1' });
    const view = sessionsFixture([], { hosted: [hosted], tickets: [ticketSessionsFixture('PROJ-1', { hosted: [hosted] })] });
    expect(rowsFor(view, 'PROJ-1')[0]).toEqual(new HostedRow('h1', undefined, LampColour.Off));
  });

  it('lists no more than five of the sessions linked to no ticket', () => {
    const past = ['a', 'b', 'c', 'd', 'e', 'f'].map((id) => pastFixture({ id }));
    expect(rowsFor(sessionsFixture([], { past, unlinked: { past } }), undefined)).toHaveLength(5);
  });
});
