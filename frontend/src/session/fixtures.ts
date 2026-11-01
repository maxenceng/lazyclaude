import type { LiveSessionView, SessionsView } from '../protocol';

export function sessionFixture(overrides: { id?: string; state?: LiveSessionView['state']; ticket?: string | undefined } = {}): LiveSessionView {
  const ticket = overrides.ticket;
  return {
    id: overrides.id ?? 's',
    pid: 1,
    name: 'session',
    directory: '/p',
    startedAt: '2026-01-01T00:00:00Z',
    age: '1m',
    version: { kind: 'noVersion' },
    state: overrides.state ?? { kind: 'busy' },
    links: ticket === undefined ? [] : [{ ticket, verb: 'start' }],
  };
}

export function sessionsFixture(live: LiveSessionView[]): SessionsView {
  return { live, past: [], hosted: [] };
}
