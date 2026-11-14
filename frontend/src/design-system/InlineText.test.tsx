import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { InlineText } from './InlineText';

describe('a text with inline markdown', () => {
  it('sets code in code type without its backticks', () => {
    render(<InlineText source="pty4j leaves `/tmp/pty4j-*` behind" />);
    expect(screen.getByText('/tmp/pty4j-*').tagName).toBe('CODE');
    expect(screen.getByText(/pty4j leaves/)).toHaveTextContent('pty4j leaves /tmp/pty4j-* behind');
  });

  it('reads a link as its words, since nothing follows it', () => {
    render(<InlineText source="see [the ADR](docs/adr/0106.md)" />);
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getByText(/see/)).toHaveTextContent('see the ADR');
  });

  it('keeps text with no markdown as it was written', () => {
    render(<InlineText source="External links do not work." />);
    expect(screen.getByText('External links do not work.')).toBeInTheDocument();
  });
});
