import type { LampColour } from '../design-system';
import type { LiveSessionView, SessionsView } from '../protocol';

export type { LampColour };

/** A live session's marker, as the lamp that shows it; `off` where the session carries none. */
export interface Marker {
  colour: LampColour;
  ticket: string | undefined;
  session: string;
}

// deferred: the marker's mapping and strength are written here and in SessionState — close it when LiveSessionView carries its marker on the wire.
const STRENGTH: readonly LampColour[] = ['permission', 'input', 'turn', 'off'];

const byStrength = (a: { colour: LampColour }, b: { colour: LampColour }) => STRENGTH.indexOf(a.colour) - STRENGTH.indexOf(b.colour);

function colourOf(session: LiveSessionView): LampColour {
  switch (session.state.kind) {
    case 'waitingForPermission':
      return 'permission';
    case 'waitingForInput':
      return 'input';
    case 'yourTurn':
      return 'turn';
    case 'busy':
    case 'unknown':
      return 'off';
  }
}

/** One marker per live session, the strongest first, numbered by the first ticket it is linked to. */
export function markersOf(view: SessionsView): Marker[] {
  return view.live.map((session) => ({ colour: colourOf(session), ticket: session.links[0]?.ticket, session: session.id })).sort(byStrength);
}

/** A ticket's marker: the strongest among the live sessions linked to it, whichever of their links it is; none when no session waits on you. */
export function markerOfTicket(view: SessionsView, ticket: string): LampColour | undefined {
  return view.live
    .filter((session) => session.links.some((link) => link.ticket === ticket))
    .map((session) => ({ colour: colourOf(session) }))
    .filter((marker) => marker.colour !== 'off')
    .sort(byStrength)[0]?.colour;
}
