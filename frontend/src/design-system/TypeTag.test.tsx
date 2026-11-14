import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';
import { TypeColour, TypeTag } from './TypeTag';

const sheet = stylesheet('TypeTag.module.css');

describe('a type tag', () => {
  it.each([
    [TypeColour.Feature, 'type-feature'],
    [TypeColour.Fix, 'type-fix'],
    [TypeColour.Chore, 'type-chore'],
  ])('colours %s from its own token', (colour, token) => {
    render(<TypeTag name="anything" colour={colour} />);
    expect(screen.getByText('anything')).toHaveAttribute('data-type', colour);
    expect(ruleOf(sheet, `.tag[data-type='${colour}']`)).toContain(`color: var(--tag-ink, var(--${token}))`);
  });

  it('leaves a type given no colour dim', () => {
    render(<TypeTag name="docs" colour={undefined} />);
    expect(screen.getByText('docs')).toHaveAttribute('data-type', 'other');
    expect(ruleOf(sheet, '.tag')).toContain('color: var(--dim)');
  });
});
