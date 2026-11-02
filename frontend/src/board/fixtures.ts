import type { BoardView, TicketView } from '../protocol';

export const ticketFixture = (overrides: Partial<TicketView> = {}): TicketView => {
  return {
    id: 'PROJ-1',
    branch: 'proj-1-a-ticket',
    title: 'A ticket',
    status: 'TODO',
    context: { kind: 'noContext' },
    type: { kind: 'noType' },
    otherLabels: [],
    body: { kind: 'noBody' },
    verbs: [],
    ...overrides,
  };
};

export const boardFixture = (tickets: TicketView[], unreadable: BoardView['unreadable'] = []): BoardView => {
  const statuses = [...new Set(tickets.map((ticket) => ticket.status))];
  return {
    kind: 'board',
    root: '/p',
    unreadable,
    groups: statuses.map((status) => ({ status, tickets: tickets.filter((ticket) => ticket.status === status) })),
  };
};
