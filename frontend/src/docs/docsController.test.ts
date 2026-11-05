import { describe, expect, it } from 'vitest';
import { fakeClient } from '../test/fakeClient';
import { RecordingOpener } from './recordingOpener';
import { DocsController } from './docsController';
import { emptyFixture, folderFixture, readFixture, treeFixture, unreadableFixture } from './fixtures';
import { RowKind } from './rows';
import { ShownKind } from './shownDocument';

const TREE = treeFixture(
  [readFixture('README.md', '# Docs\n\nSee [the guide](guide/intro.md#setup).'), emptyFixture('blank.md')],
  [folderFixture('guide', [readFixture('guide/intro.md', '# Intro\n\n## Setup\n\nText.\n\n## Setup\n\nAgain.')])],
);

const setup = async (tree = TREE) => {
  const client = fakeClient();
  const opener = new RecordingOpener();
  const warnings: string[] = [];
  const controller = new DocsController({ client, root: () => '/p', opener, warn: (text) => warnings.push(text), onOpened: () => {} });
  const loading = controller.load();
  client.answerNext(tree);
  await loading;
  return { controller, client, opener, warnings };
};

describe('following a link in the docs', () => {
  it('opens the document at its top and says the fragment was not found when no heading has that slug', async () => {
    const { controller, warnings } = await setup();
    controller.follow('guide/intro.md#nowhere');
    expect(controller.shown()).toMatchObject({ kind: ShownKind.Found, document: { path: 'guide/intro.md' } });
    expect(controller.selection.get()).toMatchObject({ fragment: undefined });
    expect(warnings).toEqual(['No heading "nowhere" in guide/intro.md; opened at its top.']);
  });

  it('opens at the heading whose slug matches, and selects the document in the tree', async () => {
    const { controller, warnings } = await setup();
    controller.follow('guide/intro.md#setup-1');
    expect(controller.selectedKey()).toEqual({ kind: RowKind.Document, path: 'guide/intro.md' });
    expect(controller.selection.get()).toMatchObject({ fragment: 'setup-1' });
    expect(warnings).toEqual([]);
  });

  it('scrolls again when the same heading is asked for again', async () => {
    const { controller } = await setup();
    controller.follow('guide/intro.md#setup');
    const first = controller.selection.get().opened;
    controller.follow('#setup');
    expect(controller.selection.get().opened).toBeGreaterThan(first);
    expect(controller.selection.get()).toMatchObject({ fragment: 'setup' });
  });

  it('unfolds the folders above the document it opens', async () => {
    const { controller } = await setup();
    controller.press({ kind: RowKind.Folder, path: 'guide' });
    controller.activate();
    expect(controller.rows().map((row) => row.path)).not.toContain('guide/intro.md');
    controller.followFromTicket('docs/guide/intro.md');
    expect(controller.rows().map((row) => row.path)).toContain('guide/intro.md');
  });

  it('shows a link to a missing document under docs as a document that cannot be read, selecting no row', async () => {
    const { controller, warnings } = await setup();
    controller.follow('guide/gone.md#x');
    expect(controller.shown()).toMatchObject({ kind: ShownKind.Missing, path: 'guide/gone.md' });
    expect(controller.rows().map((row) => row.path)).toContain('guide/intro.md');
    expect(warnings).toEqual([]);
  });

  it('says nothing of a fragment when the document it names cannot be read, since its reason is shown', async () => {
    const { controller, warnings } = await setup(treeFixture([readFixture('README.md', '# Docs'), unreadableFixture('locked.md', 'Permission denied')]));
    controller.follow('locked.md#part');
    expect(controller.shown()).toMatchObject({ kind: ShownKind.Found, document: { path: 'locked.md' } });
    expect(warnings).toEqual([]);
  });

  it('says the fragment was not found in an empty document', async () => {
    const { controller, warnings } = await setup();
    controller.follow('blank.md#part');
    expect(warnings).toEqual(['No heading "part" in blank.md; opened at its top.']);
  });

  it('opens a file under docs that the tree does not list with the system opener', async () => {
    const { controller, opener } = await setup();
    controller.follow('guide/diagram.svg');
    expect(opener.paths).toEqual(['/p/docs/guide/diagram.svg']);
    expect(controller.selectedKey()).toEqual({ kind: RowKind.Document, path: 'README.md' });
  });

  it('warns and opens nothing for a fragment in a ticket\'s body', async () => {
    const { controller, opener, warnings } = await setup();
    controller.followFromTicket('#notes');
    expect(opener.paths).toEqual([]);
    expect(warnings).toEqual(["Did not follow #notes: a ticket's own headings are not followed."]);
  });

  it('opens a link outside the tree with the system opener, resolved from the project root', async () => {
    const { controller, opener } = await setup();
    controller.follow('../src/main.rs');
    expect(opener.paths).toEqual(['/p/src/main.rs']);
    expect(opener.urls).toEqual([]);
    expect(controller.shown()).toMatchObject({ kind: ShownKind.Found, document: { path: 'README.md' } });
  });

  it('opens an http link in the system browser', async () => {
    const { controller, opener } = await setup();
    controller.follow('https://example.com/a');
    expect(opener.urls).toEqual(['https://example.com/a']);
  });

  it('warns with the reason when the system opener fails', async () => {
    const { controller, opener, warnings } = await setup();
    opener.failure = new Error('xdg-open not found');
    controller.follow('../src/main.rs');
    controller.follow('https://example.com/a');
    await Promise.resolve();
    await Promise.resolve();
    expect(warnings).toEqual(['Could not open /p/src/main.rs: xdg-open not found', 'Could not open https://example.com/a: xdg-open not found']);
  });

  it('warns and opens nothing for a scheme it does not open', async () => {
    const { controller, opener, warnings } = await setup();
    controller.follow('javascript:alert(1)');
    expect(opener.urls).toEqual([]);
    expect(opener.paths).toEqual([]);
    expect(warnings).toEqual(['Did not open javascript:alert(1): only http and https links and project paths are opened.']);
  });

  it('follows a ticket\'s link from the project root', async () => {
    const { controller, opener } = await setup();
    controller.followFromTicket('docs/guide/intro.md#intro');
    expect(controller.selectedKey()).toEqual({ kind: RowKind.Document, path: 'guide/intro.md' });
    controller.followFromTicket('src/lib.rs');
    expect(opener.paths).toEqual(['/p/src/lib.rs']);
  });
});

