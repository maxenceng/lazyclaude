import type { HostedSessionView, LiveSessionView, PastSessionView, SessionsView } from '../protocol';

export const sessionFixture = (overrides: { id?: string; state?: LiveSessionView['state']; ticket?: string | undefined; tickets?: string[] } = {}): LiveSessionView => {
  const tickets = overrides.tickets ?? (overrides.ticket === undefined ? [] : [overrides.ticket]);
  return {
    id: overrides.id ?? 's',
    pid: 1,
    name: 'session',
    directory: '/p',
    startedAt: '2026-01-01T00:00:00Z',
    age: '1m',
    version: { kind: 'noVersion' },
    state: overrides.state ?? { kind: 'busy' },
    links: tickets.map((ticket) => ({ ticket, verb: 'start' })),
  };
};

export const sessionsFixture = (live: LiveSessionView[], more: { past?: PastSessionView[]; hosted?: HostedSessionView[] } = {}): SessionsView => {
  return { live, past: more.past ?? [], hosted: more.hosted ?? [] };
};

export const pastFixture = (overrides: { id?: string; title?: string; ticket?: string } = {}): PastSessionView => {
  return {
    id: overrides.id ?? 'old',
    title: overrides.title === undefined ? { kind: 'noTitle' } : { kind: 'aiTitle', value: overrides.title },
    opening: { kind: 'noFirstPrompt' },
    reading: { kind: 'readableTranscript' },
    directory: '/p/wt',
    lastActivity: '2026-01-01T00:00:00Z',
    age: '2d',
    startedAt: '2026-01-01T00:00:00Z',
    links: overrides.ticket === undefined ? [] : [{ ticket: overrides.ticket, verb: 'start' }],
    interrupted: false,
  };
};

export const hostedFixture = (id: string, overrides: { ticket?: string; live?: LiveSessionView } = {}): HostedSessionView => {
  return { id, pid: 1, live: overrides.live ?? null, busyOrWaiting: false, ticket: overrides.ticket ?? null };
};
