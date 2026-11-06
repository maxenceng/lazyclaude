import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { RowKind } from './diffRows';
import { added, diffFixture, fileFixture, hunk, kept, noLine, onLine, openPending, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff, threadsFixture } from './fixtures';
import { Harness, type Reads } from './reviewHarness';
import { DiffStateKind } from './diffState';

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const readTo = (harness: Harness, overrides: Omit<Reads, 'diff'> = {}) => harness.readAll({ ...overrides, diff });

describe('a review being read', () => {
  it('faults the diff with its retry when placing the threads fails, keeping the rows of the diff read', async () => {
    const harness = new Harness();
    harness.review.read();
    await harness.answer('review/pendingOf', { kind: 'noPendingReview' });
    await harness.answer('review/threadsOf', threadsFixture([]));
    await harness.answer('review/diffOf', diff);
    await harness.fail('review/threadsInDiff', new RpcError(-32000, 'sidecar gone'));
    await harness.answer('review/pendingInDiff', []);
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Fault);
    expect(harness.review.rows().lineCount()).toBe(4);
    expect(harness.refusals).toEqual([]);
  });

  it('asks for the pending review, the diff and the threads of the pull request, and is loading until they answer', () => {
    const harness = new Harness();
    harness.review.read();
    expect(harness.client.calls).toEqual([
      { method: 'review/pendingOf', params: { root: '/p', number: 7 } },
      { method: 'review/diffOf', params: { root: '/p', number: 7 } },
      { method: 'review/threadsOf', params: { root: '/p', number: 7 } },
    ]);
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Loading);
    expect(harness.review.threads.kind).toBe('loadingThreads');
    expect(harness.review.pending.kind).toBe('loadingPendingReview');
  });

  it('places the threads and the pending comments on the diff the sidecar answered, and shows them under their lines', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    const comment = pendingCommentFixture();
    const threads = threadsFixture([thread]);
    const pending = openPending([comment]);
    await readTo(harness, { threads, pending, placedThreads: [threadInDiff(thread, onLine(1))], placedPending: [pendingInDiff(comment, onLine(2))] });
    expect(harness.client.calls.slice(3)).toEqual([
      { method: 'review/threadsInDiff', params: { diff, threads } },
      { method: 'review/pendingInDiff', params: { diff, pending } },
    ]);
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Populated);
    expect(harness.review.rows().all().map((row) => row.kind)).toEqual([
      RowKind.File,
      RowKind.Hunk,
      RowKind.Line,
      RowKind.Line,
      RowKind.Thread,
      RowKind.Line,
      RowKind.Pending,
      RowKind.File,
      RowKind.Hunk,
      RowKind.Line,
    ]);
  });

  it('lists the pending comments before the threads in Comments, and a thread on no line last among them', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    const comment = pendingCommentFixture();
    await readTo(harness, {
      threads: threadsFixture([thread]),
      pending: openPending([comment]),
      placedThreads: [threadInDiff(thread, noLine)],
      placedPending: [pendingInDiff(comment, onLine(0))],
    });
    expect(harness.review.entries().map((entry) => entry.kind)).toEqual(['pending', 'thread']);
  });

  it('takes only the answer to the latest read of each kind', async () => {
    const harness = new Harness();
    harness.review.read();
    harness.review.read();
    await harness.answer('review/diffOf', diff);
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Loading);
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'newer', tooLarge: false });
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Unavailable);
  });

  it('is unavailable with the reason when the sidecar says the diff cannot be read, and says when it is too large', async () => {
    const harness = new Harness();
    harness.review.read();
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'diff too big', tooLarge: true });
    expect(harness.review.diffState()).toMatchObject({ kind: DiffStateKind.Unavailable, reason: 'diff too big', tooLarge: true });
  });

  it('is empty when the diff lists no file', async () => {
    const harness = new Harness();
    harness.review.read();
    await harness.answer('review/diffOf', diffFixture([]));
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Empty);
  });

  it('is a fault when the diff read itself fails, and reads everything again, loading, on retry', async () => {
    const harness = new Harness();
    harness.review.read();
    await harness.fail('review/diffOf', new RpcError(-32000, 'sidecar gone'));
    expect(harness.review.diffState()).toMatchObject({ kind: DiffStateKind.Fault, message: 'sidecar gone' });
    harness.review.read();
    expect(harness.review.diffState().kind).toBe(DiffStateKind.Loading);
  });

  it('shows threads and the pending review as unknown, with the reason, when their reads fail', async () => {
    const harness = new Harness();
    harness.review.read();
    await harness.fail('review/threadsOf', new RpcError(-32000, 'no threads for you'));
    await harness.fail('review/pendingOf', new RpcError(-32000, 'no pending for you'));
    expect(harness.review.threads).toEqual({ kind: 'unavailableThreads', reason: 'no threads for you' });
    expect(harness.review.pending).toEqual({ kind: 'unavailablePendingReview', reason: 'no pending for you' });
  });
});

