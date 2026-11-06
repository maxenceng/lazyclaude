import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Ci } from './CiGlyph';
import { LampColour } from './Lamp';
import { TicketCard } from './TicketCard';

describe('a ticket card', () => {
  it('puts the whole title over a footer of its number, type and labels', () => {
    render(
      <TicketCard id="PROJ-1" title="A ticket" meta={['feature', 'help wanted']} selected={false} done={false} lamp={undefined} onSelect={() => {}} />,
    );
    expect(screen.getByRole('button')).toHaveTextContent('A ticketPROJ-1featurehelp wanted');
  });

  it('carries its lamp, its draft mark and its CI result in the footer', () => {
    render(
      <TicketCard
        id="PROJ-1"
        title="A ticket"
        meta={[]}
        selected={false}
        done={false}
        lamp={LampColour.Input}
        pullRequest={{ marked: true, ci: Ci.Failed }}
        onSelect={() => {}}
      />,
    );
    const card = screen.getByRole('button');
    expect(within(card).getByRole('img', { name: 'draft or without checks' })).toBeInTheDocument();
    expect(within(card).getByRole('img', { name: 'checks failed' })).toBeInTheDocument();
  });
});
