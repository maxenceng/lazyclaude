import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge, BadgeTone } from './Badge';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('Badge.module.css');

describe('a badge', () => {
  it('says its word', () => {
    render(<Badge>1 pending</Badge>);
    expect(screen.getByText('1 pending')).toHaveAttribute('data-tone', 'plain');
  });

  it('fills amber with the dark ink the token pair is tested for', () => {
    render(<Badge tone={BadgeTone.Amber}>pending</Badge>);
    expect(screen.getByText('pending')).toHaveAttribute('data-tone', 'amber');
    expect(ruleOf(sheet, '.amber')).toContain('color: var(--badge-pending-ink)');
    expect(ruleOf(sheet, '.amber')).toContain('background: var(--badge-pending-ground)');
  });

  it('sets its word at 12px or more', () => {
    expect(ruleOf(sheet, '.badge')).toContain('var(--size-small)');
  });
});
