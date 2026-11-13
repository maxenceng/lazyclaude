import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ReadingSection } from './ReadingSection';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('ReadingSection.module.css');

describe('a reading section', () => {
  it('names what it holds by its title', () => {
    render(
      <ReadingSection title="Sessions">
        <p>a row</p>
      </ReadingSection>,
    );
    expect(screen.getByRole('region', { name: 'Sessions' })).toHaveTextContent('a row');
    expect(screen.getByRole('heading', { name: 'Sessions' })).toBeInTheDocument();
  });

  it('sets its title as a small dim label, with no count and no rule under it', () => {
    expect(ruleOf(sheet, '.title')).toContain('font-size: var(--size-micro)');
    expect(ruleOf(sheet, '.title')).toContain('color: var(--dim)');
    expect(ruleOf(sheet, '.title')).not.toContain('border');
  });
});
