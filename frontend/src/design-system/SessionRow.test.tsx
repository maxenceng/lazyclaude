import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LampColour } from './Lamp';
import { PillInk, SessionRow } from './SessionRow';
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

  it('does not pick itself or take the cursor when the pointer only rests on it', async () => {
    const onSelect = vi.fn();
    render(<SessionRow label="refine" selected={false} lamp={undefined} onSelect={onSelect} />);
    await userEvent.hover(screen.getByRole('button'));
    expect(onSelect, 'hover is not a cursor move: enter still starts the verb the cursor is on').not.toHaveBeenCalled();
    expect(screen.getByRole('button')).not.toHaveAttribute('aria-current');
  });

  it('tells the pointer moving onto it to a row that follows the pointer, once per entry', async () => {
    const onPoint = vi.fn();
    render(<SessionRow label="refine" selected={false} lamp={undefined} onSelect={() => {}} onPoint={onPoint} />);
    const user = userEvent.setup();
    await user.hover(screen.getByRole('button'));
    await user.pointer({ target: screen.getByRole('button'), coords: { x: 2, y: 2 } });
    expect(onPoint, 'a move within the row is not a new entry: a keyboard move away from a still pointer stands').toHaveBeenCalledOnce();
  });

  it('draws no hover ground on a row that follows the pointer, so the cursor ground is its one highlight', () => {
    render(<SessionRow label="refine" selected={false} lamp={undefined} onSelect={() => {}} onPoint={() => {}} />);
    expect(screen.getByRole('button').className, 'the pointed class is what the hover-ground rule leaves out').toContain('pointed');
    expect(ruleOf(sheet, '.row:not(.tall):not(.pointed):hover:not(.selected)')).toContain('--hover-ground');
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

  it('answers the pointer: a hand over it, and its action lit', () => {
    expect(ruleOf(sheet, 'button.tall:hover')).toContain('cursor: pointer');
    expect(ruleOf(sheet, 'button.tall:hover .action')).toContain('color: var(--text)');
  });

  it('keeps the prototype\'s card heights: its name on a set leading, a waiting card a little roomier', () => {
    expect(ruleOf(sheet, '.tall .label')).toContain('/ var(--session-name-leading)');
    expect(ruleOf(sheet, ".tall[data-lamp='permission']")).toContain('padding: var(--session-waiting-pad) var(--space-4)');
  });

  it('names its session in the mono face', () => {
    expect(ruleOf(sheet, '.tall .label')).toContain('var(--font-mono)');
  });

  it('keeps its own ground when selected, with no cursor fill, edge or outline', () => {
    expect(ruleOf(sheet, '.tall.selected')).toContain('background: var(--row-ground, var(--card))');
    expect(ruleOf(sheet, '.tall.selected')).toContain('box-shadow: none');
    expect(ruleOf(sheet, '.tall.selected')).not.toContain('outline');
  });

  it('is tall when it carries a state, and keeps the plain row for a verb', () => {
    expect(ruleOf(sheet, '.tall')).toContain('padding');
    render(<SessionRow label="start" state="busy" selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('button').className).toContain('tall');
  });

  it('draws a clean pill on its own neutral tokens, not the lamp\'s, and says its words', () => {
    render(<SessionRow label="../app.worktrees/x" state="clean" pill={PillInk.Clean} selected={false} lamp={LampColour.Off} onSelect={() => {}} />);
    expect(screen.getByRole('img')).toHaveTextContent('clean');
    expect(screen.getByRole('img').className).toContain('cleanPill');
    expect(ruleOf(sheet, '.cleanPill')).toContain('background: var(--worktree-clean)');
    expect(ruleOf(sheet, '.cleanPill')).toContain('color: var(--worktree-clean-ink)');
  });

  it('is no button, and has no hover affordance, where it is given no handler', () => {
    render(<SessionRow label="../app.worktrees/x" state="held" selected={false} lamp={LampColour.Off} />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('../app.worktrees/x').closest('li')).toHaveTextContent('held');
    expect(sheet.split('\n'), 'the hover is on the button alone').not.toContain('.tall:hover {');
  });
});
