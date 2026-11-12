import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LampColour } from './Lamp';
import { SessionRow } from './SessionRow';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('SessionRow.module.css');

describe('a session row', () => {
  it('is tinted while it is the user\'s turn, from the tile token', () => {
    render(<SessionRow label="start" detail="hosted" selected={false} lamp={LampColour.Turn} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', 'turn');
    expect(ruleOf(sheet, ".row[data-lamp='turn']")).toContain('--row-ground: var(--lamp-turn-tile)');
  });

  it('lightens its detail on a your-turn row, where the plain dim ink would not read', () => {
    expect(ruleOf(sheet, ".row[data-lamp='turn']")).toContain('--row-dim: var(--lamp-turn-tile-dim)');
    expect(ruleOf(sheet, '.detail')).toContain('color: var(--row-dim, var(--dim))');
  });

  it('carries no tint for a session that waits on nothing', () => {
    render(<SessionRow label="start" detail="hosted" selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', 'off');
    expect(screen.getByRole('button')).not.toHaveAttribute('data-lamp', 'turn');
  });

  it('spans the whole width of its list, with no cap on it', () => {
    expect(ruleOf(sheet, '.row')).toContain('width: 100%');
    expect(ruleOf(sheet, '.row')).not.toContain('max-width');
  });
});
