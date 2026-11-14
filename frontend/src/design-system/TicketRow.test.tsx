import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { LampColour } from './Lamp';
import { TicketRow } from './TicketRow';

describe('a ticket row', () => {
  it('scrolls into its pane when selected, so a done row below the fold stays in sight', () => {
    const scrolled = vi.spyOn(Element.prototype, 'scrollIntoView');
    const { rerender } = render(<TicketRow id="PROJ-1" title="Shipped" selected={false} done lamp={undefined} onSelect={() => {}} />);
    expect(scrolled).not.toHaveBeenCalled();
    rerender(<TicketRow id="PROJ-1" title="Shipped" selected done lamp={undefined} onSelect={() => {}} />);
    expect(scrolled.mock.contexts).toEqual([screen.getByRole('button')]);
    scrolled.mockRestore();
  });
});

describe('a ticket row and the session waiting on it', () => {
  it.each([
    [LampColour.Permission, 'permission'],
    [LampColour.Input, 'needs input'],
    [LampColour.Turn, 'your turn'],
  ])('carries a worded pill for a %s session', (lamp, words) => {
    render(<TicketRow id="PROJ-1" title="A ticket" selected={false} done={false} lamp={lamp} onSelect={() => {}} />);
    expect(screen.getByRole('img')).toHaveTextContent(words);
  });

  it('carries no pill when no session waits', () => {
    render(<TicketRow id="PROJ-1" title="A ticket" selected={false} done={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
