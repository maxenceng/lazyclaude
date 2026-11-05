import { describe, expect, it } from 'vitest';
import { DiffRows, RowKind } from './diffRows';
import { added, diffFixture, fileFixture, hunk, kept, onLine, pendingCommentFixture, pendingInDiff, removed, threadFixture, threadInDiff } from './fixtures';

const twoFiles = diffFixture([
  fileFixture('src/a.ts', [hunk('@@ -1,2 +1,3 @@', [kept(1, 1, 'one'), removed(2, 'two'), added(2, 'deux')]), hunk('@@ -9 +10 @@', [added(10, 'ten')])]),
  fileFixture('src/b.ts', [hunk('@@ -1 +1 @@', [added(1, 'b')])]),
]);

describe('the rows of a review diff', () => {
  it('lists a rule for each file, a header for each hunk and a row for each line', () => {
    const rows = DiffRows.of(twoFiles, [], [], false);
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
    const rows = DiffRows.of(twoFiles, [], [], false);
    expect([0, 1, 2, 3, 4].map((line) => rows.fileOfLine(line))).toEqual([0, 0, 0, 0, 1]);
    expect(rows.firstLineOf(1)).toBe(4);
    expect(rows.firstLineOfNextFile(0)).toBe(4);
    expect(rows.firstLineOfNextFile(4)).toBeUndefined();
    expect(rows.firstLineOfPreviousFile(4)).toBe(0);
    expect(rows.firstLineOfPreviousFile(2)).toBeUndefined();
  });

  it('puts a thread and a pending comment under the line they are placed on, and hides a resolved thread unless asked', () => {
    const open = threadInDiff(threadFixture({ id: 'open' }), onLine(1));
    const resolved = threadInDiff(threadFixture({ id: 'done', resolved: true, open: false }), onLine(1));
    const pending = pendingInDiff(pendingCommentFixture({ id: 'mine' }), onLine(1));
    const hidden = DiffRows.of(twoFiles, [open, resolved], [pending], false);
    expect(hidden.all().map((row) => row.kind).slice(2, 6)).toEqual([RowKind.Line, RowKind.Line, RowKind.Thread, RowKind.Pending]);
    const shown = DiffRows.of(twoFiles, [open, resolved], [pending], true);
    expect(shown.all().filter((row) => row.kind === RowKind.Thread)).toHaveLength(2);
  });

  it('leaves a thread that sits on no line out of the rows', () => {
    const rows = DiffRows.of(twoFiles, [threadInDiff(threadFixture(), { kind: 'noDiffLine' })], [], true);
    expect(rows.all().some((row) => row.kind === RowKind.Thread)).toBe(false);
  });

  it('has no lines for a diff that was not read, and none for a file that has no hunk', () => {
    expect(DiffRows.of({ kind: 'loadingDiff' }, [], [], false).lineCount()).toBe(0);
    const renamed = DiffRows.of(diffFixture([fileFixture('b.ts', [], { kind: 'renamedFile', from: 'a.ts' })]), [], [], false);
    expect(renamed.lineCount()).toBe(0);
    expect(renamed.firstLineOf(0)).toBeUndefined();
    expect(renamed.files()).toHaveLength(1);
  });
});