describe('the cursor of a review', () => {
  it('stands on a diff line, held within the lines there are, and takes the file of its line as the selected file', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { review } = harness;
    review.moveLine(-1);
    expect(review.cursor()).toBe(0);
    review.moveLine(2);
    expect(review.cursor()).toBe(2);
    expect(review.selectedFile()).toBe(0);
    review.moveLine(9);
    expect(review.cursor()).toBe(3);
    expect(review.selectedFile()).toBe(1);
  });

  it('goes to the first line of the next file and back to the previous, and nowhere past the last', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { review } = harness;
    review.nextFile();
    expect(review.cursor()).toBe(3);
    review.nextFile();
    expect(review.cursor()).toBe(3);
    review.previousFile();
    expect(review.cursor()).toBe(0);
  });

  it('goes to the first line of the selected file', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.review.selectFile(1);
    expect(harness.review.selectedFile()).toBe(1);
    harness.review.goToSelectedFile();
    expect(harness.review.cursor()).toBe(3);
  });

  it('moves half of the view at a time', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.review.observeLines(4);
    harness.review.halfPage(true);
    expect(harness.review.cursor()).toBe(2);
    harness.review.halfPage(false);
    expect(harness.review.cursor()).toBe(0);
  });
});

const anchored = { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 } };
const refusedAnchor = { kind: 'refusedAnchor', reason: 'a range cannot span two hunks' };

describe('the range of a review', () => {
  it('is marked by a drag from a line to another, asking where a comment on the lines between would hang', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.review.markRange(0, 2);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await marking;
    expect(harness.review.cursor()).toBe(2);
    expect([0, 1, 2, 3].map((line) => harness.review.inRange(line))).toEqual([true, true, true, false]);
  });

  it('is marked by a drag upward as the lines from the one released on to the one pressed on', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.review.markRange(3, 1);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 1, last: 3 } });
    harness.client.answerNext(anchored);
    await marking;
    expect(harness.review.cursor()).toBe(1);
    expect([0, 1, 2, 3, 4].map((line) => harness.review.inRange(line))).toEqual([false, true, true, true, false]);
  });

  it('is refused with the reason when it spans two hunks or two files, and keeps the range it had', async () => {
    const harness = new Harness();
    await readTo(harness);
    const first = harness.review.markRange(0, 1);
    harness.client.answerNext(anchored);
    await first;
    const across = harness.review.markRange(1, 3);
    harness.client.answerNext(refusedAnchor);
    await across;
    expect(harness.refusals).toEqual(['a range cannot span two hunks']);
    expect([0, 1, 2, 3].map((line) => harness.review.inRange(line))).toEqual([true, true, false, false]);
  });

  it('ends at the line released on even when the cursor moves while the sidecar is asked', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.review.markRange(0, 1);
    harness.review.moveLine(2);
    harness.client.answerNext(anchored);
    await marking;
    expect([0, 1, 2, 3].map((line) => harness.review.inRange(line))).toEqual([true, true, false, false]);
  });

  it('takes only the answer of the latest drag', async () => {
    const harness = new Harness();
    await readTo(harness);
    const older = harness.review.markRange(0, 1);
    const newer = harness.review.markRange(2, 3);
    harness.client.answerLast(anchored);
    await newer;
    harness.client.answerNext(anchored);
    await older;
    expect([0, 1, 2, 3].map((line) => harness.review.inRange(line))).toEqual([false, false, true, true]);
  });

  it('raises nothing for an ask answered after the review was left', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.review.markRange(1, 3);
    harness.review.close();
    harness.client.answerNext(refusedAnchor);
    await marking;
    expect(harness.refusals).toEqual([]);
  });

  it('is marked by v at the cursor and ended by v at another line, and dropped by escape', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { review } = harness;
    await review.markRangeKey();
    expect(review.inRange(0)).toBe(true);
    review.moveLine(2);
    const ending = review.markRangeKey();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await ending;
    expect(review.inRange(1)).toBe(true);
    expect(review.clearRange()).toBe(true);
    expect(review.inRange(1)).toBe(false);
    expect(review.clearRange()).toBe(false);
  });
});

