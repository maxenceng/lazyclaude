import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { RpcError } from '../protocol';
import { added, diffFixture, fileFixture, hunk, kept, onLine, openPending, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff, threadsFixture } from './fixtures';
import { CommentsPane, DiffPane, FilesPane, ReviewDialogs, ReviewPanel } from './ReviewScreen';
import { Harness, settle } from './sessionHarness';

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const renderDiff = (harness: Harness, focused = true) => {
  const onFocus = vi.fn();
  const onRetry = vi.fn();
  render(<DiffPane session={harness.session} focused={focused} onFocus={onFocus} onRetry={onRetry} />);
  return { onFocus, onRetry };
};

describe('the diff of a review', () => {
  it('says it is reading while the diff has not answered', () => {
    const harness = new Harness();
    harness.session.read();
    renderDiff(harness);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the diff');
  });

  it('says why the pull request cannot be read', async () => {
    const harness = new Harness();
    harness.session.read();
    renderDiff(harness);
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'gh is not signed in', tooLarge: false });
    expect(screen.getByText('PR unknown (gh is not signed in)')).toBeInTheDocument();
  });

  it('says the diff is too large for GitHub', async () => {
    const harness = new Harness();
    harness.session.read();
    renderDiff(harness);
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: '20000 lines', tooLarge: true });
    expect(screen.getByText('diff too large for GitHub (20000 lines)')).toBeInTheDocument();
  });

  it('says the pull request changes nothing', async () => {
    const harness = new Harness();
    await harness.readAll({ diff: diffFixture([]) });
    renderDiff(harness);
    expect(screen.getByText('no changes')).toBeInTheDocument();
  });

  it('shows the failure of the read with a retry', async () => {
    const harness = new Harness();
    harness.session.read();
    const { onRetry } = renderDiff(harness);
    await harness.fail('review/diffOf', new RpcError(-32000, 'sidecar gone'));
    expect(screen.getByRole('alert')).toHaveTextContent('sidecar gone');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('titles the pane with the pull request', () => {
    const harness = new Harness();
    renderDiff(harness);
    expect(screen.getByRole('region', { name: 'PR #7 Add the thing' })).toBeInTheDocument();
  });

  it('shows each file under a rule with its hunks and their lines between two number gutters, a sign saying what changed', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    expect(screen.getByRole('heading', { name: 'src/a.ts' })).toBeInTheDocument();
    expect(screen.getByText('@@ -1,2 +1,3 @@')).toBeInTheDocument();
    const lines = screen.getAllByRole('button');
    expect(lines.map((line) => line.textContent)).toEqual(['11 one', '2-two', '2+deux', '1+b']);
    expect(within(lines[1]!).getByText('-')).toBeInTheDocument();
    expect(within(lines[2]!).getByText('+')).toBeInTheDocument();
  });

  it('marks the cursor line as current', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    act(() => harness.session.moveLine(2));
    expect(screen.getAllByRole('button')[2]).toHaveAttribute('aria-current', 'true');
    expect(screen.getAllByRole('button')[0]).not.toHaveAttribute('aria-current');
  });

  it('sets a thread and a pending comment under their line', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 }, comments: [{ author: 'ada', body: 'Why this?' }, { author: 'bob', body: 'Speed' }] });
    const comment = pendingCommentFixture({ body: 'Rename this' });
    await harness.readAll({
      diff,
      threads: threadsFixture([thread]),
      pending: openPending([comment]),
      placedThreads: [threadInDiff(thread, onLine(0))],
      placedPending: [pendingInDiff(comment, onLine(0))],
    });
    renderDiff(harness);
    const articles = screen.getAllByRole('article');
    expect(articles[0]).toHaveTextContent('◆ L1–2');
    expect(articles[0]).toHaveTextContent('ada Why this?');
    expect(articles[0]).toHaveTextContent('bob Speed');
    expect(articles[1]).toHaveTextContent('✎ L1');
    expect(articles[1]).toHaveTextContent('you Rename this');
  });

  it('puts the cursor on a line pressed, focusing the diff', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const { onFocus } = renderDiff(harness, false);
    fireEvent.mouseDown(screen.getAllByRole('button')[2]!);
    fireEvent.mouseUp(screen.getAllByRole('button')[2]!);
    expect(onFocus).toHaveBeenCalled();
    expect(harness.session.cursor()).toBe(2);
  });

  it('marks the range dragged from a line to the line the pointer is released on', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(screen.getAllByRole('button')[0]!);
    fireEvent.mouseUp(screen.getAllByRole('button')[2]!);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    await act(async () => {
      harness.client.answerNext({ kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 } });
      await settle();
    });
    expect(screen.getAllByRole('button').slice(0, 3).every((line) => line.className.includes('range'))).toBe(true);
  });

  it('marks a range dragged upward, released on a line above the one pressed', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(screen.getAllByRole('button')[3]!);
    fireEvent.mouseUp(screen.getAllByRole('button')[1]!);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 1, last: 3 } });
  });
});

