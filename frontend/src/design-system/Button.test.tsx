import { render, screen } from '@testing-library/react';
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
});