describe('resolved threads of a review', () => {
  it('stay out of the diff until they are asked for, and stay listed in Comments', async () => {
    const harness = new Harness();
    const resolved = threadFixture({ id: 'T1', resolved: true, open: false });
    await readTo(harness, { threads: threadsFixture([resolved]), placedThreads: [threadInDiff(resolved, onLine(1))] });
    expect(harness.review.rows().all().some((row) => row.kind === RowKind.Thread)).toBe(false);
    expect(harness.review.entries()).toHaveLength(1);
    harness.review.toggleResolved();
    expect(harness.review.rows().all().some((row) => row.kind === RowKind.Thread)).toBe(true);
  });
});

const written = (comments = [pendingCommentFixture({ id: 'C9', body: 'Saved' })], summary = '') => ({ kind: 'writtenReview', pending: openPending(comments, { summary }) });

const withBoxOpen = async (harness: Harness, readOverrides: Omit<Reads, 'diff'> = {}) => {
  await readTo(harness, readOverrides);
  const opening = harness.review.comment();
  harness.client.answerNext(anchored);
  await opening;
};

describe('the comment box of a review', () => {
  it('opens on the line of the cursor once the sidecar says a comment can hang there', async () => {
    const harness = new Harness();
    await readTo(harness);
    const opening = harness.review.comment();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 0 } });
    harness.client.answerNext(anchored);
    await opening;
    expect(harness.review.box()).toMatchObject({ text: '', saving: false });
  });

  it('opens on the range marked', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.review.markRange(0, 2);
    harness.client.answerNext(anchored);
    await marking;
    const opening = harness.review.comment();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await opening;
    expect(harness.review.box()).toBeDefined();
  });

  it('does not open, and says why, when the line cannot take a comment', async () => {
    const harness = new Harness();
    await readTo(harness);
    const opening = harness.review.comment();
    harness.client.answerNext(refusedAnchor);
    await opening;
    expect(harness.review.box()).toBeUndefined();
    expect(harness.refusals).toEqual(['a range cannot span two hunks']);
  });

  it('writes the comment to the pending review, holds still while saving, and closes with the pending review the sidecar answers', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.typeInBox('Rename this');
    harness.review.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({
      method: 'review/comment',
      params: { root: '/p', number: 7, path: 'src/a.ts', position: anchored.position, body: 'Rename this' },
    });
    expect(harness.review.box()?.saving).toBe(true);
    const saved = written();
    await harness.answer('review/comment', saved);
    expect(harness.review.box()).toBeUndefined();
    expect(harness.review.pending).toEqual(saved.pending);
    expect(harness.methods().at(-1)).toBe('review/pendingInDiff');
  });

  it('keeps the text and says why it was not saved when GitHub refuses the write', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.typeInBox('Rename this');
    harness.review.saveBox();
    await harness.answer('review/comment', { kind: 'refusedWrite', reason: 'GitHub is down' });
    expect(harness.review.box()).toMatchObject({ text: 'Rename this', saving: false, notSaved: 'GitHub is down' });
  });

  it('closes and reads the pending review again when the call itself fails, offering no save that could write twice', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.typeInBox('Rename this');
    harness.review.saveBox();
    await harness.fail('review/comment', new RpcError(-32000, 'sidecar gone'));
    expect(harness.review.box()).toBeUndefined();
    expect(harness.refusals).toEqual(['not known to be written (sidecar gone): reading the pending review again']);
    expect(harness.methods().at(-1)).toBe('review/pendingOf');
  });

  it('stays open while its save is out, so what GitHub answers is shown on it', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.typeInBox('Rename this');
    harness.review.saveBox();
    harness.review.cancelBox();
    expect(harness.review.box()).toBeDefined();
    await harness.answer('review/comment', { kind: 'refusedWrite', reason: 'GitHub is down' });
    expect(harness.review.box()).toMatchObject({ text: 'Rename this', notSaved: 'GitHub is down' });
  });

  it('is not replaced by a comment asked for before another box opened', async () => {
    const harness = new Harness();
    await readTo(harness);
    const opening = harness.review.comment();
    harness.review.openSummary();
    harness.client.answerNext(anchored);
    await opening;
    expect(harness.review.box()?.target.kind).toBe('summary');
  });

  it('is thrown away, with the range it was written on, by cancelling', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.cancelBox();
    expect(harness.review.box()).toBeUndefined();
    expect(harness.review.clearRange()).toBe(false);
  });

  it('takes no text while a save is out', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.review.typeInBox('first');
    harness.review.saveBox();
    harness.review.typeInBox('second');
    expect(harness.review.box()?.text).toBe('first');
  });

  it('writes one after the other, the second only once the first is answered', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C1' });
    await readTo(harness, { pending: openPending([comment]), placedPending: [pendingInDiff(comment, onLine(0))] });
    harness.review.deleteAtCursor();
    harness.review.deleteAtCursor();
    await harness.answer('review/delete', written([]));
    expect(harness.methods().filter((method) => method === 'review/delete')).toHaveLength(2);
  });
});

