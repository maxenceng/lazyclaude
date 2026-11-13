import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BoardKeyBar } from './BoardKeyBar';

describe('the Board\'s key bar', () => {
  it('lists the keys that move, open and start sessions while a ticket is under the cursor, with the new draft and what is handed to it', () => {
    render(<BoardKeyBar ticket capture extra={['o docs', '1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('j/k move');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('enter open');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('c new draft');
    expect(screen.getByRole('contentinfo')).toHaveTextContent('o docs');
  });

  it('keeps only the keys that act when no ticket can be under the cursor: new draft, the panel and the window tabs', () => {
    render(<BoardKeyBar ticket={false} capture extra={['o docs', '1/2/3 tabs']} />);
    const bar = screen.getByRole('contentinfo');
    expect(bar).toHaveTextContent('c new draft');
    expect(bar).toHaveTextContent('o docs');
    expect(bar).toHaveTextContent('1/2/3 tabs');
    expect(bar).not.toHaveTextContent('j/k move');
    expect(bar).not.toHaveTextContent('enter open');
    expect(bar).not.toHaveTextContent(/\bn new\b/);
  });

  it('leaves the new draft off where there is no project to capture into', () => {
    render(<BoardKeyBar ticket={false} capture={false} extra={['1/2/3 tabs']} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('new draft');
  });
});
