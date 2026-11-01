import type { LampColour } from '../design-system';
import type { LiveSessionView, SessionsView } from '../protocol';

export type { LampColour };


export interface Lamp {
  colour: LampColour;
  ticket: string | undefined;
  session: string;
}

const URGENCY: readonly LampColour[] = ['permission', 'input', 'turn', 'off'];

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

/** One lamp per live session, the one that needs you most first. */
export function lampsOf(view: SessionsView): Lamp[] {
  return view.live
    .map((session) => ({ colour: colourOf(session), ticket: session.links[0]?.ticket, session: session.id }))
    .sort((a, b) => URGENCY.indexOf(a.colour) - URGENCY.indexOf(b.colour));
}

/** The lamp beside a ticket: its most urgent lit session; nothing when none of them waits on you. */
export function lampOfTicket(view: SessionsView, ticket: string): LampColour | undefined {
  return lampsOf(view).find((lamp) => lamp.ticket === ticket && lamp.colour !== 'off')?.colour;
}