const readWithComment = async (harness: Harness, comment: ReturnType<typeof pendingCommentFixture>) => {
  await readTo(harness, { pending: openPending([comment]), placedPending: [pendingInDiff(comment, onLine(0))] });
};

const readWithThread = async (harness: Harness, thread: ReturnType<typeof threadFixture>) => {
  await readTo(harness, { threads: threadsFixture([thread]), placedThreads: [threadInDiff(thread, onLine(2))] });
};

describe('the pending comments of a review', () => {
  const comment = pendingCommentFixture({ id: 'C1', body: 'Rename this' });

  it('are opened in the box with their text by enter on their line, and saved as an edit', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.review.enterInDiff();
    expect(harness.review.box()).toMatchObject({ text: 'Rename this' });
    harness.review.typeInBox('Rename that');
    harness.review.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/edit', params: { root: '/p', number: 7, comment: 'C1', body: 'Rename that' } });
  });

  it('are deleted from the cursor line, and the pending review the sidecar answers replaces what was read', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.review.deleteAtCursor();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/delete', params: { root: '/p', number: 7, comment: 'C1' } });
    await harness.answer('review/delete', written([]));
    expect(harness.review.pending).toEqual(written([]).pending);
  });

  it('are not deleted, and the notice says so, when GitHub refuses', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.review.deleteAtCursor();
    await harness.answer('review/delete', { kind: 'refusedWrite', reason: 'already gone' });
    expect(harness.refusals).toEqual(['not deleted (already gone)']);
  });

  it('are deleted from Comments when selected there, after going to their line', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.review.moveLine(2);
    harness.review.deleteSelectedComment();
    expect(harness.client.calls.at(-1)).toMatchObject({ method: 'review/delete' });
    expect(harness.review.cursor()).toBe(0);
  });
});

describe('the threads of a review', () => {
  const thread = threadFixture({ id: 'T1', comments: [{ author: 'ada', body: 'Why this?' }] });

  it('are replied to from Comments in the box, saved as a reply to the thread', async () => {
    const harness = new Harness();
    await readWithThread(harness, thread);
    harness.review.commentOnSelectedComment();
    expect(harness.review.box()).toMatchObject({ text: '' });
    harness.review.typeInBox('Because');
    harness.review.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/reply', params: { root: '/p', number: 7, thread: 'T1', body: 'Because' } });
  });

  it('are gone to by enter on their entry in Comments', async () => {
    const harness = new Harness();
    await readWithThread(harness, thread);
    harness.review.enterOnComment();
    expect(harness.review.cursor()).toBe(2);
  });
});

