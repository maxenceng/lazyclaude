import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Pane, PaneActionKind } from './Pane';

describe('a pane', () => {
  it('carries no button when it has no action', () => {
    render(<Pane title="Ticket" focused={false}>body</Pane>);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('carries its action in the title row, named by its label, and hands a click to it', async () => {
    const onClick = vi.fn();
    render(
      <Pane title="Document" focused={false} action={{ kind: PaneActionKind.Close, label: 'Close document', onClick }}>
        body
      </Pane>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Close document' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
