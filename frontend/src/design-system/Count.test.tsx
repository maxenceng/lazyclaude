import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Count } from './Count';
import { LampColour } from './Lamp';
import { ruleOf, stylesheet } from './stylesheet';

const count = stylesheet('Count.module.css');

describe('a lamp count', () => {
  it.each([
    [LampColour.Permission, 0, '0 permission'],
    [LampColour.Input, 1, '1 needs input'],
    [LampColour.Turn, 1, '1 your turn'],
  ])('words %s as what it counts', (colour, count, words) => {
    const { container } = render(<Count colour={colour} count={count} />);
    expect(container).toHaveTextContent(words);
    expect(screen.getByText(String(count)).tagName).toBe('B');
  });
});

describe('a clickable lamp count', () => {
  it('is a button that calls its click', () => {
    const onClick = vi.fn();
    render(<Count colour={LampColour.Turn} count={2} onClick={onClick} />);
    fireEvent.click(screen.getByRole('button', { name: /your turn/ }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('keeps the window focus on a press', () => {
    render(<Count colour={LampColour.Turn} count={2} onClick={vi.fn()} />);
    expect(fireEvent.mouseDown(screen.getByRole('button'))).toBe(false);
  });

  it('is a disabled button that does nothing when it has nowhere to go', () => {
    const onClick = vi.fn();
    render(<Count colour={LampColour.Turn} count={2} onClick={onClick} disabled />);
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByRole('button')).toBeDisabled();
    expect(onClick).not.toHaveBeenCalled();
  });

  it('dims its number and keeps its colour while disabled', () => {
    render(<Count colour={LampColour.Turn} count={0} onClick={vi.fn()} disabled />);
    expect(screen.getByRole('button')).toHaveAttribute('data-lamp', 'turn');
    expect(ruleOf(count, '.button:disabled b')).toContain('color: var(--dim)');
  });

  it('shows the blue focus edge, as a Button does', () => {
    expect(ruleOf(count, '.button:focus-visible')).toContain('outline: var(--focus-edge) solid var(--focus)');
  });

  it('looks like a dock tab: default cursor, no hover change', () => {
    expect(ruleOf(count, '.button')).toContain('cursor: default');
    expect(count).not.toContain(':hover');
  });
});
