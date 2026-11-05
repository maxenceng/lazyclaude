import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { RowKind } from './diffRows';
import { added, diffFixture, fileFixture, hunk, kept, noLine, onLine, openPending, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff, threadsFixture } from './fixtures';
import { Harness, type Reads } from './sessionHarness';
import { DiffStateKind } from './diffState';

const diff = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

const readTo = (harness: Harness, overrides: Omit<Reads, 'diff'> = {}) => harness.readAll({ ...overrides, diff });

describe('a review being read', () => {
  it('asks for the pending review, the diff and the threads of the pull request, and is loading until they answer', () => {
    const harness = new Harness();
    harness.session.read();
    expect(harness.client.calls).toEqual([
      { method: 'review/pendingOf', params: { root: '/p', number: 7 } },
      { method: 'review/diffOf', params: { root: '/p', number: 7 } },
      { method: 'review/threadsOf', params: { root: '/p', number: 7 } },
    ]);
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Loading);
    expect(harness.session.threads.kind).toBe('loadingThreads');
    expect(harness.session.pending.kind).toBe('loadingPendingReview');
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
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Populated);
    expect(harness.session.rows().all().map((row) => row.kind)).toEqual([
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
    expect(harness.session.entries().map((entry) => entry.kind)).toEqual(['pending', 'thread']);
  });

  it('takes only the answer to the latest read of each kind', async () => {
    const harness = new Harness();
    harness.session.read();
    harness.session.read();
    await harness.answer('review/diffOf', diff);
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Loading);
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'newer', tooLarge: false });
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Unavailable);
  });

  it('is unavailable with the reason when the sidecar says the diff cannot be read, and says when it is too large', async () => {
    const harness = new Harness();
    harness.session.read();
    await harness.answer('review/diffOf', { kind: 'unavailableDiff', reason: 'diff too big', tooLarge: true });
    expect(harness.session.diffState()).toMatchObject({ kind: DiffStateKind.Unavailable, reason: 'diff too big', tooLarge: true });
  });

  it('is empty when the diff lists no file', async () => {
    const harness = new Harness();
    harness.session.read();
    await harness.answer('review/diffOf', diffFixture([]));
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Empty);
  });

  it('is a fault when the diff read itself fails, and reads everything again, loading, on retry', async () => {
    const harness = new Harness();
    harness.session.read();
    await harness.fail('review/diffOf', new RpcError(-32000, 'sidecar gone'));
    expect(harness.session.diffState()).toMatchObject({ kind: DiffStateKind.Fault, message: 'sidecar gone' });
    harness.session.read();
    expect(harness.session.diffState().kind).toBe(DiffStateKind.Loading);
  });

  it('shows threads and the pending review as unknown, with the reason, when their reads fail', async () => {
    const harness = new Harness();
    harness.session.read();
    await harness.fail('review/threadsOf', new RpcError(-32000, 'no threads for you'));
    await harness.fail('review/pendingOf', new RpcError(-32000, 'no pending for you'));
    expect(harness.session.threads).toEqual({ kind: 'unavailableThreads', reason: 'no threads for you' });
    expect(harness.session.pending).toEqual({ kind: 'unavailablePendingReview', reason: 'no pending for you' });
  });
});

describe('the cursor of a review', () => {
  it('stands on a diff line, held within the lines there are, and takes the file of its line as the selected file', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { session } = harness;
    session.moveLine(-1);
    expect(session.cursor()).toBe(0);
    session.moveLine(2);
    expect(session.cursor()).toBe(2);
    expect(session.selectedFile()).toBe(0);
    session.moveLine(9);
    expect(session.cursor()).toBe(3);
    expect(session.selectedFile()).toBe(1);
  });

  it('goes to the first line of the next file and back to the previous, and nowhere past the last', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { session } = harness;
    session.nextFile();
    expect(session.cursor()).toBe(3);
    session.nextFile();
    expect(session.cursor()).toBe(3);
    session.previousFile();
    expect(session.cursor()).toBe(0);
  });

  it('goes to the first line of the selected file', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.session.selectFile(1);
    expect(harness.session.selectedFile()).toBe(1);
    harness.session.goToSelectedFile();
    expect(harness.session.cursor()).toBe(3);
  });

  it('moves half of the view at a time', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.session.observeLines(4);
    harness.session.halfPage(true);
    expect(harness.session.cursor()).toBe(2);
    harness.session.halfPage(false);
    expect(harness.session.cursor()).toBe(0);
  });
});