const withThreads = async (harness: Harness) => {
  const open = threadFixture({ id: 'T1', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 }, comments: [{ author: 'ada', body: 'Why?' }] });
  const left = threadFixture({ id: 'T2', path: 'src/a.ts', position: { kind: 'placedThread', side: 'LEFT', first: 2, last: 2 }, resolved: true, open: false, comments: [{ author: 'bob', body: 'Gone' }] });
  const outdated = threadFixture({ id: 'T3', path: 'src/b.ts', position: { kind: 'outdatedThread' }, open: false, comments: [{ author: 'cy', body: 'Old' }] });
  const comment = pendingCommentFixture({ id: 'C1', anchor: { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 2, last: 2 } } });
  await harness.readAll({
    diff,
    threads: threadsFixture([open, left, outdated]),
    pending: openPending([comment]),
    placedThreads: [threadInDiff(open, onLine(0)), threadInDiff(left, onLine(1)), threadInDiff(outdated, { kind: 'noDiffLine' })],
    placedPending: [pendingInDiff(comment, onLine(2))],
  });
};

describe('the Files of a review', () => {
  it('lists each file with the letter for what the pull request does to it, its name, and the open threads and pending comments on it', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<FilesPane session={harness.session} focused onFocus={() => {}} />);
    const rows = screen.getAllByRole('button');
    expect(rows[0]).toHaveTextContent('Ma.ts◆1✎1');
    expect(rows[1]).toHaveTextContent('Mb.ts');
    expect(within(rows[0]!).getByLabelText('1 open thread')).toBeInTheDocument();
    expect(within(rows[0]!).getByLabelText('1 pending comment')).toBeInTheDocument();
  });

  it('names a renamed file by its old and new names', async () => {
    const harness = new Harness();
    await harness.readAll({ diff: diffFixture([fileFixture('src/new.ts', [], { kind: 'renamedFile', from: 'lib/old.ts' })]) });
    render(<FilesPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getByRole('button')).toHaveTextContent('Rold.ts → new.ts');
  });

  it('marks the selected file, selects one by a press, and goes to the first line of the selected one by a second', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const onFocus = vi.fn();
    render(<FilesPane session={harness.session} focused onFocus={onFocus} />);
    expect(screen.getAllByRole('button')[0]).toHaveAttribute('aria-current', 'true');
    await userEvent.click(screen.getAllByRole('button')[1]!);
    expect(harness.session.selectedFile()).toBe(1);
    expect(harness.session.cursor()).toBe(0);
    await userEvent.click(screen.getAllByRole('button')[1]!);
    expect(harness.session.cursor()).toBe(3);
    expect(onFocus).toHaveBeenCalled();
  });

  it('lists nothing until the diff is read', () => {
    const harness = new Harness();
    harness.session.read();
    render(<FilesPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the diff');
  });
});

describe('the Comments of a review', () => {
  it('lists the pending comments first, then the threads, each with where it hangs', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getAllByRole('button').map((row) => row.textContent)).toEqual(['✎a.ts:2', '◆a.ts:1–2 ada', 'a.ts:-2 bob', 'b.ts  outdated']);
  });

  it('dims a resolved thread', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getAllByRole('button')[2]!.className).toContain('dim');
    expect(screen.getAllByRole('button')[1]!.className).not.toContain('dim');
  });

  it('says it is reading while the threads have not answered', () => {
    const harness = new Harness();
    harness.session.read();
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the threads');
  });

  it('says why the threads cannot be read', async () => {
    const harness = new Harness();
    harness.session.read();
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    await harness.answer('review/threadsOf', { kind: 'unavailableThreads', reason: 'rate limited' });
    expect(screen.getByText('threads unknown (rate limited)')).toBeInTheDocument();
  });

  it('says there are no threads', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    expect(screen.getByText('no threads')).toBeInTheDocument();
  });

  it('selects an entry by a press, and goes to its line by a second', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane session={harness.session} focused onFocus={() => {}} />);
    await userEvent.click(screen.getAllByRole('button')[2]!);
    expect(harness.session.selectedComment()).toBe(2);
    await userEvent.click(screen.getAllByRole('button')[2]!);
    expect(harness.session.cursor()).toBe(1);
  });
});

