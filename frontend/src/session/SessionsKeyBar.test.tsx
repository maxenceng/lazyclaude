import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DockKeyBar, SessionsKeyBar } from './SessionsKeyBar';

describe('the dock\'s key bar', () => {
  it('has no link entry while the pointer is on no link', () => {
    render(<DockKeyBar mouseReported={false} hoveredLink={undefined} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('ctrl+click');
  });

  it('leads with ctrl+click open and the address while the pointer is on a link', () => {
    render(<DockKeyBar mouseReported={false} hoveredLink="https://example.com" />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('ctrl+click open https://example.comctrl+shift+c copyctrl+shift+v paste');
  });

  it('shows a long address whole, leaving its clipping to the bar so the other keys stay', () => {
    render(<DockKeyBar mouseReported hoveredLink="https://example.com/a/very/long/path/that/goes/on?q=1" />);
    const bar = screen.getByRole('contentinfo');
    expect(bar).toHaveTextContent('ctrl+click open https://example.com/a/very/long/path/that/goes/on?q=1ctrl+shift+c copy');
    expect(bar.firstElementChild).toHaveTextContent('ctrl+click open');
  });
});

describe('the ticket\'s sessions key bar', () => {
  it('lists the key that removes the worktree after the new session key, only when it is offered', () => {
    const { rerender } = render(<SessionsKeyBar picking={false} worktree="d remove worktree" />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('n newd remove worktree');
    rerender(<SessionsKeyBar picking={false} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('remove worktree');
  });

  it('offers it over no picker only', () => {
    render(<SessionsKeyBar picking worktree="d remove worktree" />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('remove worktree');
  });
});
