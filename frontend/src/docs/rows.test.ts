import { describe, expect, it } from 'vitest';
import { emptyFixture, folderFixture, readFixture, treeFixture } from './fixtures';
import { RowKind, documentIn, visibleRows } from './rows';

const tree = treeFixture(
  [readFixture('README.md', '# Hi'), emptyFixture('blank.md')],
  [
    folderFixture('adr', [readFixture('adr/0001-first.md', 'one')], [folderFixture('adr/old', [readFixture('adr/old/0000.md', 'zero')])]),
    folderFixture('guide', [readFixture('guide/intro.md', 'intro')]),
  ],
);

describe('the rows of the docs tree', () => {
  it('lists the documents, then each folder with its own documents and folders, as the tree arrives', () => {
    expect(visibleRows(tree, new Set()).map((row) => [row.kind, row.path, row.name, row.depth])).toEqual([
      [RowKind.Document, 'README.md', 'README.md', 0],
      [RowKind.Document, 'blank.md', 'blank.md', 0],
      [RowKind.Folder, 'adr', 'adr', 0],
      [RowKind.Document, 'adr/0001-first.md', '0001-first.md', 1],
      [RowKind.Folder, 'adr/old', 'old', 1],
      [RowKind.Document, 'adr/old/0000.md', '0000.md', 2],
      [RowKind.Folder, 'guide', 'guide', 0],
      [RowKind.Document, 'guide/intro.md', 'intro.md', 1],
    ]);
  });

  it('counts the documents beneath a folder at any depth', () => {
    const counts = visibleRows(tree, new Set())
      .filter((row) => row.kind === RowKind.Folder)
      .map((row) => [row.path, row.count]);
    expect(counts).toEqual([
      ['adr', 2],
      ['adr/old', 1],
      ['guide', 1],
    ]);
  });

  it('hides what is beneath a folded folder and marks the folder', () => {
    const rows = visibleRows(tree, new Set(['adr']));
    expect(rows.map((row) => row.path)).toEqual(['README.md', 'blank.md', 'adr', 'guide', 'guide/intro.md']);
    expect(rows.find((row) => row.path === 'adr')?.folded).toBe(true);
  });

  it('finds a document at any depth by its path, and none for a path the tree does not list', () => {
    expect(documentIn(tree, 'adr/old/0000.md')?.path).toBe('adr/old/0000.md');
    expect(documentIn(tree, 'adr/missing.md')).toBeUndefined();
  });
});
