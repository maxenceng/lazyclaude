import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { RpcError } from '../protocol';
import { added, commentsFixture, diffFixture, fileFixture, hunk, kept, onLine, openPending, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff, threadsFixture } from './fixtures';
import { CommentsPane, DiffPane, FilesPane, ReviewDialogs, ReviewPanel } from './ReviewScreen';
import { Harness, settle } from './reviewHarness';

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const renderDiff = (harness: Harness, focused = true) => {
  const onFocus = vi.fn();
  const onRetry = vi.fn();
  render(<DiffPane review={harness.review} focused={focused} onFocus={onFocus} onRetry={onRetry} />);
  return { onFocus, onRetry };
};

describe('the diff of a review', () => {
  it('says it is reading while the diff has not answered', () => {
    const harness = new Harness();
    harness.review.read();
    renderDiff(harness);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the diff');
  });

  it('says why the pull request cannot be read', async () => {
    const harness = new Harness();
    harness.review.read();
    renderDiff(harness);
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'gh is not signed in', tooLarge: false });
    expect(screen.getByText('PR unknown (gh is not signed in)')).toBeInTheDocument();
  });

  it('says the diff is too large for GitHub', async () => {
    const harness = new Harness();
    harness.review.read();
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
    harness.review.read();
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
    act(() => harness.review.moveLine(2));
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
      comments: commentsFixture({ threads: [threadInDiff(thread, onLine(0))], underLines: [threadInDiff(thread, onLine(0))], pending: [pendingInDiff(comment, onLine(0))] }),
    });
    renderDiff(harness);
    const articles = screen.getAllByRole('article');
    expect(articles[0]).toHaveTextContent('◆ L1–2');
    expect(articles[0]).toHaveTextContent('ada Why this?');
    expect(articles[0]).toHaveTextContent('bob Speed');
    expect(articles[1]).toHaveTextContent('✎ L1');
    expect(articles[1]).toHaveTextContent('you Rename this');
  });

  it('ends a thread with a Reply, and a pending comment with none', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    const comment = pendingCommentFixture({ body: 'Rename this' });
    await harness.readAll({
      diff,
      threads: threadsFixture([thread]),
      pending: openPending([comment]),
      comments: commentsFixture({ threads: [threadInDiff(thread, onLine(0))], underLines: [threadInDiff(thread, onLine(0))], pending: [pendingInDiff(comment, onLine(0))] }),
    });
    renderDiff(harness);
    const articles = screen.getAllByRole('article');
    expect(within(articles[0]!).getByRole('button', { name: 'Reply to the thread on L1' })).toHaveTextContent('Reply');
    expect(within(articles[1]!).queryByRole('button')).not.toBeInTheDocument();
  });

  it('opens the reply box on a click on Reply, resolved thread included, moving neither the cursor nor the focus', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1', resolved: true, open: false, comments: [{ author: 'ada', body: 'Why this?' }] });
    await harness.readAll({ diff, threads: threadsFixture([thread]), comments: commentsFixture({ threads: [threadInDiff(thread, onLine(0))], underLines: [threadInDiff(thread, onLine(0))], listed: [threadInDiff(thread, onLine(0))] }), });
    harness.review.moveLine(2);
    const onFocus = vi.fn();
    render(
      <>
        <DiffPane review={harness.review} focused={false} onFocus={onFocus} onRetry={() => {}} />
        <ReviewDialogs review={harness.review} />
      </>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Reply to the thread on L1' }));
    expect(screen.getByRole('dialog', { name: 'Why this?' })).toBeInTheDocument();
    expect(harness.review.cursor()).toBe(2);
    expect(onFocus).not.toHaveBeenCalled();
  });

  it('puts the cursor on a line pressed, focusing the diff', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const { onFocus } = renderDiff(harness, false);
    fireEvent.mouseDown(screen.getAllByRole('button')[2]!);
    fireEvent.mouseUp(screen.getAllByRole('button')[2]!);
    expect(onFocus).toHaveBeenCalled();
    expect(harness.review.cursor()).toBe(2);
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

  it('takes no press of a button other than the left one', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(screen.getAllByRole('button')[2]!, { button: 2 });
    expect(harness.review.cursor()).toBe(0);
  });

  it('ends a drag at a press off the diff, so a release the window never saw marks nothing later', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    const callsBefore = harness.client.calls.length;
    fireEvent.mouseDown(screen.getAllByRole('button')[0]!);
    fireEvent.mouseDown(document.body);
    fireEvent.mouseUp(screen.getAllByRole('button')[2]!);
    expect(harness.client.calls).toHaveLength(callsBefore);
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
    comments: commentsFixture({
      threads: [threadInDiff(open, onLine(0)), threadInDiff(left, onLine(1)), threadInDiff(outdated, { kind: 'noDiffLine' })],
      underLines: [threadInDiff(open, onLine(0))],
      pending: [pendingInDiff(comment, onLine(2))],
      listed: [pendingInDiff(comment, onLine(2)), threadInDiff(open, onLine(0)), threadInDiff(left, onLine(1)), threadInDiff(outdated, { kind: 'noDiffLine' })],
      lastOnLines: [pendingInDiff(comment, onLine(2))],
    }),
  });
};

