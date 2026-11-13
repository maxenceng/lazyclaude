import { render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { pastFixture } from './fixtures';
import { HostedRow, LiveRow, PastRow, type SessionRow, STARTING } from './sessionRows';
import { SessionsList } from './SessionsList';

const list = (rows: SessionRow[]) =>
  render(<SessionsList rows={rows} cursor={0} picker={undefined} onPress={() => {}} />);

describe('a ticket\'s sessions', () => {
  it('carries on each hosted row, one button, the action attach and its key', () => {
    list([new HostedRow('h1', 'start 145', 'yourTurn')]);
    const row = screen.getByRole('button', { name: /start 145/ });
    expect(row).toHaveTextContent('attach');
    expect(row).toHaveTextContent('enter');
  });

  it('carries resume and its key on a past row', () => {
    list([new PastRow(pastFixture({ id: 'p1', title: 'refine 145' }))]);
    expect(screen.getByRole('button', { name: /refine 145/ })).toHaveTextContent('resume');
    expect(screen.getByRole('button', { name: /refine 145/ })).toHaveTextContent('enter');
  });

  it('carries no action on a live session another window hosts, which cannot be entered', () => {
    list([new LiveRow('elsewhere', 'waitingForInput')]);
    expect(screen.getByRole('button', { name: /elsewhere/ })).not.toHaveTextContent('enter');
  });

  it.each([
    ['waitingForPermission', 'permission'],
    ['waitingForInput', 'needs input'],
    ['yourTurn', 'your turn'],
    ['busy', 'busy'],
    ['unknown', 'unknown'],
    [STARTING, 'starting'],
  ] as const)('says a %s hosted session in the word %s on its pill', (state, word) => {
    list([new HostedRow('h1', 'start 145', state)]);
    expect(within(screen.getByRole('button')).getByRole('img')).toHaveTextContent(word);
  });

  it('says a past session has ended', () => {
    list([new PastRow(pastFixture({ id: 'p1', title: 'refine 145' }))]);
    expect(within(screen.getByRole('button')).getByRole('img')).toHaveTextContent('ended');
  });

  it('marks a waiting session\'s row with its state, and a busy one\'s with none a tint answers to', () => {
    list([new HostedRow('h1', 'a', 'yourTurn'), new HostedRow('h2', 'b', 'busy')]);
    expect(screen.getAllByRole('button').map((row) => row.getAttribute('data-lamp'))).toEqual(['turn', 'off']);
  });

  describe('and how long ago they ran', () => {
    beforeEach(() => vi.useFakeTimers({ now: Date.parse('2026-03-29T12:00:00Z') }));
    afterEach(() => vi.useRealTimers());

    it('shows the age of a hosted session since it started, and of a past one since it last ran', () => {
      list([new HostedRow('h1', 'a', 'yourTurn', '2026-03-29T11:48:00Z'), new PastRow(pastFixture({ id: 'p1', title: 'old', lastActivity: '2026-03-27T12:00:00Z' }))]);
      const [hosted, past] = screen.getAllByRole('button');
      expect(hosted).toHaveTextContent('12m');
      expect(past).toHaveTextContent('2d');
    });

    it('shows none for a session whose start is not read', () => {
      list([new HostedRow('h1', 'a', 'yourTurn')]);
      expect(screen.getByRole('button').textContent).not.toMatch(/\d+[mhd]|now/);
    });
  });
});
