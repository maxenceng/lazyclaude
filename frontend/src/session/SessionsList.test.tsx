import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LampColour } from './markers';
import { pastFixture } from './fixtures';
import { HostedRow, LiveRow, PastRow, type SessionRow } from './sessionRows';
import { SessionsList } from './SessionsList';

const list = (rows: SessionRow[]) =>
  render(<SessionsList rows={rows} cursor={0} picker={undefined} onPress={() => {}} />);

describe('a ticket\'s sessions', () => {
  it('carries on each hosted row, one button, the action attach and its key', () => {
    list([new HostedRow('h1', 'start 145', LampColour.Turn)]);
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
    list([new LiveRow('elsewhere', LampColour.Input)]);
    expect(screen.getByRole('button', { name: /elsewhere/ })).not.toHaveTextContent('enter');
  });
});
