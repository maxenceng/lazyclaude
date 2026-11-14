import { describe, expect, it } from 'vitest';
import { DiffRows, RowKind, StopKind, deltaOf } from './diffRows';
import { added, atHead, diffFixture, fileFixture, hunk, kept, onLine, outside, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff } from './fixtures';

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

  it('stops the cursor on each line, in the order they are listed, knowing the file of each', () => {
    const rows = DiffRows.of(twoFiles, [], []);
    expect(rows.stops()).toEqual([0, 1, 2, 3].map((line) => ({ kind: StopKind.Line, line, file: 0 })).concat([{ kind: StopKind.Line, line: 4, file: 1 }]));
    expect(rows.firstStopOf(1)).toBe(4);
    expect(rows.firstStopOfNextFile(0)).toBe(4);
    expect(rows.firstStopOfNextFile(4)).toBeUndefined();
    expect(rows.firstStopOfPreviousFile(4)).toBe(0);
    expect(rows.firstStopOfPreviousFile(2)).toBeUndefined();
  });

  it('puts the threads review shows under their lines, and every pending comment, under the line they are placed on', () => {
    const open = threadInDiff(threadFixture({ id: 'open' }), onLine(1));
    const resolved = threadInDiff(threadFixture({ id: 'done', resolved: true, open: false }), onLine(1));
    const pending = pendingInDiff(pendingCommentFixture({ id: 'mine' }), onLine(1));
    const rows = DiffRows.of(twoFiles, [open, resolved], [pending]);
    expect(rows.all().map((row) => row.kind).slice(2, 7)).toEqual([RowKind.Line, RowKind.Line, RowKind.Thread, RowKind.Thread, RowKind.Pending]);
  });

  it('shows a thread review places at the head of a file once, right under its rule and before its first hunk, and stops the cursor on that rule', () => {
    const lineless = threadInDiff(threadFixture({ id: 'old', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), atHead(1));
    const rows = DiffRows.of(twoFiles, [lineless], []);
    expect(rows.all().map((row) => row.kind).slice(7, 10)).toEqual([RowKind.File, RowKind.Thread, RowKind.Hunk]);
    expect(rows.all().filter((row) => row.kind === RowKind.Thread)).toHaveLength(1);
    expect(rows.stops().slice(4)).toEqual([
      { kind: StopKind.Head, file: 1 },
      { kind: StopKind.Line, line: 4, file: 1 },
    ]);
    expect(rows.firstStopOf(1)).toBe(4);
  });

  it('shows a pending comment review places at the head of a file under that rule too', () => {
    const lineless = pendingInDiff(pendingCommentFixture({ id: 'mine', anchor: { kind: 'onDiffAnchor', path: 'src/a.ts', position: { kind: 'placedThread', side: 'RIGHT', first: 99, last: 99 } } }), atHead(0));
    expect(DiffRows.of(twoFiles, [], [lineless]).all().map((row) => row.kind).slice(0, 3)).toEqual([RowKind.File, RowKind.Pending, RowKind.Hunk]);
  });

  it('says which lines a thread or a pending comment hangs from, and which cards hang from none', () => {
    const open = threadInDiff(threadFixture({ id: 'open' }), onLine(1));
    const pending = pendingInDiff(pendingCommentFixture({ id: 'mine' }), onLine(3));
    const lineless = threadInDiff(threadFixture({ id: 'old', path: 'src/b.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), atHead(1));
    const rows = DiffRows.of(twoFiles, [open, lineless], [pending]);
    expect([0, 1, 2, 3].map((line) => [rows.hasThread(line), rows.hasPending(line)])).toEqual([
      [false, false],
      [true, false],
      [false, false],
      [false, true],
    ]);
    expect(rows.all().filter((row) => row.kind === RowKind.Thread).map((row) => row.lineless)).toEqual([false, true]);
  });

  it('shows what review places outside the diff under a rule after the last file, which the cursor stops on last', () => {
    const gone = threadInDiff(threadFixture({ path: 'gone.ts', position: { kind: 'outdatedThread', was: { kind: 'noOriginalPlace' } } }), outside);
    const mine = pendingInDiff(pendingCommentFixture({ id: 'mine' }), outside);
    const rows = DiffRows.of(twoFiles, [gone], [mine]);
    expect(rows.all().map((row) => row.kind).slice(-3)).toEqual([RowKind.Outside, RowKind.Thread, RowKind.Pending]);
    expect(rows.stops().at(-1)).toEqual({ kind: StopKind.Outside, file: 2 });
    expect(rows.firstStopOfNextFile(4)).toBe(5);
  });

  it('has no rows for a diff that was not read, and no lines for a file that has no hunk', () => {
    expect(DiffRows.of({ kind: 'loadingDiff' }, [threadInDiff(threadFixture(), outside)], []).all()).toEqual([]);
    const renamed = DiffRows.of(diffFixture([fileFixture('b.ts', [], { kind: 'renamedFile', from: 'a.ts' })]), [], []);
    expect(renamed.lineCount()).toBe(0);
    expect(renamed.firstStopOf(0)).toBeUndefined();
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
