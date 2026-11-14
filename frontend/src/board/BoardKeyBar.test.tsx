import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BoardKeyBar, BoardTabKeyBar } from './BoardKeyBar';

describe('the Board\'s key bar', () => {
  it('lists the keys that move, open and start sessions while a ticket is under the cursor, with the capture and what is handed to it', () => {
    render(<BoardKeyBar ticket capture extra={['o docs', '1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('j/k move');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('enter open');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('c capture');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('o docs');
  });

  it('keeps only the keys that act when no ticket can be under the cursor: new ticket, the panel and the window tabs', () => {
    render(<BoardKeyBar ticket={false} capture extra={['o docs', '1/2/3 tabs']} />);
    const bar = screen.getByRole('contentinfo');
    expect(bar).toHaveTextContent('c capture');
    expect(bar).toHaveTextContent('o docs');
    expect(bar).toHaveTextContent('1/2/3 tabs');
    expect(bar).not.toHaveTextContent('j/k move');
    expect(bar).not.toHaveTextContent('enter open');
    expect(bar).not.toHaveTextContent('n/N new');
  });

  it('leaves the new ticket off where there is no project to capture into', () => {
    render(<BoardKeyBar ticket={false} capture={false} extra={['1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('capture');
  });
});

describe('the Board tab\'s key bar', () => {
  it('lists moving over the columns, going to the ticket and the new ticket while a ticket is under the cursor', () => {
    render(<BoardTabKeyBar ticket capture extra={['1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('h/j/k/l moveenter go to ticketc capture1/2/3 tabs');
  });

  it('keeps only the new ticket and the window tabs when no ticket can be under the cursor', () => {
    render(<BoardTabKeyBar ticket={false} capture extra={['1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('c capture1/2/3 tabs');
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('move');
  });

  it('leaves the new ticket off where there is no project to capture into', () => {
    render(<BoardTabKeyBar ticket={false} capture={false} extra={['1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('capture');
  });
});