describe('the Files of a review', () => {
  it('lists each file with the letter for what the pull request does to it, its name, and the open threads and pending comments on it', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    const rows = screen.getAllByRole('button');
    expect(rows[0]).toHaveTextContent('Ma.ts◆1✎1');
    expect(rows[1]).toHaveTextContent('Mb.ts');
    expect(within(rows[0]!).getByLabelText('1 open thread')).toBeInTheDocument();
    expect(within(rows[0]!).getByLabelText('1 pending comment')).toBeInTheDocument();
  });

  it('names a renamed file by its old and new names', async () => {
    const harness = new Harness();
    await harness.readAll({ diff: diffFixture([fileFixture('src/new.ts', [], { kind: 'renamedFile', from: 'lib/old.ts' })]) });
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByRole('button')).toHaveTextContent('Rold.ts → new.ts');
  });

  it('marks the selected file, and a press selects one and goes to its first line', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const onFocus = vi.fn();
    render(<FilesPane review={harness.review} focused={false} onFocus={onFocus} />);
    expect(screen.getAllByRole('button')[0]).toHaveAttribute('aria-current', 'true');
    await userEvent.click(screen.getAllByRole('button')[1]!);
    expect(harness.review.selectedFile()).toBe(1);
    expect(harness.review.cursor(), 'one press goes, even on a pane that had no focus').toBe(3);
    expect(onFocus).toHaveBeenCalled();
  });

  it('lists nothing until the diff is read', () => {
    const harness = new Harness();
    harness.review.read();
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the diff');
  });
});

