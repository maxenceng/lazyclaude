import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { SessionWorktreeShownView } from '../protocol';
import { WorktreeSection } from './WorktreeSection';

const PATH = '../app.worktrees/proj-1-a-ticket';
type Shown = Exclude<SessionWorktreeShownView, { kind: 'noWorktree' }>;

const draw = (shown: Shown | undefined, refusal?: string, onPress = () => {}, removable = shown?.kind === 'clearWorktree') =>
  render(<WorktreeSection shown={shown} branch="proj-1-a-ticket" refusal={refusal} removable={removable} onPress={onPress} />);

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

  it('inks the clean pill with the neutral token, and no other pill', () => {
    const { unmount } = draw({ kind: 'clearWorktree', shown: PATH });
    expect(screen.getByRole('img').className).toContain('cleanPill');
    unmount();
    draw({ kind: 'unreadWorktree', reason: 'x', held: 'cannot remove: unreadable (x)' });
    expect(screen.getByRole('img').className).not.toContain('cleanPill');
  });

  it('presses through the row', async () => {
    const onPress = vi.fn();
    draw({ kind: 'clearWorktree', shown: PATH }, undefined, onPress);
    await userEvent.click(screen.getByRole('button'));
    expect(onPress).toHaveBeenCalledOnce();
  });

  it('says held with the reason under the row and offers no remove, for what is in the worktree', () => {
    draw({ kind: 'notClearWorktree', shown: PATH, state: { kind: 'worktreeContents', dirtyPaths: ['a'], commitsBeyondMerge: 0, sessions: [] }, held: 'cannot remove: 1 dirty path' });
    const row = screen.getByText(PATH).closest('li') as HTMLElement;
    expect(row).toHaveTextContent('held');
    expect(row).not.toHaveTextContent('remove');
    expect(screen.getByText('cannot remove: 1 dirty path')).toBeInTheDocument();
  });

  it.each([
    ['held', { kind: 'notClearWorktree', shown: PATH, state: { kind: 'worktreeContents', dirtyPaths: ['a'], commitsBeyondMerge: 0, sessions: [] }, held: 'cannot remove: 1 dirty path' }],
    ['no merged pull request', { kind: 'notClearWorktree', shown: PATH, state: { kind: 'noMergedPullRequest' }, held: 'cannot remove: no merged pull request' }],
    ['unreadable', { kind: 'unreadWorktree', reason: 'x', held: 'cannot remove: unreadable (x)' }],
  ] as Array<[string, Shown]>)('exposes no button on a %s row, which acts on nothing', (_name, shown) => {
    draw(shown);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('says unknown for a branch with no merged pull request', () => {
    draw({ kind: 'notClearWorktree', shown: PATH, state: { kind: 'noMergedPullRequest' }, held: 'cannot remove: no merged pull request' });
    expect(screen.getByText(PATH).closest('li')).toHaveTextContent('unknown');
    expect(screen.getByText('cannot remove: no merged pull request')).toBeInTheDocument();
  });

  it('says unknown for an unreadable worktree, naming the branch where there is no path', () => {
    draw({ kind: 'unreadWorktree', reason: 'git exited 128', held: 'cannot remove: unreadable (git exited 128)' });
    const row = screen.getByText('proj-1-a-ticket').closest('li') as HTMLElement;
    expect(row).toHaveTextContent('unknown');
    expect(screen.getByText('cannot remove: unreadable (git exited 128)')).toBeInTheDocument();
  });

  it('shows why the last removal removed nothing, beside the row', () => {
    draw({ kind: 'clearWorktree', shown: PATH }, 'cannot remove: 1 dirty path');
    expect(screen.getByRole('alert')).toHaveTextContent('cannot remove: 1 dirty path');
  });

  it('shows why the last removal removed nothing even where no worktree is drawn any more', () => {
    draw(undefined, 'cannot remove: git worktree remove exited 128');
    expect(screen.getByRole('alert').textContent, 'a re-read that fails or finds no worktree must not hide the reason of the removal before it').toBe('cannot remove: git worktree remove exited 128');
  });

  it('offers no remove on a clean worktree that is not removable, as while its removal is in flight', () => {
    draw({ kind: 'clearWorktree', shown: PATH }, undefined, () => {}, false);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('clean')).toBeInTheDocument();
  });
});
