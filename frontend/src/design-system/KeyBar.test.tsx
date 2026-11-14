import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KeyBar } from './KeyBar';
import { ruleOf, stylesheet } from './stylesheet';

const keyBar = stylesheet('KeyBar.module.css');

describe('the key bar', () => {
  it('sets each key apart from what it does', () => {
    render(<KeyBar keys={['j/k move', 'enter open or fold']} />);
    expect(screen.getByText('j/k').tagName).toBe('KBD');
    expect(screen.getByText('open or fold')).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toHaveTextContent('j/k move');
  });

  it('puts a leading entry first, whole in the text', () => {
    render(<KeyBar lead="ctrl+click open https://example.com/a/long/path" keys={['ctrl+shift+c copy']} />);
    expect(screen.getByRole('contentinfo')).toHaveTextContent('ctrl+click open https://example.com/a/long/pathctrl+shift+c copy');
    expect(screen.getByText('ctrl+click').tagName).toBe('KBD');
  });

  it('lets only the leading entry give way, clipped with an ellipsis, so the other keys stay whole', () => {
    expect(ruleOf(keyBar, '.entry')).toContain('flex-shrink: 0');
    expect(ruleOf(keyBar, '.lead')).toContain('flex-shrink: 1');
    expect(ruleOf(keyBar, '.lead')).toContain('min-width: 0');
    expect(ruleOf(keyBar, '.lead')).toContain('overflow: hidden');
    expect(ruleOf(keyBar, '.lead')).toContain('text-overflow: ellipsis');
  });
});