describe('the Comments of a review', () => {
  it('lists the entries in the order review lists them, each with where it hangs, who spoke last on the right and its first comment under it', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getAllByRole('button').map((row) => row.textContent)).toEqual(['✎a.ts:2Rename this', '◆a.ts:1–2adaWhy?', 'a.ts:-2bobGone', 'b.ts  outdatedcyOld']);
  });

  it('counts replies after the first comment, in the singular and the plural, beside who spoke last', async () => {
    const harness = new Harness();
    const one = threadFixture({ id: 'T1', comments: [{ author: 'ada', body: 'Why?' }, { author: 'bob', body: 'a' }] });
    const two = threadFixture({ id: 'T2', comments: [{ author: 'ada', body: 'Why?' }, { author: 'bob', body: 'a' }, { author: 'eve', body: 'b' }] });
    await harness.readAll({ diff, threads: threadsFixture([one, two]), comments: commentsFixture({ listed: [threadInDiff(one, onLine(0)), threadInDiff(two, onLine(0))] }) });
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByText('1 reply · bob')).toBeInTheDocument();
    expect(screen.getByText('2 replies · eve')).toBeInTheDocument();
    expect(screen.queryByText(/0 repl/)).not.toBeInTheDocument();
  });

  it('keeps only the first line of a multi-line first comment', async () => {
    const harness = new Harness();
    const long = threadFixture({ comments: [{ author: 'ada', body: 'First\nSecond' }] });
    await harness.readAll({ diff, threads: threadsFixture([long]), comments: commentsFixture({ listed: [threadInDiff(long, onLine(0))] }) });
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByText('First')).toBeInTheDocument();
    expect(screen.queryByText(/Second/)).not.toBeInTheDocument();
  });

  it('dims a resolved thread', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getAllByRole('button')[2]!.className).toContain('dim');
    expect(screen.getAllByRole('button')[1]!.className).not.toContain('dim');
  });

  it('says it is reading while the threads have not answered', () => {
    const harness = new Harness();
    harness.review.read();
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the threads');
  });

  it('says why the threads cannot be read', async () => {
    const harness = new Harness();
    harness.review.read();
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    await harness.answer('review/threadsOf', { kind: 'unavailableThreads', reason: 'rate limited' });
    expect(screen.getByText('threads unknown (rate limited)')).toBeInTheDocument();
  });

  it('says there are no threads', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    expect(screen.getByText('no threads')).toBeInTheDocument();
  });

  it('selects an entry and goes to its line by one click, the diff unfocused or not', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane review={harness.review} focused={false} onFocus={() => {}} />);
    await userEvent.click(screen.getAllByRole('button')[2]!);
    expect(harness.review.selectedComment()).toBe(2);
    expect(harness.review.cursor()).toBe(1);
  });

  it('opens a pending comment in the box by a second click on its row', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    await userEvent.click(screen.getAllByRole('button')[1]!);
    await userEvent.click(screen.getAllByRole('button')[0]!);
    expect(harness.review.cursor()).toBe(2);
    expect(harness.review.box()).toBeUndefined();
    await userEvent.click(screen.getAllByRole('button')[0]!);
    expect(harness.review.box()).toBeDefined();
  });

  it('selects an outdated thread and leaves the diff where it was', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<CommentsPane review={harness.review} focused onFocus={() => {}} />);
    await userEvent.click(screen.getAllByRole('button')[3]!);
    expect(harness.review.selectedComment()).toBe(3);
    expect(harness.review.cursor()).toBe(0);
  });
});

describe('the Review panel', () => {
  it('says it is reading while the pending review has not answered', () => {
    const harness = new Harness();
    harness.review.read();
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the pending review');
  });

  it('says there is no pending review', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('no pending review')).toBeInTheDocument();
  });

  it('says why the pending review is unknown', async () => {
    const harness = new Harness();
    harness.review.read();
    render(<ReviewPanel review={harness.review} />);
    await harness.answer('review/pendingOf', { kind: 'unavailablePendingReview', reason: 'no scope' });
    expect(screen.getByText('pending unknown (no scope)')).toBeInTheDocument();
  });

  it('counts the pending comments under the first line of the summary, or says there is no summary', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([pendingCommentFixture(), pendingCommentFixture({ id: 'C2' })], { summary: 'Mostly fine\nbut see below' }), summary: 'Mostly fine\nbut see below' });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('Mostly fine')).toBeInTheDocument();
    expect(screen.getByText('2 pending')).toBeInTheDocument();
    expect(screen.queryByText(/see below/)).not.toBeInTheDocument();
  });

  it('says there is no summary when review says a submit would send none', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([]) });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('no summary')).toBeInTheDocument();
    expect(screen.getByText('0 pending')).toBeInTheDocument();
  });

  it('warns when the branch moved since the review was written', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([], { headMove: { kind: 'movedHead', writtenOn: 'abcdef0123456789', head: '1234567890abcdef' } }) });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('written on abcdef0,')).toBeInTheDocument();
    expect(screen.getByText('head is now 1234567')).toBeInTheDocument();
  });

  it('shows the summary typed for the submit even before any comment is written', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewPanel review={harness.review} />);
    await harness.openSummary();
    act(() => {
      harness.review.typeInBox('Typed first');
      harness.review.saveBox();
    });
    await harness.answerPlacement({ summary: 'Typed first' });
    expect(screen.getByText('Typed first')).toBeInTheDocument();
    expect(screen.getByText('0 pending')).toBeInTheDocument();
  });
});

