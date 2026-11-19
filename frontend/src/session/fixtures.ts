import type { SessionHostedSessionView, SessionMarkerView, SessionLiveSessionView, SessionPastSessionView, SessionSessionsView, SessionTicketSessionsView, SessionUnlinkedSessionsView } from '../protocol';

/** What the sidecar sends for a state: the test data's own copy of the mapping, which production code never holds. */
export const markerFixtureOf = (state: SessionLiveSessionView['state']): SessionMarkerView | null => {
  switch (state.kind) {
    case 'waitingForPermission':
      return { kind: 'WAITING_FOR_PERMISSION', strength: 3 };
    case 'waitingForInput':
      return { kind: 'WAITING_FOR_INPUT', strength: 2 };
    case 'yourTurn':
      return { kind: 'YOUR_TURN', strength: 1 };
    case 'busy':
    case 'unknown':
      return null;
  }
};

export const sessionFixture = (overrides: { id?: string; name?: string; state?: SessionLiveSessionView['state']; marker?: SessionMarkerView | null; ticket?: string | undefined; tickets?: string[]; directory?: string } = {}): SessionLiveSessionView => {
  const tickets = overrides.tickets ?? (overrides.ticket === undefined ? [] : [overrides.ticket]);
  const state = overrides.state ?? { kind: 'busy' };
  return {
    id: overrides.id ?? 's',
    pid: 1,
    name: overrides.name ?? 'session',
    directory: overrides.directory ?? '/p',
    startedAt: '2026-01-01T00:00:00Z',
    version: { kind: 'noVersion' },
    state,
    marker: overrides.marker === undefined ? markerFixtureOf(state) : overrides.marker,
    links: tickets.map((ticket) => ({ ticket, verb: 'start' })),
  };
};

export const sessionsFixture = (
  live: SessionLiveSessionView[],
  more: { past?: SessionPastSessionView[]; hosted?: SessionHostedSessionView[]; tickets?: SessionTicketSessionsView[]; unlinked?: Partial<SessionUnlinkedSessionsView> } = {},
): SessionSessionsView => {
  return {
    live,
    past: more.past ?? [],
    hosted: more.hosted ?? [],
    tickets: more.tickets ?? [],
    unlinked: { live: more.unlinked?.live ?? [], past: more.unlinked?.past ?? [] },
  };
};

export const ticketSessionsFixture = (
  ticket: string,
  sessions: { live?: SessionLiveSessionView[]; past?: SessionPastSessionView[]; hosted?: SessionHostedSessionView[] } = {},
): SessionTicketSessionsView => {
  return { ticket, live: sessions.live ?? [], past: sessions.past ?? [], hosted: sessions.hosted ?? [] };
};

export const pastFixture = (overrides: { id?: string; title?: string; ticket?: string; interrupted?: boolean; directory?: string; lastActivity?: string } = {}): SessionPastSessionView => {
  return {
    id: overrides.id ?? 'old',
    title: overrides.title === undefined ? { kind: 'noTitle' } : { kind: 'aiTitle', value: overrides.title },
    opening: { kind: 'noFirstPrompt' },
    reading: { kind: 'readableTranscript' },
    directory: overrides.directory ?? '/p/wt',
    lastActivity: overrides.lastActivity ?? '2026-01-01T00:00:00Z',
    startedAt: '2026-01-01T00:00:00Z',
    links: overrides.ticket === undefined ? [] : [{ ticket: overrides.ticket, verb: 'start' }],
    interrupted: overrides.interrupted ?? false,
  };
};

export const hostedFixture = (id: string, overrides: { ticket?: string; live?: SessionLiveSessionView } = {}): SessionHostedSessionView => {
  return { id, pid: 1, live: overrides.live ?? null, ticket: overrides.ticket ?? null };
};
