import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Button, ButtonTone } from './Button';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('Button.module.css');

describe('a button', () => {
  it('calls its owner on a click', async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Retry</Button>);
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onClick).toHaveBeenCalled();
  });

  it('shows the key that does the same in a chip after its label', () => {
    render(
      <Button onClick={() => {}} tone={ButtonTone.Affirm} chip="S">
        Submit review
      </Button>,
    );
    expect(screen.getByRole('button', { name: 'Submit review' })).toHaveAttribute('aria-keyshortcuts', 'S');
    expect(screen.getByText('S').tagName).toBe('KBD');
  });

  it('reads its label and its chip from the tested affirm tokens', () => {
    expect(ruleOf(sheet, '.affirm')).toContain('background: var(--affirm-ground)');
    expect(ruleOf(sheet, '.affirm')).toContain('color: var(--affirm-ink)');
    expect(ruleOf(sheet, '.chip')).toContain('background: var(--affirm-chip-ground)');
    expect(ruleOf(sheet, '.chip')).toContain('color: var(--affirm-chip-ink)');
  });

  it('is quiet when it is a way out: a seam border on no fill, its chip on the card ground', () => {
    render(
      <Button onClick={() => {}} tone={ButtonTone.Quiet} chip="esc">
        Cancel
      </Button>,
    );
    expect(screen.getByText('esc').tagName).toBe('KBD');
    expect(ruleOf(sheet, '.quiet')).toContain('background: transparent');
    expect(ruleOf(sheet, '.quiet')).toContain('border: 1px solid var(--seam)');
    expect(ruleOf(sheet, '.quiet .chip')).toContain('background: var(--card)');
  });

  it('keeps the focus where it was when it is pressed, so a popup keeps its input', () => {
    render(<Button onClick={() => {}}>Cancel</Button>);
    expect(fireEvent.mouseDown(screen.getByRole('button', { name: 'Cancel' }))).toBe(false);
  });
});