const anchored = { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 1, last: 2 } };
const refusedAnchor = { kind: 'refusedAnchor', reason: 'a range cannot span two hunks' };

describe('the range of a review', () => {
  it('is marked by a drag from a line to another, asking where a comment on the lines between would hang', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.session.markRange(0, 2);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await marking;
    expect(harness.session.cursor()).toBe(2);
    expect([0, 1, 2, 3].map((line) => harness.session.inRange(line))).toEqual([true, true, true, false]);
  });

  it('is marked by a drag upward as the lines from the one released on to the one pressed on', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.session.markRange(3, 1);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 1, last: 3 } });
    harness.client.answerNext(anchored);
    await marking;
    expect(harness.session.cursor()).toBe(1);
    expect([0, 1, 2, 3, 4].map((line) => harness.session.inRange(line))).toEqual([false, true, true, true, false]);
  });

  it('is refused with the reason when it spans two hunks or two files, and keeps the range it had', async () => {
    const harness = new Harness();
    await readTo(harness);
    const first = harness.session.markRange(0, 1);
    harness.client.answerNext(anchored);
    await first;
    const across = harness.session.markRange(1, 3);
    harness.client.answerNext(refusedAnchor);
    await across;
    expect(harness.refusals).toEqual(['a range cannot span two hunks']);
    expect([0, 1, 2, 3].map((line) => harness.session.inRange(line))).toEqual([true, true, false, false]);
  });

  it('is marked by v at the cursor and ended by v at another line, and dropped by escape', async () => {
    const harness = new Harness();
    await readTo(harness);
    const { session } = harness;
    await session.markRangeKey();
    expect(session.inRange(0)).toBe(true);
    session.moveLine(2);
    const ending = session.markRangeKey();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await ending;
    expect(session.inRange(1)).toBe(true);
    expect(session.clearRange()).toBe(true);
    expect(session.inRange(1)).toBe(false);
    expect(session.clearRange()).toBe(false);
  });
});

describe('resolved threads of a review', () => {
  it('stay out of the diff until they are asked for, and stay listed in Comments', async () => {
    const harness = new Harness();
    const resolved = threadFixture({ id: 'T1', resolved: true, open: false });
    await readTo(harness, { threads: threadsFixture([resolved]), placedThreads: [threadInDiff(resolved, onLine(1))] });
    expect(harness.session.rows().all().some((row) => row.kind === RowKind.Thread)).toBe(false);
    expect(harness.session.entries()).toHaveLength(1);
    harness.session.toggleResolved();
    expect(harness.session.rows().all().some((row) => row.kind === RowKind.Thread)).toBe(true);
  });
});

const written = (comments = [pendingCommentFixture({ id: 'C9', body: 'Saved' })], summary = '') => ({ kind: 'writtenReview', pending: openPending(comments, { summary }) });

const withBoxOpen = async (harness: Harness, readOverrides: Omit<Reads, 'diff'> = {}) => {
  await readTo(harness, readOverrides);
  const opening = harness.session.comment();
  harness.client.answerNext(anchored);
  await opening;
};