describe('the selection in the docs tree', () => {
  it('starts on the first row and moves over the rows without leaving them', async () => {
    const { controller } = await setup();
    expect(controller.selectedKey()).toEqual({ kind: RowKind.Document, path: 'README.md' });
    controller.move(-1);
    expect(controller.selectedKey()?.path).toBe('README.md');
    controller.move(1);
    expect(controller.selectedKey()?.path).toBe('blank.md');
    controller.move(10);
    expect(controller.selectedKey()?.path).toBe('guide/intro.md');
  });

  it('shows nothing on a folder and the document on a document', async () => {
    const { controller } = await setup();
    controller.move(2);
    expect(controller.shown().kind).toBe(ShownKind.None);
    controller.move(1);
    expect(controller.shown()).toMatchObject({ kind: ShownKind.Found, document: { path: 'guide/intro.md' } });
  });

  it('selects on a first press and opens on a second, folding a folder', async () => {
    const { controller } = await setup();
    controller.press({ kind: RowKind.Document, path: 'blank.md' });
    const opened = controller.selection.get().opened;
    controller.press({ kind: RowKind.Document, path: 'blank.md' });
    expect(controller.selection.get().opened).toBeGreaterThan(opened);
    controller.press({ kind: RowKind.Folder, path: 'guide' });
    controller.press({ kind: RowKind.Folder, path: 'guide' });
    expect(controller.rows().find((row) => row.path === 'guide')?.folded).toBe(true);
  });

  it('keeps the selection on its path when the tree is read again, whatever order it comes in', async () => {
    const { controller, client } = await setup();
    controller.press({ kind: RowKind.Document, path: 'blank.md' });
    client.push('docs/changed', treeFixture([emptyFixture('blank.md'), readFixture('README.md', 'x')]));
    expect(controller.selectedKey()?.path).toBe('blank.md');
  });

  it('moves to the row that took its place when the selected document goes', async () => {
    const { controller, client } = await setup();
    controller.press({ kind: RowKind.Document, path: 'blank.md' });
    client.push('docs/changed', treeFixture([readFixture('README.md', 'x'), readFixture('zzz.md', 'z')]));
    expect(controller.selectedKey()?.path).toBe('zzz.md');
  });

  it('stays on the first row it started on when a later read puts another document first', async () => {
    const { controller, client } = await setup();
    client.push('docs/changed', treeFixture([readFixture('AAA.md', 'a'), readFixture('README.md', 'x'), emptyFixture('blank.md')]));
    expect(controller.selectedKey()?.path).toBe('README.md');
  });

  it('moves to the row that took its place among the rows shown after a fold', async () => {
    const guide = folderFixture('guide', [readFixture('guide/intro.md', 'i'), readFixture('guide/more.md', 'm')]);
    const { controller, client } = await setup(treeFixture([readFixture('README.md', 'x')], [guide, folderFixture('x', []), folderFixture('y', []), folderFixture('z', [])]));
    controller.press({ kind: RowKind.Folder, path: 'guide' });
    controller.activate();
    controller.press({ kind: RowKind.Folder, path: 'x' });
    client.push('docs/changed', treeFixture([readFixture('README.md', 'x')], [guide, folderFixture('y', []), folderFixture('z', [])]));
    expect(controller.selectedKey()).toEqual({ kind: RowKind.Folder, path: 'y' });
  });

  it('keeps no history: a link followed and a move later leave only the selection', async () => {
    const { controller } = await setup();
    controller.follow('guide/intro.md');
    controller.move(-1);
    expect(controller.selectedKey()?.path).toBe('guide');
  });
});
