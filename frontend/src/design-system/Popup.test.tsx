import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Popup } from './Popup';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('Popup.module.css');

const answers = () => ({ main: { label: 'Quit', chip: 'y', onClick: vi.fn() }, out: { label: 'Stay', chip: 'esc', onClick: vi.fn() } });

describe('a popup', () => {
  it('is a modal dialog named for its title, over a scrim the mouse is held to', () => {
    render(
      <Popup title="Quit lazyclaude?" {...answers()}>
        <p>Two sessions are open.</p>
      </Popup>,
    );
    const dialog = screen.getByRole('dialog', { name: 'Quit lazyclaude?' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog.parentElement).toHaveAttribute('data-modal');
    expect(within(dialog).getByText('Two sessions are open.')).toBeInTheDocument();
  });

  it('answers with buttons that carry their keys as chips, the main one last', () => {
    const { container } = render(<Popup title="Quit lazyclaude?" {...answers()} />);
    const foot = container.querySelector<HTMLElement>('[data-popup-foot]')!;
    const buttons = within(foot).getAllByRole('button');
    expect(buttons.map((button) => button.textContent)).toEqual(['Stayesc', 'Quity']);
    expect(buttons[1]).toHaveAttribute('aria-keyshortcuts', 'y');
  });

  it('calls the answer a button stands for', async () => {
    const given = answers();
    render(<Popup title="Quit lazyclaude?" {...given} />);
    await userEvent.click(screen.getByRole('button', { name: 'Quit' }));
    await userEvent.click(screen.getByRole('button', { name: 'Stay' }));
    expect(given.main.onClick).toHaveBeenCalledTimes(1);
    expect(given.out.onClick).toHaveBeenCalledTimes(1);
  });

  it('closes from the action in its title row as its way out does', async () => {
    const given = answers();
    render(<Popup title="Quit lazyclaude?" {...given} />);
    await userEvent.click(screen.getByRole('button', { name: 'Close (esc)' }));
    expect(given.out.onClick).toHaveBeenCalledTimes(1);
  });

  it('draws no body between its title row and its foot when it holds nothing', () => {
    const { container } = render(<Popup title="Submit review as Comment?" {...answers()} />);
    expect(container.querySelector('[data-popup-body]')).toBeNull();
  });

  it('draws the body when it holds something', () => {
    const { container } = render(
      <Popup title="Quit lazyclaude?" {...answers()}>
        <p>Asked.</p>
      </Popup>,
    );
    expect(container.querySelector('[data-popup-body]')).toHaveTextContent('Asked.');
  });

  it('does nothing for a press on the scrim when it is not given a dismissal', () => {
    const given = answers();
    render(<Popup title="Quit lazyclaude?" {...given} />);
    fireEvent.mouseDown(screen.getByRole('dialog').parentElement!);
    expect(given.out.onClick).not.toHaveBeenCalled();
  });

  it('is dismissed by a press on the scrim itself, not by one inside it', () => {
    const onDismiss = vi.fn();
    render(<Popup title="Quit lazyclaude?" {...answers()} onDismiss={onDismiss} />);
    fireEvent.mouseDown(screen.getByRole('dialog'));
    expect(onDismiss).not.toHaveBeenCalled();
    fireEvent.mouseDown(screen.getByRole('dialog').parentElement!);
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('is not dismissed by a click, so a selection dragged out of it stays', () => {
    const onDismiss = vi.fn();
    render(<Popup title="Quit lazyclaude?" {...answers()} onDismiss={onDismiss} />);
    fireEvent.click(screen.getByRole('dialog').parentElement!);
    expect(onDismiss).not.toHaveBeenCalled();
  });

  it('is drawn from the tokens of a lit title row, a sunk foot and a dimmed ground', () => {
    expect(ruleOf(sheet, '.scrim')).toContain('background: color-mix(in srgb, var(--ground) 55%, transparent)');
    expect(ruleOf(sheet, '.frame')).toContain('border-radius: var(--pane-radius)');
    expect(ruleOf(sheet, '.frame')).toContain('box-shadow: var(--popup-shadow)');
    expect(ruleOf(sheet, '.head')).toContain('background: var(--lit)');
    expect(ruleOf(sheet, '.foot')).toContain('background: var(--pane-sunk)');
  });
});
