import { describe, expect, it } from 'vitest';
import { DiffRows, RowKind, deltaOf } from './diffRows';
import { added, diffFixture, fileFixture, hunk, kept, onLine, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff } from './fixtures';

const twoFiles = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')]), hunk('@@ -9 +10 @@', [added(10, 'ten')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

describe('the rows of a review diff', () => {
  it('lists a rule for each file, a header for each hunk and a row for each line', () => {
    const rows = DiffRows.of(twoFiles, [], []);
    expect(rows.all().map((row) => row.kind)).toEqual([
      RowKind.File,
      RowKind.Hunk,
      RowKind.Line,
      RowKind.Line,
      RowKind.Line,
      RowKind.Hunk,
      RowKind.Line,
      RowKind.File,
      RowKind.Hunk,
      RowKind.Line,
    ]);
    expect(rows.lineCount()).toBe(5);
  });

  it('numbers the lines in the order they are listed and knows the file of each', () => {
    const rows = DiffRows.of(twoFiles, [], []);
    expect([0, 1, 2, 3, 4].map((line) => rows.fileOfLine(line))).toEqual([0, 0, 0, 0, 1]);
    expect(rows.firstLineOf(1)).toBe(4);
    expect(rows.firstLineOfNextFile(0)).toBe(4);
    expect(rows.firstLineOfNextFile(4)).toBeUndefined();
    expect(rows.firstLineOfPreviousFile(4)).toBe(0);
    expect(rows.firstLineOfPreviousFile(2)).toBeUndefined();
  });

  it('puts the threads review shows under their lines, and every pending comment, under the line they are placed on', () => {
    const open = threadInDiff(threadFixture({ id: 'open' }), onLine(1));
    const resolved = threadInDiff(threadFixture({ id: 'done', resolved: true, open: false }), onLine(1));
    const pending = pendingInDiff(pendingCommentFixture({ id: 'mine' }), onLine(1));
    const rows = DiffRows.of(twoFiles, [open, resolved], [pending]);
    expect(rows.all().map((row) => row.kind).slice(2, 7)).toEqual([RowKind.Line, RowKind.Line, RowKind.Thread, RowKind.Thread, RowKind.Pending]);
  });

  it('leaves a thread that sits on no line out of the rows under the lines', () => {
    const rows = DiffRows.of(twoFiles, [threadInDiff(threadFixture(), { kind: 'noDiffLine' })], []);
    expect(rows.all().some((row) => row.kind === RowKind.Thread)).toBe(false);
  });

  it('shows a thread with no line once, right under the header of its file and before its first hunk', () => {
    const lineless = threadInDiff(threadFixture({ id: 'old', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), { kind: 'noDiffLine' });
    const kinds = DiffRows.of(twoFiles, [], [], [lineless]).all();
    expect(kinds.map((row) => row.kind).slice(7, 10)).toEqual([RowKind.File, RowKind.Thread, RowKind.Hunk]);
    expect(kinds.filter((row) => row.kind === RowKind.Thread)).toHaveLength(1);
  });

  it('shows a pending comment with no line at the top of its file too', () => {
    const lineless = pendingInDiff(pendingCommentFixture({ id: 'mine', anchor: { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 99, last: 99 } } }), { kind: 'noDiffLine' });
    expect(DiffRows.of(twoFiles, [], [], [lineless]).all().map((row) => row.kind).slice(0, 3)).toEqual([RowKind.File, RowKind.Pending, RowKind.Hunk]);
  });

  it('says which lines a thread or a pending comment hangs from, and which cards hang from none', () => {
    const open = threadInDiff(threadFixture({ id: 'open' }), onLine(1));
    const pending = pendingInDiff(pendingCommentFixture({ id: 'mine' }), onLine(3));
    const lineless = threadInDiff(threadFixture({ id: 'old', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), { kind: 'noDiffLine' });
    const rows = DiffRows.of(twoFiles, [open], [pending], [lineless]);
    expect([0, 1, 2, 3].map((line) => [rows.hasThread(line), rows.hasPending(line)])).toEqual([
      [false, false],
      [true, false],
      [false, false],
      [false, true],
    ]);
    expect(rows.all().filter((row) => row.kind === RowKind.Thread).map((row) => row.lineless)).toEqual([false, true]);
  });

  it('drops a thread with no line whose file the diff does not hold', () => {
    const lineless = threadInDiff(threadFixture({ path: 'gone.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), { kind: 'noDiffLine' });
    expect(DiffRows.of(twoFiles, [], [], [lineless]).all().some((row) => row.kind === RowKind.Thread)).toBe(false);
  });

  it('has no lines for a diff that was not read, and none for a file that has no hunk', () => {
    expect(DiffRows.of({ kind: 'loadingDiff' }, [], []).lineCount()).toBe(0);
    const renamed = DiffRows.of(diffFixture([fileFixture('b.ts', [], { kind: 'renamedFile', from: 'a.ts' })]), [], []);
    expect(renamed.lineCount()).toBe(0);
    expect(renamed.firstLineOf(0)).toBeUndefined();
    expect(renamed.files()).toHaveLength(1);
  });
});

describe('the lines a file changes', () => {
  it('counts those it adds and those it removes over all its hunks', () => {
    expect(deltaOf(twoFiles.files[0]!)).toEqual({ added: 2, removed: 1 });
  });

  it('counts none for a file with no hunk', () => {
    expect(deltaOf(fileFixture('b.ts', [], { kind: 'renamedFile', from: 'a.ts' }))).toEqual({ added: 0, removed: 0 });
  });
});
