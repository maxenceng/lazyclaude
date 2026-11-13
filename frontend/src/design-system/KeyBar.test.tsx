import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KeyBar } from './KeyBar';

describe('the key bar', () => {
  it('sets each key apart from what it does', () => {
    render(<KeyBar keys={['j/k move', 'enter open or fold']} />);
    expect(screen.getByText('j/k').tagName).toBe('KBD');
    expect(screen.getByText('open or fold')).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toHaveTextContent('j/k move');
  });
});
