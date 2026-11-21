import { LampColour } from '../design-system';
import type { SessionLiveSessionView, SessionMarkerView, SessionSessionsView } from '../protocol';

export { LampColour };

/** A live session's lamp tile: the lamp its marker lights, `off` where it carries none, and that marker's strength. */
export interface LampTile {
  colour: LampColour;
  strength: number;
  ticket: string | undefined;
  session: string;
}

/** What a session with no marker weighs: less than any strength the sidecar sends. */
const UNMARKED = Number.NEGATIVE_INFINITY;

const LAMPS: Record<SessionMarkerView['kind'], LampColour> = {
  WAITING_FOR_PERMISSION: LampColour.Permission,
  WAITING_FOR_INPUT: LampColour.Input,
  YOUR_TURN: LampColour.Turn,
};

/** The lamp a marker lights; the unlit one for none. */
export const colourOfMarker = (marker: SessionMarkerView | null): LampColour => (marker === null ? LampColour.Off : LAMPS[marker.kind]);

/** How much a marker asks of you, as the sidecar weighs it; less than any marker's for none. */
export const strengthOfMarker = (marker: SessionMarkerView | null): number => marker?.strength ?? UNMARKED;

/** Orders by the strength the wire carries, the strongest first; equals keep the order they came in. */
export const byStrength = (a: { strength: number }, b: { strength: number }) => Number(b.strength > a.strength) - Number(b.strength < a.strength);

export const colourOf = (session: SessionLiveSessionView): LampColour => colourOfMarker(session.marker);

/** One lamp tile per live session, the strongest marker first, numbered by the first ticket it is linked to. */
export const lampTilesOf = (view: SessionSessionsView): LampTile[] => {
  return view.live
    .map((session) => ({ colour: colourOf(session), strength: strengthOfMarker(session.marker), ticket: session.links[0]?.ticket, session: session.id }))
    .sort(byStrength);
};

/** A ticket's marker: the strongest among the live sessions linked to it, whichever of their links it is; none when no session waits on you. */
export const markerOfTicket = (view: SessionSessionsView, ticket: string): LampColour | undefined => {
  const strongest = view.live
    .filter((session) => session.marker !== null && session.links.some((link) => link.ticket === ticket))
    .map((session) => ({ marker: session.marker, strength: strengthOfMarker(session.marker) }))
    .sort(byStrength)[0];
  return strongest === undefined ? undefined : colourOfMarker(strongest.marker);
};