const anchored = { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 1 } };

const openedBox = async (harness: Harness) => {
  await harness.readAll({ diff });
  const opening = harness.review.comment();
  harness.client.answerNext(anchored);
  await act(async () => {
    await opening;
  });
};

describe('the comment box of a review', () => {
  it('shows nothing while no box is open', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewDialogs review={harness.review} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('is a popup titled for a pending comment, holding what is typed', async () => {
    const harness = new Harness();
    render(<ReviewDialogs review={harness.review} />);
    await openedBox(harness);
    expect(screen.getByRole('dialog', { name: 'Pending comment' })).toBeInTheDocument();
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    expect(harness.review.box()?.text).toBe('Rename this');
  });

  it('saves with ctrl+s and says it is saving, holding the text still', async () => {
    const harness = new Harness();
    render(<ReviewDialogs review={harness.review} />);
    await openedBox(harness);
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    await userEvent.keyboard('{Control>}s{/Control}');
    expect(harness.client.calls.at(-1)?.method).toBe('review/comment');
    expect(screen.getByText('saving…')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Comment text' })).toHaveAttribute('readonly');
  });

  it('says why a comment was not saved, keeping the text to save again', async () => {
    const harness = new Harness();
    render(<ReviewDialogs review={harness.review} />);
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
    render(<ReviewDialogs review={harness.review} />);
    await openedBox(harness);
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(harness.review.box()).toBeUndefined();
  });

  it('is thrown away, as by escape, by a press outside it', async () => {
    const harness = new Harness();
    render(<ReviewDialogs review={harness.review} />);
    await openedBox(harness);
    await userEvent.type(screen.getByRole('textbox', { name: 'Comment text' }), 'Rename this');
    await userEvent.click(screen.getByRole('dialog').parentElement!);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(harness.review.box()).toBeUndefined();
  });

  it('stays open for a press inside it, even one let go outside it', async () => {
    const harness = new Harness();
    render(<ReviewDialogs review={harness.review} />);
    await openedBox(harness);
    const user = userEvent.setup();
    await user.pointer([{ keys: '[MouseLeft>]', target: screen.getByRole('textbox', { name: 'Comment text' }) }, { target: screen.getByRole('dialog').parentElement! }, { keys: '[/MouseLeft]' }]);
    expect(screen.getByRole('dialog', { name: 'Pending comment' }), 'a selection dragged out of the box is not a press outside it').toBeInTheDocument();
  });

  it('shows the thread a reply answers above the box, titled with its first line', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1', comments: [{ author: 'ada', body: 'Why this?\nReally.' }, { author: 'bob', body: 'Speed' }] });
    await harness.readAll({ diff, threads: threadsFixture([thread]), comments: commentsFixture({ threads: [threadInDiff(thread, onLine(0))], listed: [threadInDiff(thread, onLine(0))] }) });
    render(<ReviewDialogs review={harness.review} />);
    act(() => harness.review.commentOnSelectedComment());
    expect(screen.getByRole('dialog', { name: 'Why this?' })).toBeInTheDocument();
    expect(screen.getByText(/ada\s+Why this\?/)).toBeInTheDocument();
    expect(screen.getByText(/bob\s+Speed/)).toBeInTheDocument();
  });

  it('is titled Summary for the summary, and keeps it for the submit', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewDialogs review={harness.review} />);
    await harness.openSummary('From github.com');
    expect(screen.getByRole('dialog', { name: 'Summary' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Comment text' })).toHaveValue('From github.com');
  });
});

const confirming = async () => {
  const harness = new Harness();
  await harness.readAll({ diff });
  render(<ReviewDialogs review={harness.review} />);
  await harness.openSummary();
  act(() => harness.review.saveBox());
  await harness.answerPlacement();
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
    await harness.answerPlacement();
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
