import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PanelTabs } from './PanelTabs';

const TABS = [
  { id: 'board', label: 'Board', count: 110 },
  { id: 'docs', label: 'Docs', count: 0 },
];

describe('the panel tabs', () => {
  it('names each tab with its count, a real zero printed', () => {
    render(<PanelTabs tabs={TABS} selected="board" onSelect={() => {}} />);
    expect(screen.getByRole('tab', { name: 'Board 110' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Docs 0' })).toHaveAttribute('aria-selected', 'false');
  });

  it('hands a click on a tab to the screen', async () => {
    const onSelect = vi.fn();
    render(<PanelTabs tabs={TABS} selected="board" onSelect={onSelect} />);
    await userEvent.click(screen.getByRole('tab', { name: 'Docs 0' }));
    expect(onSelect).toHaveBeenCalledWith('docs');
  });
});
