import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { RpcError } from '../protocol';
import { ruleOf, stylesheet } from '../design-system/stylesheet';
import { added, commentsFixture, diffFixture, fileFixture, hunk, kept, onLine, placed, openPending, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff, threadsFixture } from './fixtures';
import { DiffPane, FilesPane, ReviewDialogs, ReviewPanel } from './ReviewScreen';
import { Harness, settle } from './reviewHarness';

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const lineElements = (): HTMLElement[] => [...document.querySelectorAll<HTMLElement>('[data-line]')];

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

  it('shows each file under a rule with its hunks and their lines behind one gutter of numbers, the old number on a removed line and a sign saying what changed', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    expect(screen.getByRole('heading', { name: /src\/a\.ts/ })).toBeInTheDocument();
    expect(screen.getByText('@@ -1,2 +1,3 @@')).toBeInTheDocument();
    const lines = lineElements();
    expect(lines.map((line) => line.textContent)).toEqual(['1 one', '2-two', '2+deux', '1+b']);
    expect(within(lines[1]!).getByText('-')).toBeInTheDocument();
    expect(within(lines[2]!).getByText('+')).toBeInTheDocument();
  });

  it('marks the cursor line as current', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    act(() => harness.review.moveLine(2));
    expect(lineElements()[2]).toHaveAttribute('aria-current', 'true');
    expect(lineElements()[0]).not.toHaveAttribute('aria-current');
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
    expect(articles[0]).toHaveTextContent('a.ts:1–2');
    expect(articles[0]).toHaveTextContent('open · 2 comments');
    expect(articles[0]).toHaveTextContent('adaWhy this?');
    expect(articles[0]).toHaveTextContent('bobSpeed');
    expect(articles[1]).toHaveTextContent('a.ts:1');
    expect(articles[1]).toHaveTextContent('pending · not submitted');
    expect(articles[1]).toHaveTextContent('youRename this');
  });

  it('marks the line a thread hangs from, by a glyph and not by colour alone', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    await harness.readAll({ diff, threads: threadsFixture([thread]), comments: commentsFixture({ threads: [threadInDiff(thread, onLine(1))], underLines: [threadInDiff(thread, onLine(1))] }) });
    renderDiff(harness);
    expect(within(lineElements()[1]!).getByRole('img', { name: 'a thread hangs from this line' })).toHaveTextContent('◆');
    expect(within(lineElements()[0]!).queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows a thread once, as a card across the diff, and no second list of it', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    await harness.readAll({ diff, threads: threadsFixture([thread]), comments: commentsFixture({ threads: [threadInDiff(thread, onLine(0))], underLines: [threadInDiff(thread, onLine(0))], listed: [threadInDiff(thread, onLine(0))] }) });
    renderDiff(harness);
    expect(screen.getAllByText('Why this?')).toHaveLength(1);
  });

  it('shows a thread with no line at the top of its file, under the file header and before the first hunk, marked outdated', async () => {
    const harness = new Harness();
    const outdated = threadFixture({ id: 'T3', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } }, open: false, comments: [{ author: 'cy', body: 'Old' }] });
    const entry = threadInDiff(outdated, { kind: 'noDiffLine' });
    await harness.readAll({ diff, threads: threadsFixture([outdated]), comments: commentsFixture({ threads: [entry], listed: [entry] }) });
    renderDiff(harness);
    const card = screen.getByRole('article');
    expect(card).toHaveTextContent('b.ts');
    expect(card).toHaveTextContent('outdated');
    expect(card).toHaveTextContent('cyOld');
    const heading = screen.getByRole('heading', { name: /src\/b\.ts/ }).closest('li')!;
    expect(heading.nextElementSibling).toContainElement(card);
    expect(card.closest('li')!.nextElementSibling).toHaveTextContent('@@ -1 +1 @@');
  });

  it.each([
    ['one line', { kind: 'originallyPlaced', place: placed(40) }, 'outdated · was line 40'],
    ['a range', { kind: 'originallyPlaced', place: placed(40, 41) }, 'outdated · was lines 40–41'],
    ['no place', { kind: 'noOriginalPlace' }, 'outdated'],
  ] as const)('says where an outdated thread was on %s', async (_name, was, tag) => {
    const harness = new Harness();
    const outdated = threadFixture({ id: 'T4', path: 'src/b.ts', position: { kind: 'outdatedThread', was }, open: true });
    const entry = threadInDiff(outdated, { kind: 'noDiffLine' });
    await harness.readAll({ diff, threads: threadsFixture([outdated]), comments: commentsFixture({ threads: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(within(screen.getByRole('article')).getByText(tag, { exact: true })).toBeInTheDocument();
  });

  it('says where a thread was when it is placed on a line the diff no longer holds', async () => {
    const harness = new Harness();
    const moved = threadFixture({ id: 'T5', path: 'src/b.ts', position: placed(40, 41), open: true });
    const entry = threadInDiff(moved, { kind: 'noDiffLine' });
    await harness.readAll({ diff, threads: threadsFixture([moved]), comments: commentsFixture({ threads: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(screen.getByRole('article')).toHaveTextContent('outdated · was lines 40–41');
  });

  it('shows a thread once even when the diff holds no hunk of its file', async () => {
    const harness = new Harness();
    const outdated = threadFixture({ id: 'T3', path: 'src/c.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } });
    const entry = threadInDiff(outdated, { kind: 'noDiffLine' });
    await harness.readAll({ diff, threads: threadsFixture([outdated]), comments: commentsFixture({ threads: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('opens the comment box from a press on a line number', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    await userEvent.click(screen.getAllByRole('button', { name: 'Comment on line 2' })[1]!);
    expect(harness.client.calls.at(-1)).toMatchObject({ method: 'review/anchorOf', params: { first: 2, last: 2 } });
  });

  it('wraps a long line under a hanging indent and sets code at 14px or more', () => {
    const sheet = stylesheet('DiffLine.module.css');
    expect(ruleOf(sheet, '.text')).toContain('text-indent: -4ch');
    expect(ruleOf(sheet, '.text')).toContain('white-space: pre-wrap');
    expect(ruleOf(sheet, '.line')).toContain('var(--size-body)');
  });

  it('gives the cursor line a lift and an edge, never the inverse of the focused title row', () => {
    const rule = ruleOf(stylesheet('DiffLine.module.css'), '.cursor');
    expect(rule).toContain('background: var(--diff-cursor)');
    expect(rule).not.toContain('--cursor-ink');
    expect(rule).not.toContain('--selected');
  });

  it('ends a thread with a Reply, and a pending comment with an Edit and a Delete', async () => {
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
    expect(within(articles[0]!).getByRole('button', { name: 'Reply to the thread on a.ts:1' })).toHaveTextContent('Reply');
    expect(within(articles[1]!).queryByRole('button', { name: /Reply/ })).not.toBeInTheDocument();
    expect(within(articles[1]!).getByRole('button', { name: 'Edit your comment on a.ts:1' })).toBeInTheDocument();
    expect(within(articles[1]!).getByRole('button', { name: 'Delete your comment on a.ts:1' })).toBeInTheDocument();
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
    await userEvent.click(screen.getByRole('button', { name: 'Reply to the thread on a.ts:1' }));
    expect(screen.getByRole('dialog', { name: 'Why this?' })).toBeInTheDocument();
    expect(harness.review.cursor()).toBe(2);
    expect(onFocus).not.toHaveBeenCalled();
  });

  it('puts the cursor on a line pressed, focusing the diff', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const { onFocus } = renderDiff(harness, false);
    fireEvent.mouseDown(lineElements()[2]!);
    fireEvent.mouseUp(lineElements()[2]!);
    expect(onFocus).toHaveBeenCalled();
    expect(harness.review.cursor()).toBe(2);
  });

  it('marks the range dragged from a line to the line the pointer is released on', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(lineElements()[0]!);
    fireEvent.mouseUp(lineElements()[2]!);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    await act(async () => {
      harness.client.answerNext({ kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 } });
      await settle();
    });
    expect(lineElements().slice(0, 3).every((line) => line.className.includes('range'))).toBe(true);
  });

  it('takes no press of a button other than the left one', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(lineElements()[2]!, { button: 2 });
    expect(harness.review.cursor()).toBe(0);
  });

  it('ends a drag at a press off the diff, so a release the window never saw marks nothing later', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    const callsBefore = harness.client.calls.length;
    fireEvent.mouseDown(lineElements()[0]!);
    fireEvent.mouseDown(document.body);
    fireEvent.mouseUp(lineElements()[2]!);
    expect(harness.client.calls).toHaveLength(callsBefore);
  });

  it('marks a range dragged upward, released on a line above the one pressed', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    renderDiff(harness);
    fireEvent.mouseDown(lineElements()[3]!);
    fireEvent.mouseUp(lineElements()[1]!);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 1, last: 3 } });
  });
});

const withThreads = async (harness: Harness) => {
  const open = threadFixture({ id: 'T1', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 }, comments: [{ author: 'ada', body: 'Why?' }] });
  const left = threadFixture({ id: 'T2', path: 'src/a.ts', position: { kind: 'placedThread', side: 'LEFT', first: 2, last: 2 }, resolved: true, open: false, comments: [{ author: 'bob', body: 'Gone' }] });
  const outdated = threadFixture({ id: 'T3', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } }, open: false, comments: [{ author: 'cy', body: 'Old' }] });
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

const fileRows = (): HTMLElement[] => [...document.querySelectorAll<HTMLElement>('[data-row] button')];

describe('the Files of a review', () => {
  it('lists each file with its letter, name, directory and line counts, and word badges for its open threads and pending comments', async () => {
    const harness = new Harness();
    await withThreads(harness);
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    const rows = fileRows();
    expect(rows[0]).toHaveTextContent('Ma.ts+1 −1src/1 thread1 pending');
    expect(rows[1]).toHaveTextContent('Mb.ts+1 −0src/');
    expect(rows[1]).not.toHaveTextContent('thread');
    expect(rows[1]).not.toHaveTextContent('pending');
  });

  it('counts a thread with no line among the threads of its file', async () => {
    const harness = new Harness();
    const outdated = threadFixture({ id: 'T3', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } }, open: true });
    const entry = threadInDiff(outdated, { kind: 'noDiffLine' });
    await harness.readAll({ diff, threads: threadsFixture([outdated]), comments: commentsFixture({ threads: [entry], listed: [entry] }) });
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    expect(fileRows()[1]).toHaveTextContent('1 thread');
  });

  it('names a renamed file by its old and new names', async () => {
    const harness = new Harness();
    await harness.readAll({ diff: diffFixture([fileFixture('src/new.ts', [], { kind: 'renamedFile', from: 'lib/old.ts' })]) });
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    expect(fileRows()[0]).toHaveTextContent('Rold.ts → new.ts');
  });

  it('marks the selected file, and a press selects one and goes to its first line', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    const onFocus = vi.fn();
    render(<FilesPane review={harness.review} focused={false} onFocus={onFocus} />);
    expect(fileRows()[0]).toHaveAttribute('aria-current', 'true');
    await userEvent.click(fileRows()[1]!);
    expect(harness.review.selectedFile()).toBe(1);
    expect(harness.review.cursor(), 'one press goes, even on a pane that had no focus').toBe(3);
    expect(onFocus).toHaveBeenCalled();
  });

  it('lists nothing until the diff is read', () => {
    const harness = new Harness();
    harness.review.read();
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    expect(fileRows()).toHaveLength(0);
    expect(screen.getAllByRole('status')[0]).toHaveTextContent('Reading the diff');
  });

  it('carries the pending review at its foot, beside a Submit button', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([pendingCommentFixture()]) });
    render(<FilesPane review={harness.review} focused onFocus={() => {}} />);
    const foot = screen.getByRole('region', { name: 'Review' });
    expect(foot).toHaveTextContent('pending review');
    expect(within(foot).getByRole('button', { name: 'Submit review' })).toBeInTheDocument();
  });
});

