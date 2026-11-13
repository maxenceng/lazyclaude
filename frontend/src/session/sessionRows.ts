import type { SessionPastSessionView, SessionSessionsView } from '../protocol';
import { byStrength, colourOfState, LampColour, type LiveState } from './markers';

export enum SessionRowKind {
  Hosted = 'hosted',
  Live = 'live',
  Past = 'past',
  New = 'new',
}

/** A hosted session with no record read yet: just spawned, or still on Claude Code's trust dialog. */
export const STARTING = 'starting';

/** What a running session is doing: what its record says, or starting while it has none. */
export type RunningState = LiveState | typeof STARTING;

/** A hosted session of the ticket: Enter focuses its tab. `name` is none while its record is not read. */
export class HostedRow {
  readonly kind = SessionRowKind.Hosted;
  constructor(
    readonly session: string,
    readonly name: string | undefined,
    readonly state: RunningState,
  ) {}

  get lamp(): LampColour {
    return this.state === STARTING ? LampColour.Off : colourOfState(this.state);
  }
}

/** A live session of the ticket that this window does not host; it can be seen and not entered. */
export class LiveRow {
  readonly kind = SessionRowKind.Live;
  constructor(
    readonly name: string,
    readonly state: LiveState,
  ) {}

  get lamp(): LampColour {
    return colourOfState(this.state);
  }
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

export type SessionRow = HostedRow | LiveRow | PastRow | NewRow;

/** The rows under `ticket` from the sessions `session` says it lists, the running ones carrying a marker first, the strongest first; none where no ticket is selected, since nothing shows them there. */
export const rowsFor = (view: SessionSessionsView | undefined, ticket: string | undefined): SessionRow[] => {
  if (ticket === undefined) {
    return [];
  }
  const own = view?.tickets.find((listed) => listed.ticket === ticket);
  const hostedIds = new Set((view?.hosted ?? []).map((session) => session.live?.id ?? session.id));
  const running = [
    ...(own?.hosted ?? []).map((session) => new HostedRow(session.id, session.live?.name, session.live === null ? STARTING : session.live.state.kind)),
    ...(own?.live ?? []).filter((session) => !hostedIds.has(session.id)).map((session) => new LiveRow(session.name, session.state.kind)),
  ];
  return [
    ...running.sort((a, b) => byStrength({ colour: a.lamp }, { colour: b.lamp })),
    ...(own?.past ?? []).map((session) => new PastRow(session)),
    new NewRow(),
  ];
};

/** What a row stands for, the same across reads while it is the same session: the cursor follows it when the rows reorder. */
export const rowKey = (row: SessionRow): string => {
  switch (row.kind) {
    case SessionRowKind.Hosted:
      return `${row.kind}:${row.session}`;
    case SessionRowKind.Live:
      return `${row.kind}:${row.name}`;
    case SessionRowKind.Past:
      return `${row.kind}:${row.session.id}`;
    case SessionRowKind.New:
      return row.kind;
  }
};
