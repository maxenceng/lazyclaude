import { describe, expect, it } from 'vitest';
import { LampColour } from './markers';
import { hostedFixture, pastFixture, sessionFixture, sessionsFixture, ticketSessionsFixture } from './fixtures';
import { HostedRow, LiveRow, NewRow, PastRow, rowsFor, SessionRowKind, STARTING } from './sessionRows';

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
    expect(rows[0]).toEqual(new HostedRow('h1', 'session', 'waitingForInput', '2026-01-01T00:00:00Z'));
    expect(rows[1]).toEqual(new LiveRow('session', 'busy', '2026-01-01T00:00:00Z'));
    expect((rows[2] as PastRow).session.id).toBe('p1');
  });

  it('lists the sessions waiting on you first, the strongest first, the others after them in the order they came', () => {
    const busy = sessionFixture({ id: 'busy', state: { kind: 'busy' }, ticket: 'PROJ-1' });
    const turn = sessionFixture({ id: 'turn', state: { kind: 'yourTurn' }, ticket: 'PROJ-1' });
    const permission = sessionFixture({ id: 'permission', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'Bash' } }, ticket: 'PROJ-1' });
    const input = sessionFixture({ id: 'input', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-1' });
    const hostedBusy = hostedFixture('hb', { ticket: 'PROJ-1', live: busy });
    const hostedTurn = hostedFixture('ht', { ticket: 'PROJ-1', live: turn });
    const view = sessionsFixture([busy, turn, permission, input], {
      hosted: [hostedBusy, hostedTurn],
      tickets: [ticketSessionsFixture('PROJ-1', { live: [busy, turn, permission, input], hosted: [hostedBusy, hostedTurn], past: [pastFixture({ id: 'p1', ticket: 'PROJ-1' })] })],
    });
    const rows = rowsFor(view, 'PROJ-1');
    expect(rows.map((row) => (row.kind === SessionRowKind.Hosted || row.kind === SessionRowKind.Live ? row.lamp : row.kind))).toEqual([
      LampColour.Permission,
      LampColour.Input,
      LampColour.Turn,
      LampColour.Off,
      SessionRowKind.Past,
      SessionRowKind.New,
    ]);
  });

  it('lists no session linked to no ticket under a selected ticket, an interrupted one included', () => {
    const unlinked = [pastFixture({ id: 'free' }), pastFixture({ id: 'cut', interrupted: true })];
    const view = sessionsFixture([], { past: unlinked, unlinked: { past: unlinked } });
    expect(rowsFor(view, 'PROJ-1')).toEqual([new NewRow()]);
  });

  it('lists no row when no ticket is selected, so the cursor cannot act on sessions nothing shows', () => {
    const hosted = hostedFixture('h1');
    const view = sessionsFixture([], { past: [pastFixture({ id: 'free' })], hosted: [hosted], unlinked: { past: [pastFixture({ id: 'free' })] } });
    expect(rowsFor(view, undefined)).toEqual([]);
  });

  it('lists a hosted session whose live record is not read yet as starting', () => {
    const hosted = hostedFixture('h1', { ticket: 'PROJ-1' });
    const view = sessionsFixture([], { hosted: [hosted], tickets: [ticketSessionsFixture('PROJ-1', { hosted: [hosted] })] });
    expect(rowsFor(view, 'PROJ-1')[0]).toEqual(new HostedRow('h1', undefined, STARTING));
  });
});
