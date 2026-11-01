import { describe, expect, it } from 'vitest';
import { sessionFixture, sessionsFixture } from './fixtures';
import { lampOfTicket, lampsOf } from './lamps';

const sessions = sessionsFixture([
  sessionFixture({ id: 's1', state: { kind: 'yourTurn' }, ticket: 'PROJ-1' }),
  sessionFixture({ id: 's2', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'edit a file' } }, ticket: 'PROJ-2' }),
  sessionFixture({ id: 's3', state: { kind: 'busy' }, ticket: 'PROJ-3' }),
  sessionFixture({ id: 's4', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-4' }),
  sessionFixture({ id: 's5', state: { kind: 'yourTurn' }, ticket: undefined }),
]);

describe('the lamps of the live sessions', () => {
  it('gives one lamp per live session, permission first, then input, then your turn, then unlit', () => {
    expect(lampsOf(sessions).map((lamp) => lamp.colour)).toEqual(['permission', 'input', 'turn', 'turn', 'off']);
  });

  it('numbers a lamp by the ticket its session is linked to', () => {
    expect(lampsOf(sessions).map((lamp) => lamp.ticket)).toEqual(['PROJ-2', 'PROJ-4', 'PROJ-1', undefined, 'PROJ-3']);
  });

  it('lights a ticket with the most urgent lamp of its sessions', () => {
    expect(lampOfTicket(sessions, 'PROJ-2')).toBe('permission');
    expect(lampOfTicket(sessions, 'PROJ-1')).toBe('turn');
  });

  it('leaves a ticket without a session unlit', () => {
    expect(lampOfTicket(sessions, 'PROJ-9')).toBeUndefined();
  });

  it('leaves a ticket whose session is busy unlit', () => {
    expect(lampOfTicket(sessions, 'PROJ-3')).toBeUndefined();
  });
});
