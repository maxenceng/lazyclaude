import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { SessionWorktreeShownView } from '../protocol';
import { WorktreeSection } from './WorktreeSection';

const PATH = '../app.worktrees/proj-1-a-ticket';
type Shown = Exclude<SessionWorktreeShownView, { kind: 'noWorktree' }>;

const draw = (shown: Shown | undefined, refusal?: string, onPress = () => {}) =>
  render(<WorktreeSection shown={shown} branch="proj-1-a-ticket" refusal={refusal} onPress={onPress} />);

describe('a done ticket\'s worktree section', () => {
  it('draws nothing where no worktree is shown', () => {
    draw(undefined);
    expect(screen.queryByRole('heading', { name: 'worktree' })).toBeNull();
  });

  it('draws a clean pill, the path and remove with d, in one button', () => {
    draw({ kind: 'clearWorktree', shown: PATH });
    const row = screen.getByRole('button', { name: /proj-1-a-ticket/ });
    expect(screen.getByRole('heading', { name: 'worktree' })).toBeInTheDocument();
    expect(row).toHaveTextContent('clean');
    expect(row).toHaveTextContent(PATH);
    expect(row).toHaveTextContent('remove');
    expect(row).toHaveTextContent('d');
  });

  it('presses through the row', async () => {
    const onPress = vi.fn();
    draw({ kind: 'clearWorktree', shown: PATH }, undefined, onPress);
    await userEvent.click(screen.getByRole('button'));
    expect(onPress).toHaveBeenCalledOnce();
  });

  it('says held with the reason under the row and offers no remove, for what is in the worktree', async () => {
    const onPress = vi.fn();
    draw({ kind: 'heldBackWorktree', shown: PATH, state: { kind: 'worktreeContents', dirtyPaths: ['a'], commitsBeyondMerge: 0, sessions: [] }, held: 'cannot remove: 1 dirty path' }, undefined, onPress);
    const row = screen.getByRole('button');
    expect(row).toHaveTextContent('held');
    expect(row).toHaveTextContent(PATH);
    expect(row).not.toHaveTextContent('remove');
    expect(screen.getByText('cannot remove: 1 dirty path')).toBeInTheDocument();
    await userEvent.click(row);
    expect(onPress).not.toHaveBeenCalled();
  });

  it('says unknown for a branch with no merged pull request', () => {
    draw({ kind: 'heldBackWorktree', shown: PATH, state: { kind: 'noMergedPullRequest' }, held: 'cannot remove: no merged pull request' });
    expect(screen.getByRole('button')).toHaveTextContent('unknown');
    expect(screen.getByText('cannot remove: no merged pull request')).toBeInTheDocument();
  });

  it('says unknown for an unreadable worktree, naming the branch where there is no path', () => {
    draw({ kind: 'unreadWorktree', reason: 'git exited 128', held: 'cannot remove: unreadable (git exited 128)' });
    const row = screen.getByRole('button');
    expect(row).toHaveTextContent('unknown');
    expect(row).toHaveTextContent('proj-1-a-ticket');
    expect(screen.getByText('cannot remove: unreadable (git exited 128)')).toBeInTheDocument();
  });

  it('shows why the last removal removed nothing, beside the row', () => {
    draw({ kind: 'clearWorktree', shown: PATH }, 'cannot remove: 1 dirty path');
    expect(screen.getByRole('alert')).toHaveTextContent('cannot remove: 1 dirty path');
  });
});