describe('the summary of a review', () => {
  it('is typed in the box, kept without writing, and then confirmed before it submits the review as a Comment', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.review.openSummary();
    harness.review.typeInBox('Looks good');
    harness.review.saveBox();
    expect(harness.client.calls).toHaveLength(callsBefore);
    expect(harness.review.box()).toBeUndefined();
    expect(harness.review.submitPrompt()).toBe(true);
    harness.review.answerSubmit(true);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/submit', params: { root: '/p', number: 7, summary: 'Looks good' } });
    await harness.answer('review/submit', written());
    expect(harness.submitted).toBe(1);
  });

  it('starts from the body of the pending review it adopted', async () => {
    const harness = new Harness();
    await readTo(harness, { pending: openPending([], { summary: 'From github.com' }) });
    harness.review.openSummary();
    expect(harness.review.box()).toMatchObject({ text: 'From github.com' });
  });

  it('leaves the review pending, writing nothing, when the confirm is answered any other way', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.review.openSummary();
    harness.review.saveBox();
    harness.review.answerSubmit(false);
    expect(harness.review.submitPrompt()).toBe(false);
    expect(harness.client.calls).toHaveLength(callsBefore);
    expect(harness.submitted).toBe(0);
  });

  it('is not submitted, and the notice says so, when GitHub refuses', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.review.openSummary();
    harness.review.saveBox();
    harness.review.answerSubmit(true);
    await harness.answer('review/submit', { kind: 'refusedWrite', reason: 'nothing to submit' });
    expect(harness.refusals).toEqual(['not submitted (nothing to submit)']);
    expect(harness.submitted).toBe(0);
  });

  it('drops the answer of a write for a review since closed', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.review.openSummary();
    harness.review.saveBox();
    harness.review.answerSubmit(true);
    harness.review.close();
    await harness.answer('review/submit', written());
    expect(harness.submitted).toBe(0);
  });
});

describe('the mouse over a review', () => {
  it('puts the cursor on the line pressed, and a second press on it, in the focused diff, is enter', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C1', body: 'Rename this' });
    await readWithComment(harness, comment);
    harness.review.moveLine(2);
    harness.review.pressLine(0, true);
    expect(harness.review.cursor()).toBe(0);
    expect(harness.review.box()).toBeUndefined();
    void harness.review.releaseLine(0);
    harness.review.pressLine(0, true);
    expect(harness.review.box()).toMatchObject({ text: 'Rename this' });
  });

  it('takes a press on an unfocused diff for the first of two, even on the cursor line', async () => {
    const harness = new Harness();
    await readWithComment(harness, pendingCommentFixture({ id: 'C1' }));
    harness.review.pressLine(0, false);
    expect(harness.review.box()).toBeUndefined();
  });

  it('marks the range from the line pressed to the line released on, and asks nothing for a press and release on the same line', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.review.pressLine(1, true);
    void harness.review.releaseLine(1);
    expect(harness.client.calls).toHaveLength(callsBefore);
    harness.review.pressLine(2, true);
    const marking = harness.review.releaseLine(0);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await marking;
    expect([0, 1, 2, 3].map((line) => harness.review.inRange(line))).toEqual([true, true, true, false]);
  });

  it('ends a drag at the next press, so a release the window never reported does not capture the next drag', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.review.pressLine(0, true);
    harness.review.pressLine(3, true);
    void harness.review.releaseLine(3);
    expect(harness.client.calls).toHaveLength(callsBefore);
  });

  it('selects a file by a press, and a second press on the selected file goes to its first line', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.review.pressFile(1, false);
    expect(harness.review.selectedFile()).toBe(1);
    expect(harness.review.cursor()).toBe(0);
    harness.review.pressFile(1, true);
    expect(harness.review.cursor()).toBe(3);
  });

  it('selects a comment by a press, and a second press on the selected one goes to its line', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    await readWithThread(harness, thread);
    harness.review.pressComment(0, false);
    expect(harness.review.cursor()).toBe(0);
    harness.review.pressComment(0, true);
    expect(harness.review.cursor()).toBe(2);
  });
});
