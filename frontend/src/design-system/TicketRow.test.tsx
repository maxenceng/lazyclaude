import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
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
