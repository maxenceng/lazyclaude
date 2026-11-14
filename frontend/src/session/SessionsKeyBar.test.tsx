import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DockKeyBar } from './SessionsKeyBar';

describe('the dock\'s key bar', () => {
  it('has no link entry while the pointer is on no link', () => {
    render(<DockKeyBar mouseReported={false} hoveredLink={undefined} />);
    expect(screen.getByRole('contentinfo')).not.toHaveTextContent('ctrl+click');
  });

  it('leads with ctrl+click open and the address while the pointer is on a link', () => {
    render(<DockKeyBar mouseReported={false} hoveredLink="https://example.com" />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('ctrl+click open https://example.comctrl+shift+c copyctrl+shift+v paste');
  });

  it('clips a long address with an ellipsis so the other keys stay', () => {
    render(<DockKeyBar mouseReported hoveredLink="https://example.com/a/very/long/path/that/goes/on?q=1" />);
    const bar = screen.getByRole('contentinfo');
    expect(bar).toHaveTextContent('ctrl+click open https://example.com…ctrl+shift+c copy');
    expect(bar).not.toHaveTextContent('goes/on');
  });
});