describe('the Review panel', () => {
  it('says it is reading while the pending review has not answered', () => {
    const harness = new Harness();
    harness.review.read();
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByRole('status')).toHaveTextContent('Reading the pending review');
  });

  it('says there is no pending review, and still offers to submit', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getAllByText('no pending review').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Submit review' })).toBeInTheDocument();
  });

  it('says why the pending review is unknown', async () => {
    const harness = new Harness();
    harness.review.read();
    render(<ReviewPanel review={harness.review} />);
    await harness.answer('review/pendingOf', { kind: 'unavailablePendingReview', reason: 'no scope' });
    expect(screen.getByText('pending unknown (no scope)')).toBeInTheDocument();
  });

  it('badges the pending review and counts its comments with the first line of the summary', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([pendingCommentFixture(), pendingCommentFixture({ id: 'C2' })], { summary: 'Mostly fine\nbut see below' }), summary: 'Mostly fine\nbut see below' });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('pending review')).toBeInTheDocument();
    expect(screen.getByText(/2 comments · “Mostly fine”/)).toBeInTheDocument();
    expect(screen.queryByText(/see below/)).not.toBeInTheDocument();
  });

  it('counts none and says nothing of a summary when review says a submit would send none', async () => {
    const harness = new Harness();
    await harness.readAll({ diff, pending: openPending([]) });
    render(<ReviewPanel review={harness.review} />);
    expect(screen.getByText('0 comments')).toBeInTheDocument();
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
    expect(screen.getByText(/0 comments · “Typed first”/)).toBeInTheDocument();
  });

  it('submits on its button as S does, asking for the summary first', async () => {
    const harness = new Harness();
    await harness.readAll({ diff });
    render(
      <>
        <ReviewPanel review={harness.review} />
        <ReviewDialogs review={harness.review} />
      </>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Submit review' }));
    expect(harness.methods().at(-1)).toBe('review/summaryToSubmit');
    await act(async () => {
      harness.client.answer('review/summaryToSubmit', '');
      await settle();
    });
    expect(screen.getByRole('dialog', { name: 'Summary' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Submit review' })).toHaveAttribute('aria-keyshortcuts', 'S');
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
    act(() => harness.review.replyTo(thread));
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

describe('a pending comment with no line', () => {
  it.each([
    ['one line', placed(40), 'pending · outdated · was line 40'],
    ['a range', placed(40, 41), 'pending · outdated · was lines 40–41'],
  ] as const)('says where it was on %s', async (_name, position, tag) => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C7', anchor: { kind: 'onDiffAnchor', path: 'src/b.ts', position } });
    const entry = pendingInDiff(comment, { kind: 'noDiffLine' });
    await harness.readAll({ diff, pending: openPending([comment]), comments: commentsFixture({ pending: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(within(screen.getByRole('article')).getByText(tag, { exact: true })).toBeInTheDocument();
  });

  it('says where a reply was when its thread went outdated', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C9', anchor: { kind: 'replyAnchor', path: 'src/b.ts', thread: 'T1', position: { kind: 'outdatedThread', was: { kind: 'originallyPlaced', place: placed(7) } } } });
    const entry = pendingInDiff(comment, { kind: 'noDiffLine' });
    await harness.readAll({ diff, pending: openPending([comment]), comments: commentsFixture({ pending: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(within(screen.getByRole('article')).getByText('pending · outdated · was line 7', { exact: true })).toBeInTheDocument();
  });

  it('says only that it is outdated when its placement carries no line', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C8', anchor: { kind: 'replyAnchor', path: 'src/b.ts', thread: 'T1', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } } });
    const entry = pendingInDiff(comment, { kind: 'noDiffLine' });
    await harness.readAll({ diff, pending: openPending([comment]), comments: commentsFixture({ pending: [entry], listed: [entry] }) });
    renderDiff(harness);
    expect(within(screen.getByRole('article')).getByText('pending · outdated', { exact: true })).toBeInTheDocument();
  });
});