describe('the Review panel', () => {
  it('says it is reading while the pending review has not answered', () => {
    const harness = new Harness();
    harness.session.read();
    render(<ReviewPanel session={harness.session} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the pending review');
  });

  it('says there is no pending review', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewPanel session={harness.session} />);
    expect(screen.getByText('no pending review')).toBeInTheDocument();
  });

  it('says why the pending review is unknown', async () => {
    const harness = new Harness();
    harness.session.read();
    render(<ReviewPanel session={harness.session} />);
    await harness.answer('review/pendingOf', { kind: 'unavailablePendingReview', reason: 'no scope' });
    expect(screen.getByText('pending unknown (no scope)')).toBeInTheDocument();
  });

  it('counts the pending comments under the first line of the summary, or says there is no summary', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([pendingCommentFixture(), pendingCommentFixture({ id: 'C2' })], { summary: 'Mostly fine\nbut see below' }) });
    render(<ReviewPanel session={harness.session} />);
    expect(screen.getByText('Mostly fine')).toBeInTheDocument();
    expect(screen.getByText('2 pending')).toBeInTheDocument();
    expect(screen.queryByText(/see below/)).not.toBeInTheDocument();
  });

  it('says there is no summary when the pending review has none', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([]) });
    render(<ReviewPanel session={harness.session} />);
    expect(screen.getByText('no summary')).toBeInTheDocument();
    expect(screen.getByText('0 pending')).toBeInTheDocument();
  });

  it('warns when the branch moved since the review was written', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([], { headMove: { kind: 'movedHead', writtenOn: 'abcdef0123456789', head: '1234567890abcdef' } }) });
    render(<ReviewPanel session={harness.session} />);
    expect(screen.getByText('written on abcdef0,')).toBeInTheDocument();
    expect(screen.getByText('head is now 1234567')).toBeInTheDocument();
  });

  it('shows the summary typed for the submit even before any comment is written', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewPanel session={harness.session} />);
    act(() => {
      harness.session.openSummary();
      harness.session.typeInBox('Typed first');
      harness.session.saveBox();
    });
    expect(screen.getByText('Typed first')).toBeInTheDocument();
    expect(screen.getByText('0 pending')).toBeInTheDocument();
  });
});

const anchored = { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 1 } };

const openedBox = async (harness: Harness) => {
  await harness.readAll({ diff });
  const opening = harness.session.comment();
  harness.client.answerNext(anchored);
  await act(async () => {
    await opening;
  });
};

describe('the comment box of a review', () => {
  it('shows nothing while no box is open', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewDialogs session={harness.session} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('is a popup titled for a pending comment, holding what is typed', async () => {
    const harness = new Harness();
    render(<ReviewDialogs session={harness.session} />);
    await openedBox(harness);
    expect(screen.getByRole('dialog', { name: 'Pending comment' })).toBeInTheDocument();
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    expect(harness.session.box()?.text).toBe('Rename this');
  });

  it('saves with ctrl+s and says it is saving, holding the text still', async () => {
    const harness = new Harness();
    render(<ReviewDialogs session={harness.session} />);
    await openedBox(harness);
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    await userEvent.keyboard('{Control>}s{/Control}');
    expect(harness.client.calls.at(-1)?.method).toBe('review/comment');
    expect(screen.getByText('saving…')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Comment text' })).toHaveAttribute('readonly');
  });

  it('says why a comment was not saved, keeping the text to save again', async () => {
    const harness = new Harness();
    render(<ReviewDialogs session={harness.session} />);
    await openedBox(harness);
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    await userEvent.keyboard('{Control>}s{/Control}');
    await harness.answer('review/comment', { kind: 'refusedWrite', reason: 'GitHub is down' });
    expect(screen.getByText('not saved (GitHub is down)')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Comment text' })).toHaveValue('Rename this');
    expect(screen.getByRole('textbox', { name: 'Comment text' })).not.toHaveAttribute('readonly');
  });

  it('is thrown away by escape', async () => {
    const harness = new Harness();
    render(<ReviewDialogs session={harness.session} />);
    await openedBox(harness);
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(harness.session.box()).toBeUndefined();
  });

  it('shows the thread a reply answers above the box, titled with its first line', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1', comments: [{ author: 'ada', body: 'Why this?\nReally.' }, { author: 'bob', body: 'Speed' }] });
    await harness.readAll({ diff, threads: threadsFixture([thread]), placedThreads: [threadInDiff(thread, onLine(0))] });
    render(<ReviewDialogs session={harness.session} />);
    act(() => harness.session.commentOnSelectedComment());
    expect(screen.getByRole('dialog', { name: 'Why this?' })).toBeInTheDocument();
    expect(screen.getByText(/ada\s+Why this\?/)).toBeInTheDocument();
    expect(screen.getByText(/bob\s+Speed/)).toBeInTheDocument();
  });

  it('is titled Summary for the summary, and keeps it for the submit', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewDialogs session={harness.session} />);
    act(() => harness.session.openSummary());
    expect(screen.getByRole('dialog', { name: 'Summary' })).toBeInTheDocument();
  });
});

const confirming = async () => {
  const harness = new Harness();
  await harness.readAll({ diff });
  render(<ReviewDialogs session={harness.session} />);
  act(() => {
    harness.session.openSummary();
    harness.session.saveBox();
  });
  return harness;
};

describe('the confirm after the summary', () => {
  it('asks whether to submit the review as a Comment', async () => {
    await confirming();
    expect(screen.getByRole('dialog', { name: 'Submit review as Comment?' })).toBeInTheDocument();
  });

  it('submits on Submit', async () => {
    const harness = await confirming();
    await userEvent.click(screen.getByRole('button', { name: 'Submit' }));
    expect(harness.client.calls.at(-1)?.method).toBe('review/submit');
  });

  it('keeps the review pending on Keep pending, writing nothing', async () => {
    const harness = await confirming();
    const calls = harness.client.calls.length;
    await userEvent.click(screen.getByRole('button', { name: 'Keep pending' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(harness.client.calls).toHaveLength(calls);
  });
});
