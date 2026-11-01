import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ConnectionScreen } from './ConnectionScreen';

describe('the connection screen', () => {
  it('says the sidecar is starting while it is connecting', () => {
    render(<ConnectionScreen status={{ kind: 'connecting' }} onRestart={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('Starting the backend');
  });

  it('refuses a protocol it was not generated for, naming both versions, with no restart', () => {
    render(<ConnectionScreen status={{ kind: 'refused', expected: 1, actual: 2 }} onRestart={() => {}} />);
    expect(screen.getByRole('alert')).toHaveTextContent('speaks protocol 2');
    expect(screen.getByRole('alert')).toHaveTextContent('built for protocol 1');
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('says the backend stopped, shows the last stderr lines and offers a restart', async () => {
    const onRestart = vi.fn();
    render(<ConnectionScreen status={{ kind: 'exited', code: 1, stderr: ['first', 'out of memory'] }} onRestart={onRestart} />);
    expect(screen.getByRole('alert')).toHaveTextContent('The backend stopped');
    expect(screen.getByRole('alert')).toHaveTextContent('exit code 1');
    expect(screen.getByRole('alert')).toHaveTextContent('first');
    expect(screen.getByRole('alert')).toHaveTextContent('out of memory');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Restart' }));
    expect(onRestart).toHaveBeenCalledOnce();
  });

  it('does not restart by itself', () => {
    const onRestart = vi.fn();
    render(<ConnectionScreen status={{ kind: 'exited', code: null, stderr: [] }} onRestart={onRestart} />);
    expect(onRestart).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('no exit code');
  });

  it('shows the line of a refused config.yml the backend left on stderr before it was ready', () => {
    const refusal = 'lazyclaude: config.yml:3: unknown key mouse2';
    render(<ConnectionScreen status={{ kind: 'exited', code: 1, stderr: [refusal] }} onRestart={() => {}} />);
    expect(screen.getByRole('alert')).toHaveTextContent(refusal);
  });
});