describe('the comment box of a review', () => {
  it('opens on the line of the cursor once the sidecar says a comment can hang there', async () => {
    const harness = new Harness();
    await readTo(harness);
    const opening = harness.session.comment();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 0 } });
    harness.client.answerNext(anchored);
    await opening;
    expect(harness.session.box()).toMatchObject({ text: '', saving: false });
  });

  it('opens on the range marked', async () => {
    const harness = new Harness();
    await readTo(harness);
    const marking = harness.session.markRange(0, 2);
    harness.client.answerNext(anchored);
    await marking;
    const opening = harness.session.comment();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await opening;
    expect(harness.session.box()).toBeDefined();
  });

  it('does not open, and says why, when the line cannot take a comment', async () => {
    const harness = new Harness();
    await readTo(harness);
    const opening = harness.session.comment();
    harness.client.answerNext(refusedAnchor);
    await opening;
    expect(harness.session.box()).toBeUndefined();
    expect(harness.refusals).toEqual(['a range cannot span two hunks']);
  });

  it('writes the comment to the pending review, holds still while saving, and closes with the pending review the sidecar answers', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.session.typeInBox('Rename this');
    harness.session.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({
      method: 'review/comment',
      params: { root: '/p', number: 7, path: 'src/a.ts', position: anchored.position, body: 'Rename this' },
    });
    expect(harness.session.box()?.saving).toBe(true);
    const saved = written();
    await harness.answer('review/comment', saved);
    expect(harness.session.box()).toBeUndefined();
    expect(harness.session.pending).toEqual(saved.pending);
    expect(harness.methods().at(-1)).toBe('review/pendingInDiff');
  });

  it('keeps the text and says why it was not saved when GitHub refuses the write', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.session.typeInBox('Rename this');
    harness.session.saveBox();
    await harness.answer('review/comment', { kind: 'refusedWrite', reason: 'GitHub is down' });
    expect(harness.session.box()).toMatchObject({ text: 'Rename this', saving: false, notSaved: 'GitHub is down' });
  });

  it('keeps the text and says why it was not saved when the call itself fails', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.session.typeInBox('Rename this');
    harness.session.saveBox();
    await harness.fail('review/comment', new RpcError(-32000, 'sidecar gone'));
    expect(harness.session.box()).toMatchObject({ text: 'Rename this', saving: false, notSaved: 'sidecar gone' });
  });

  it('is thrown away, with the range it was written on, by cancelling', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.session.cancelBox();
    expect(harness.session.box()).toBeUndefined();
    expect(harness.session.clearRange()).toBe(false);
  });

  it('takes no text while a save is out', async () => {
    const harness = new Harness();
    await withBoxOpen(harness);
    harness.session.typeInBox('first');
    harness.session.saveBox();
    harness.session.typeInBox('second');
    expect(harness.session.box()?.text).toBe('first');
  });

  it('writes one after the other, the second only once the first is answered', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C1' });
    await readTo(harness, { pending: openPending([comment]), placedPending: [pendingInDiff(comment, onLine(0))] });
    harness.session.deleteAtCursor();
    harness.session.deleteAtCursor();
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
    harness.session.enterInDiff();
    expect(harness.session.box()).toMatchObject({ text: 'Rename this' });
    harness.session.typeInBox('Rename that');
    harness.session.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/edit', params: { root: '/p', number: 7, comment: 'C1', body: 'Rename that' } });
  });

  it('are deleted from the cursor line, and the pending review the sidecar answers replaces what was read', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.session.deleteAtCursor();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/delete', params: { root: '/p', number: 7, comment: 'C1' } });
    await harness.answer('review/delete', written([]));
    expect(harness.session.pending).toEqual(written([]).pending);
  });

  it('are not deleted, and the notice says so, when GitHub refuses', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.session.deleteAtCursor();
    await harness.answer('review/delete', { kind: 'refusedWrite', reason: 'already gone' });
    expect(harness.refusals).toEqual(['not deleted (already gone)']);
  });

  it('are deleted from Comments when selected there, after going to their line', async () => {
    const harness = new Harness();
    await readWithComment(harness, comment);
    harness.session.moveLine(2);
    harness.session.deleteSelectedComment();
    expect(harness.client.calls.at(-1)).toMatchObject({ method: 'review/delete' });
    expect(harness.session.cursor()).toBe(0);
  });
});

describe('the threads of a review', () => {
  const thread = threadFixture({ id: 'T1', comments: [{ author: 'ada', body: 'Why this?' }] });

  it('are replied to from Comments in the box, saved as a reply to the thread', async () => {
    const harness = new Harness();
    await readWithThread(harness, thread);
    harness.session.commentOnSelectedComment();
    expect(harness.session.box()).toMatchObject({ text: '' });
    harness.session.typeInBox('Because');
    harness.session.saveBox();
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/reply', params: { root: '/p', number: 7, thread: 'T1', body: 'Because' } });
  });

  it('are gone to by enter on their entry in Comments', async () => {
    const harness = new Harness();
    await readWithThread(harness, thread);
    harness.session.enterOnComment();
    expect(harness.session.cursor()).toBe(2);
  });
});

