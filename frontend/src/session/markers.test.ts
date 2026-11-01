import { describe, expect, it } from 'vitest';
import { sessionFixture, sessionsFixture } from './fixtures';
import { markerOfTicket, markersOf } from './markers';

const sessions = sessionsFixture([
  sessionFixture({ id: 's1', state: { kind: 'yourTurn' }, ticket: 'PROJ-1' }),
  sessionFixture({ id: 's2', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'edit a file' } }, ticket: 'PROJ-2' }),
  sessionFixture({ id: 's3', state: { kind: 'busy' }, ticket: 'PROJ-3' }),
  sessionFixture({ id: 's4', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-4' }),
  sessionFixture({ id: 's5', state: { kind: 'yourTurn' }, ticket: undefined }),
]);

describe('the markers of the live sessions', () => {
  it('gives one marker per live session, permission first, then input, then your turn, then unlit', () => {
    expect(markersOf(sessions).map((marker) => marker.colour)).toEqual(['permission', 'input', 'turn', 'turn', 'off']);
  });

  it('numbers a marker by the ticket its session is linked to', () => {
    expect(markersOf(sessions).map((marker) => marker.ticket)).toEqual(['PROJ-2', 'PROJ-4', 'PROJ-1', undefined, 'PROJ-3']);
  });

  it('marks a ticket with the strongest marker of its sessions', () => {
    expect(markerOfTicket(sessions, 'PROJ-2')).toBe('permission');
    expect(markerOfTicket(sessions, 'PROJ-1')).toBe('turn');
  });

  it('leaves a ticket without a session unmarked', () => {
    expect(markerOfTicket(sessions, 'PROJ-9')).toBeUndefined();
  });

  it('leaves a ticket whose session is busy unmarked', () => {
    expect(markerOfTicket(sessions, 'PROJ-3')).toBeUndefined();
  });

  it('marks every ticket a session is linked to, not only its first', () => {
    const linkedTwice = sessionsFixture([
      sessionFixture({ id: 's1', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, tickets: ['PROJ-1', 'PROJ-2'] }),
      sessionFixture({ id: 's2', state: { kind: 'yourTurn' }, ticket: 'PROJ-2' }),
    ]);
    expect(markerOfTicket(linkedTwice, 'PROJ-2')).toBe('input');
  });
});
