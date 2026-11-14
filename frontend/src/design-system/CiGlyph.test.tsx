import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Ci, CiGlyph } from './CiGlyph';

describe('a CI glyph', () => {
  it('names the result for a screen reader by its glyph', () => {
    render(<CiGlyph ci={Ci.Failed} />);
    expect(screen.getByRole('img', { name: 'checks failed' })).toHaveTextContent('✗');
  });

  it('is blank where there is no result and no word', () => {
    const { container } = render(<CiGlyph ci={undefined} />);
    expect(container).toHaveTextContent('');
  });

  it('is followed by its word when given one, so the colour is never the only sign', () => {
    render(<CiGlyph ci={Ci.Pending} word="running" />);
    expect(screen.getByText('running')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'checks pending' })).toHaveTextContent('◌');
  });

  it('shows a dash and the word for no result when given a word', () => {
    const { container } = render(<CiGlyph ci={undefined} word="no checks" />);
    expect(container).toHaveTextContent('–no checks');
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
