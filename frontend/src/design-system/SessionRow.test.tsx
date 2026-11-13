import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LampColour } from './Lamp';
import { SessionRow } from './SessionRow';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('SessionRow.module.css');

describe('a session row', () => {
  it('is tinted while it is the user\'s turn, from the tile token', () => {
    render(<SessionRow label="start" selected={false} lamp={LampColour.Turn} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', 'turn');
    expect(ruleOf(sheet, ".row[data-lamp='turn']")).toContain('--row-ground: var(--lamp-turn-tile)');
  });

  it('lightens its action on a your-turn row, where the plain dim ink would not read', () => {
    expect(ruleOf(sheet, ".row[data-lamp='turn']")).toContain('--row-dim: var(--lamp-turn-tile-dim)');
    expect(ruleOf(sheet, '.action')).toContain('color: var(--row-dim, var(--dim))');
  });

  it('carries no tint for a session that waits on nothing', () => {
    render(<SessionRow label="start" selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', 'off');
    expect(screen.getByRole('button')).not.toHaveAttribute('data-lamp', 'turn');
  });

  it('spans the whole width of its list, with no cap on it', () => {
    expect(ruleOf(sheet, '.row')).toContain('width: 100%');
    expect(ruleOf(sheet, '.row')).not.toContain('max-width');
  });

  it('says what a press does and the key that does it, inside the one button', () => {
    render(<SessionRow label="start" action={{ label: 'attach', shortcut: 'enter' }} selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByRole('button')).toHaveTextContent('attach');
    expect(screen.getByRole('button')).toHaveTextContent('enter');
    expect(screen.getAllByRole('button')).toHaveLength(1);
  });

  it('carries no action where the row has none', () => {
    render(<SessionRow label="start" selected={false} lamp={undefined} onSelect={() => {}} />);
    expect(screen.getByRole('button')).not.toHaveTextContent('enter');
  });

  it('says its state in a worded pill on the left, then its name and its action, in the one button', () => {
    render(<SessionRow label="start 145" state="your turn" action={{ label: 'attach', shortcut: 'enter' }} selected={false} lamp={LampColour.Turn} onSelect={() => {}} />);
    const row = screen.getByRole('button');
    expect(screen.getByRole('img')).toHaveTextContent('your turn');
    expect(row.textContent).toMatch(/your turn.*start 145.*attach.*enter/);
  });

  it('words a session that waits on nothing by the state it is in, not by the lamp', () => {
    render(<SessionRow label="start 145" state="ended" selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('img')).toHaveTextContent('ended');
  });

  it.each(['permission', 'input'])('marks a %s row with the frame and tint of its state', (state) => {
    expect(ruleOf(sheet, `.row[data-lamp='${state}']`)).toContain(`--row-ground: var(--lamp-${state}-tile)`);
    expect(ruleOf(sheet, `.row[data-lamp='${state}']`)).toContain(`--row-frame: var(--lamp-${state})`);
  });

  it('lines its state, name and action up in columns, the state column the same width on every row', () => {
    expect(ruleOf(sheet, '.tall')).toContain('grid-template-columns: var(--session-state-column) minmax(0, 1fr) auto');
  });

  it('sits on the card ground in the dim ink while it waits on nothing, and in the plain ink while it waits on you', () => {
    expect(ruleOf(sheet, '.tall')).toContain('background: var(--row-ground, var(--card))');
    expect(ruleOf(sheet, '.tall')).toContain('color: var(--tall-ink, var(--dim))');
    expect(ruleOf(sheet, ".tall[data-lamp='permission']")).toContain('--tall-ink: var(--text)');
  });

  it('stands apart from the next card by a gap', () => {
    expect(ruleOf(sheet, '.tall')).toContain('margin: 0 0 var(--space-2)');
  });

  it('names its session in the mono face', () => {
    expect(ruleOf(sheet, '.tall .label')).toContain('var(--font-mono)');
  });

  it('keeps its own ground when selected and outlines it, rather than filling it with the cursor', () => {
    expect(ruleOf(sheet, '.tall.selected')).toContain('background: var(--row-ground, var(--card))');
    expect(ruleOf(sheet, '.tall.selected')).toContain('outline: var(--cursor-outline, var(--focus-edge)) solid var(--cursor-border)');
    expect(ruleOf(sheet, '.tall.selected')).toContain('box-shadow: none');
  });

  it('is tall when it carries a state, and keeps the plain row for a verb', () => {
    expect(ruleOf(sheet, '.tall')).toContain('padding');
    render(<SessionRow label="start" state="busy" selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('button').className).toContain('tall');
  });
});
