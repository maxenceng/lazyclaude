import type { SessionPastSessionView, SessionSessionsView } from '../protocol';
import { byStrength, colourOf, LampColour } from './markers';

export enum SessionRowKind {
  Hosted = 'hosted',
  Live = 'live',
  Past = 'past',
  New = 'new',
  Unlinked = 'unlinked',
}

const UNLINKED_SHOWN = 5;

/** A hosted session of the ticket: Enter focuses its tab. `name` is none while its record is not read. */
export class HostedRow {
  readonly kind = SessionRowKind.Hosted;
  constructor(
    readonly session: string,
    readonly name: string | undefined,
    readonly lamp: LampColour,
  ) {}
}

/** A live session of the ticket that this window does not host; it can be seen and not entered. */
export class LiveRow {
  readonly kind = SessionRowKind.Live;
  constructor(
    readonly name: string,
    readonly lamp: LampColour,
  ) {}
}

/** A past session linked to the ticket: Enter resumes it in the ticket's worktree. */
export class PastRow {
  readonly kind = SessionRowKind.Past;
  constructor(readonly session: SessionPastSessionView) {}
}

/** The row that starts a session for the ticket. */
export class NewRow {
  readonly kind = SessionRowKind.New;
}

/** A past session linked to no ticket: Enter resumes it in the directory it was recorded in. */
export class UnlinkedRow {
  readonly kind = SessionRowKind.Unlinked;
  constructor(readonly session: SessionPastSessionView) {}
}

export type SessionRow = HostedRow | LiveRow | PastRow | NewRow | UnlinkedRow;

/** The rows under `ticket` from the sessions `session` says it lists, the running ones waiting on you first and the strongest first, or the unlinked ones where no ticket is selected. */
export const rowsFor = (view: SessionSessionsView | undefined, ticket: string | undefined): SessionRow[] => {
  if (ticket === undefined) {
    // deferred: only the five newest sessions linked to no ticket are listed — close it when the desktop gets the TUI's expandable older row.
    return (view?.unlinked.past ?? []).slice(0, UNLINKED_SHOWN).map((session) => new UnlinkedRow(session));
  }
  const own = view?.tickets.find((listed) => listed.ticket === ticket);
  const hostedIds = new Set((view?.hosted ?? []).map((session) => session.live?.id ?? session.id));
  const running = [
    ...(own?.hosted ?? []).map((session) => new HostedRow(session.id, session.live?.name, session.live === null ? LampColour.Off : colourOf(session.live))),
    ...(own?.live ?? []).filter((session) => !hostedIds.has(session.id)).map((session) => new LiveRow(session.name, colourOf(session))),
  ];
  return [
    ...running.sort((a, b) => byStrength({ colour: a.lamp }, { colour: b.lamp })),
    ...(own?.past ?? []).map((session) => new PastRow(session)),
    new NewRow(),
  ];
};
