import { describe, expect, it } from 'vitest';
import { sessionFixture, sessionsFixture } from './fixtures';
import { colourOf, LampColour, markerOfTicket, lampTilesOf } from './markers';

const sessions = sessionsFixture([
  sessionFixture({ id: 's1', state: { kind: 'yourTurn' }, ticket: 'PROJ-1' }),
  sessionFixture({ id: 's2', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'edit a file' } }, ticket: 'PROJ-2' }),
  sessionFixture({ id: 's3', state: { kind: 'busy' }, ticket: 'PROJ-3' }),
  sessionFixture({ id: 's4', state: { kind: 'waitingForInput', reason: { kind: 'noWaitingReason' } }, ticket: 'PROJ-4' }),
  sessionFixture({ id: 's5', state: { kind: 'yourTurn' }, ticket: undefined }),
]);

describe('the markers of the live sessions', () => {
  it('gives one marker per live session, permission first, then input, then your turn, then unlit', () => {
    expect(lampTilesOf(sessions).map((tile) => tile.colour)).toEqual([LampColour.Permission, LampColour.Input, LampColour.Turn, LampColour.Turn, LampColour.Off]);
  });

  it('numbers a marker by the ticket its session is linked to', () => {
    expect(lampTilesOf(sessions).map((tile) => tile.ticket)).toEqual(['PROJ-2', 'PROJ-4', 'PROJ-1', undefined, 'PROJ-3']);
  });

  it('marks a ticket with the strongest marker of its sessions', () => {
    expect(markerOfTicket(sessions, 'PROJ-2')).toBe(LampColour.Permission);
    expect(markerOfTicket(sessions, 'PROJ-1')).toBe(LampColour.Turn);
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
    expect(markerOfTicket(linkedTwice, 'PROJ-2')).toBe(LampColour.Input);
  });

  it('orders by the strength the wire carries, not by what the state would imply', () => {
    const inverted = sessionsFixture([
      sessionFixture({ id: 'a', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'Bash' } }, marker: { kind: 'WAITING_FOR_PERMISSION', strength: 1 } }),
      sessionFixture({ id: 'b', state: { kind: 'yourTurn' }, marker: { kind: 'YOUR_TURN', strength: 9 } }),
    ]);
    expect(lampTilesOf(inverted).map((tile) => tile.session)).toEqual(['b', 'a']);
  });

  it('sorts a session with no marker after every one carrying one', () => {
    const view = sessionsFixture([sessionFixture({ id: 'none' }), sessionFixture({ id: 'turn', state: { kind: 'yourTurn' } })]);
    expect(lampTilesOf(view).map((tile) => tile.session)).toEqual(['turn', 'none']);
  });

  it('keeps the order equal markers came in', () => {
    const view = sessionsFixture(['x', 'y', 'z'].map((id) => sessionFixture({ id, state: { kind: 'yourTurn' } })));
    expect(lampTilesOf(view).map((tile) => tile.session)).toEqual(['x', 'y', 'z']);
  });

  it('draws the unlit lamp for a session with no marker', () => {
    expect(colourOf(sessionFixture({ state: { kind: 'busy' } }))).toBe(LampColour.Off);
  });

  it('marks a ticket by the greatest strength among its sessions', () => {
    const view = sessionsFixture([
      sessionFixture({ id: 'a', state: { kind: 'waitingForPermission', reason: { kind: 'waitingReason', value: 'Bash' } }, marker: { kind: 'WAITING_FOR_PERMISSION', strength: 1 }, ticket: 'PROJ-1' }),
      sessionFixture({ id: 'b', state: { kind: 'yourTurn' }, marker: { kind: 'YOUR_TURN', strength: 9 }, ticket: 'PROJ-1' }),
    ]);
    expect(markerOfTicket(view, 'PROJ-1')).toBe(LampColour.Turn);
  });
});
