import type { SessionPastSessionView, SessionSessionsView } from '../protocol';
import { colourOf, LampColour } from './markers';

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
    readonly age: string,
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

const linkedTo = (links: readonly { ticket: string }[], ticket: string | undefined): boolean => ticket !== undefined && links.some((link) => link.ticket === ticket);

/** The rows under `ticket`, or only the unlinked ones where no ticket is selected. */
export const rowsFor = (view: SessionSessionsView | undefined, ticket: string | undefined): SessionRow[] => {
  const hosted = (view?.hosted ?? []).filter((session) => ticket !== undefined && session.ticket === ticket);
  const hostedIds = new Set((view?.hosted ?? []).map((session) => session.live?.id ?? session.id));
  return [
    ...hosted.map((session) => new HostedRow(session.id, session.live?.name, session.live === null ? LampColour.Off : colourOf(session.live))),
    ...(view?.live ?? [])
      .filter((session) => linkedTo(session.links, ticket) && !hostedIds.has(session.id))
      .map((session) => new LiveRow(session.name, colourOf(session), session.age)),
    ...(view?.past ?? []).filter((session) => linkedTo(session.links, ticket)).map((session) => new PastRow(session)),
    ...(ticket === undefined ? [] : [new NewRow()]),
    // deferred: only the five newest sessions linked to no ticket are listed — close it when the desktop gets the TUI's expandable older row.
    ...(view?.past ?? [])
      .filter((session) => session.links.length === 0)
      .slice(0, UNLINKED_SHOWN)
      .map((session) => new UnlinkedRow(session)),
  ];
};
