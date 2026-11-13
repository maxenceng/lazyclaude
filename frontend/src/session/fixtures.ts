import type { SessionHostedSessionView, SessionLiveSessionView, SessionPastSessionView, SessionSessionsView, SessionTicketSessionsView, SessionUnlinkedSessionsView } from '../protocol';

export const sessionFixture = (overrides: { id?: string; state?: SessionLiveSessionView['state']; ticket?: string | undefined; tickets?: string[]; directory?: string } = {}): SessionLiveSessionView => {
  const tickets = overrides.tickets ?? (overrides.ticket === undefined ? [] : [overrides.ticket]);
  return {
    id: overrides.id ?? 's',
    pid: 1,
    name: 'session',
    directory: overrides.directory ?? '/p',
    startedAt: '2026-01-01T00:00:00Z',
    version: { kind: 'noVersion' },
    state: overrides.state ?? { kind: 'busy' },
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

export const pastFixture = (overrides: { id?: string; title?: string; ticket?: string; interrupted?: boolean; directory?: string } = {}): SessionPastSessionView => {
  return {
    id: overrides.id ?? 'old',
    title: overrides.title === undefined ? { kind: 'noTitle' } : { kind: 'aiTitle', value: overrides.title },
    opening: { kind: 'noFirstPrompt' },
    reading: { kind: 'readableTranscript' },
    directory: overrides.directory ?? '/p/wt',
    lastActivity: '2026-01-01T00:00:00Z',
    startedAt: '2026-01-01T00:00:00Z',
    links: overrides.ticket === undefined ? [] : [{ ticket: overrides.ticket, verb: 'start' }],
    interrupted: overrides.interrupted ?? false,
  };
};

export const hostedFixture = (id: string, overrides: { ticket?: string; live?: SessionLiveSessionView } = {}): SessionHostedSessionView => {
  return { id, pid: 1, live: overrides.live ?? null, ticket: overrides.ticket ?? null };
};