describe('the summary of a review', () => {
  it('is typed in the box, kept without writing, and then confirmed before it submits the review as a Comment', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.session.openSummary();
    harness.session.typeInBox('Looks good');
    harness.session.saveBox();
    expect(harness.client.calls).toHaveLength(callsBefore);
    expect(harness.session.box()).toBeUndefined();
    expect(harness.session.submitPrompt()).toBe(true);
    harness.session.answerSubmit(true);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/submit', params: { root: '/p', number: 7, summary: 'Looks good' } });
    await harness.answer('review/submit', written());
    expect(harness.submitted).toBe(1);
  });

  it('starts from the body of the pending review it adopted', async () => {
    const harness = new Harness();
    await readTo(harness, { pending: openPending([], { summary: 'From github.com' }) });
    harness.session.openSummary();
    expect(harness.session.box()).toMatchObject({ text: 'From github.com' });
  });

  it('leaves the review pending, writing nothing, when the confirm is answered any other way', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.session.openSummary();
    harness.session.saveBox();
    harness.session.answerSubmit(false);
    expect(harness.session.submitPrompt()).toBe(false);
    expect(harness.client.calls).toHaveLength(callsBefore);
    expect(harness.submitted).toBe(0);
  });

  it('is not submitted, and the notice says so, when GitHub refuses', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.session.openSummary();
    harness.session.saveBox();
    harness.session.answerSubmit(true);
    await harness.answer('review/submit', { kind: 'refusedWrite', reason: 'nothing to submit' });
    expect(harness.refusals).toEqual(['not submitted (nothing to submit)']);
    expect(harness.submitted).toBe(0);
  });

  it('drops the answer of a write for a review since closed', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.session.openSummary();
    harness.session.saveBox();
    harness.session.answerSubmit(true);
    harness.session.close();
    await harness.answer('review/submit', written());
    expect(harness.submitted).toBe(0);
  });
});

describe('the mouse over a review', () => {
  it('puts the cursor on the line pressed, and a second press on it, in the focused diff, is enter', async () => {
    const harness = new Harness();
    const comment = pendingCommentFixture({ id: 'C1', body: 'Rename this' });
    await readWithComment(harness, comment);
    harness.session.moveLine(2);
    harness.session.pressLine(0, true);
    expect(harness.session.cursor()).toBe(0);
    expect(harness.session.box()).toBeUndefined();
    void harness.session.releaseLine(0);
    harness.session.pressLine(0, true);
    expect(harness.session.box()).toMatchObject({ text: 'Rename this' });
  });

  it('takes a press on an unfocused diff for the first of two, even on the cursor line', async () => {
    const harness = new Harness();
    await readWithComment(harness, pendingCommentFixture({ id: 'C1' }));
    harness.session.pressLine(0, false);
    expect(harness.session.box()).toBeUndefined();
  });

  it('marks the range from the line pressed to the line released on, and asks nothing for a press and release on the same line', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.session.pressLine(1, true);
    void harness.session.releaseLine(1);
    expect(harness.client.calls).toHaveLength(callsBefore);
    harness.session.pressLine(2, true);
    const marking = harness.session.releaseLine(0);
    expect(harness.client.calls.at(-1)).toEqual({ method: 'review/anchorOf', params: { diff, first: 0, last: 2 } });
    harness.client.answerNext(anchored);
    await marking;
    expect([0, 1, 2, 3].map((line) => harness.session.inRange(line))).toEqual([true, true, true, false]);
  });

  it('ends a drag at the next press, so a release the window never reported does not capture the next drag', async () => {
    const harness = new Harness();
    await readTo(harness);
    const callsBefore = harness.client.calls.length;
    harness.session.pressLine(0, true);
    harness.session.pressLine(3, true);
    void harness.session.releaseLine(3);
    expect(harness.client.calls).toHaveLength(callsBefore);
  });

  it('selects a file by a press, and a second press on the selected file goes to its first line', async () => {
    const harness = new Harness();
    await readTo(harness);
    harness.session.pressFile(1, false);
    expect(harness.session.selectedFile()).toBe(1);
    expect(harness.session.cursor()).toBe(0);
    harness.session.pressFile(1, true);
    expect(harness.session.cursor()).toBe(3);
  });

  it('selects a comment by a press, and a second press on the selected one goes to its line', async () => {
    const harness = new Harness();
    const thread = threadFixture({ id: 'T1' });
    await readWithThread(harness, thread);
    harness.session.pressComment(0, false);
    expect(harness.session.cursor()).toBe(0);
    harness.session.pressComment(0, true);
    expect(harness.session.cursor()).toBe(2);
  });
});
