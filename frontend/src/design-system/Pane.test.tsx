import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Pane, PaneActionKind } from './Pane';
import { ruleOf, stylesheet } from './stylesheet';

const CROSS = 'M2 2 L10 10 M10 2 L2 10';
const pane = stylesheet('Pane.module.css');

const iconOf = (button: HTMLElement): string => button.querySelector('path')?.getAttribute('d') ?? '';

describe('a pane', () => {
  it('carries no button when it has no action', () => {
    render(<Pane title="Ticket" focused={false}>body</Pane>);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('carries its action in the title row, named by its label, and hands a click to it', async () => {
    const onClick = vi.fn();
    render(
      <Pane title="Document" focused={false} actions={[{ kind: PaneActionKind.Close, label: 'Close document', shortcut: 'esc', onClick }]}>
        body
      </Pane>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Close document (esc)' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('leaves the focus where it was when its action is clicked', async () => {
    render(
      <>
        <input aria-label="Elsewhere" />
        <Pane title="Document" focused={false} actions={[{ kind: PaneActionKind.Close, label: 'Close document', shortcut: 'esc', onClick: vi.fn() }]}>
          body
        </Pane>
      </>,
    );
    screen.getByRole('textbox', { name: 'Elsewhere' }).focus();
    await userEvent.click(screen.getByRole('button', { name: 'Close document (esc)' }));
    expect(screen.getByRole('textbox', { name: 'Elsewhere' }), 'the window keeps its own focus; a click must not hand it to the button').toHaveFocus();
  });

  it('names each action and its key in its tooltip and its accessible label', () => {
    render(
      <Pane title="Dock" focused={false} actions={[{ kind: PaneActionKind.Zoom, label: 'Zoom dock', shortcut: 'z', onClick: vi.fn() }]}>
        body
      </Pane>,
    );
    const button = screen.getByRole('button', { name: 'Zoom dock (z)' });
    expect(button).toHaveAttribute('title', 'Zoom dock (z)');
  });

  it('draws every kind of action with its own icon, and only close as a cross', () => {
    const kinds = Object.values(PaneActionKind);
    render(
      <Pane title="All" focused={false} actions={kinds.map((kind) => ({ kind, label: kind, shortcut: 'k', onClick: vi.fn() }))}>
        body
      </Pane>,
    );
    const icons = kinds.map((kind) => iconOf(screen.getByRole('button', { name: `${kind} (k)` })));
    expect(new Set(icons).size, 'one mark per kind').toBe(kinds.length);
    expect(icons.filter((icon) => icon === CROSS)).toEqual([iconOf(screen.getByRole('button', { name: `${PaneActionKind.Close} (k)` }))]);
  });

  it('lines several actions up in one slot at the right end of the title row', () => {
    render(
      <Pane
        title="Dock"
        focused={false}
        actions={[
          { kind: PaneActionKind.Zoom, label: 'Zoom dock', shortcut: 'z', onClick: vi.fn() },
          { kind: PaneActionKind.Minimise, label: 'Minimise dock', shortcut: 'M', onClick: vi.fn() },
        ]}
      >
        body
      </Pane>,
    );
    const slot = screen.getByRole('button', { name: 'Zoom dock (z)' }).parentElement;
    expect(slot).toContainElement(screen.getByRole('button', { name: 'Minimise dock (M)' }));
    expect(ruleOf(pane, '.slot')).toContain('margin-left: auto');
  });

  it('lights the title row of the focused pane in the lit ground and leaves the others on the pane', () => {
    expect(ruleOf(pane, '.focused .head')).toContain('background: var(--lit)');
    expect(ruleOf(pane, '.focused .head')).toContain('color: var(--lit-ink)');
  });

  it('outlines the selected row of an unfocused pane and fills it only in the focused one', () => {
    expect(ruleOf(pane, '.pane')).toContain('--cursor-edge: inset 0 0 0 1px var(--cursor-border)');
    expect(ruleOf(pane, '.focused')).toContain('--cursor-ground: var(--selected)');
    expect(ruleOf(pane, '.focused')).toContain('--cursor-edge: none');
  });

  it('pins its foot under the body, which is the part that scrolls', () => {
    render(
      <Pane title="Ticket" focused={false} foot={<button type="button">pinned</button>}>
        <p>scrolling</p>
      </Pane>,
    );
    const body = screen.getByText('scrolling').closest('[data-pane-body]');
    const foot = screen.getByRole('button', { name: 'pinned' }).closest('[data-pane-foot]');
    expect(body).not.toBeNull();
    expect(foot).not.toBeNull();
    expect(body).not.toContainElement(screen.getByRole('button', { name: 'pinned' }));
    expect(ruleOf(pane, '.foot')).toContain('flex: none');
  });
});

describe('a pane with panel tabs', () => {
  it('names the tabs in its title row, the pane itself labelled by its title', () => {
    render(
      <Pane title="Panels" focused tabs={{ tabs: [{ id: 'a', label: 'A', count: 1 }], selected: 'a', onSelect: () => {} }}>
        body
      </Pane>,
    );
    expect(screen.getByRole('region', { name: 'Panels' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'A 1' })).toBeInTheDocument();
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });
});
