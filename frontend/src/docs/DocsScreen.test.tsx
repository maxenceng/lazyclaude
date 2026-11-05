import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { RecordingOpener } from './recordingOpener';
import { DocsController } from './docsController';
import { DocsPane, DocumentPane } from './DocsScreen';
import { emptyFixture, folderFixture, readFixture, treeFixture, unreadableFixture } from './fixtures';

const setup = () => {
  const client = fakeClient();
  const opener = new RecordingOpener();
  const warnings: string[] = [];
  const controller = new DocsController({ client, root: () => '/p', opener, warn: (text) => warnings.push(text), onOpened: () => {} });
  const onFocus = vi.fn();
  const onRetry = vi.fn(() => void controller.load());
  const loaded = controller.load();
  render(
    <>
      <DocsPane docs={controller} focused onFocus={onFocus} onRetry={onRetry} />
      <DocumentPane docs={controller} focused={false} />
    </>,
  );
  return { client, controller, opener, warnings, loaded, onFocus, onRetry };
};

const answer = async (client: ReturnType<typeof fakeClient>, loaded: Promise<void>, result: unknown) => {
  await act(async () => {
    client.answerNext(result);
    await loaded;
  });
};

const tree = treeFixture(
  [readFixture('README.md', '# Docs\n\nSee [the guide](guide/intro.md#setup) and [a source](../src/main.rs) and [the site](https://example.com).'), emptyFixture('blank.md')],
  [folderFixture('guide', [readFixture('guide/intro.md', '# Intro\n\n## Setup\n\nText.'), unreadableFixture('guide/bad.md', 'not valid UTF-8')])],
);

describe('the Docs panel', () => {
  it('says it is reading while the watch has not answered', () => {
    setup();
    expect(screen.getByRole('status')).toHaveTextContent('Reading the docs');
  });

  it('says there is no document when the docs are empty', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, treeFixture([]));
    expect(screen.getByText('No documents under docs/.')).toBeInTheDocument();
  });

  it('says why the docs are unavailable', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, { kind: 'unavailableDocs', reason: 'docs is not a folder' });
    expect(screen.getByText('The docs cannot be read')).toBeInTheDocument();
    expect(screen.getByText('docs is not a folder')).toBeInTheDocument();
  });

  it('shows the failure of the read with a retry', async () => {
    const { client, loaded, onRetry } = setup();
    await act(async () => {
      client.failNext(new RpcError(-32000, 'sidecar gone'));
      await loaded;
    });
    expect(screen.getByRole('alert')).toHaveTextContent('sidecar gone');
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('lists the rows in the order of the tree, a folder with its count', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    const rows = within(screen.getByRole('region', { name: 'Docs' })).getAllByRole('button');
    expect(rows.map((row) => row.textContent)).toEqual(['README.md', 'blank.md', 'guide2', 'intro.md', 'bad.md']);
    expect(rows[0]).toHaveAttribute('aria-current', 'true');
  });

  it('selects on a first click, shows the document, and opens on a second', async () => {
    const { client, loaded, controller, onFocus } = setup();
    await answer(client, loaded, tree);
    await userEvent.click(screen.getByRole('button', { name: 'blank.md' }));
    expect(onFocus).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'blank.md' })).toHaveAttribute('aria-current', 'true');
    const opened = controller.selection.get().opened;
    await userEvent.click(screen.getByRole('button', { name: 'blank.md' }));
    expect(controller.selection.get().opened).toBeGreaterThan(opened);
  });

  it('folds a folder on a second click', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    await userEvent.click(screen.getByRole('button', { name: /guide/ }));
    await userEvent.click(screen.getByRole('button', { name: /guide/ }));
    expect(screen.queryByRole('button', { name: 'intro.md' })).toBeNull();
    expect(screen.getByRole('button', { name: /guide/ })).toHaveAttribute('aria-expanded', 'false');
  });
});

describe('the document in the main view', () => {
  it('says nothing is selected before the docs are read', () => {
    setup();
    expect(screen.getByText('No document selected.')).toBeInTheDocument();
  });

  it('typesets the selected document', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    expect(screen.getByRole('heading', { level: 1, name: 'Docs' })).toBeInTheDocument();
  });

  it('says an empty document is empty', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    await userEvent.click(screen.getByRole('button', { name: 'blank.md' }));
    expect(screen.getByText('Empty document.')).toBeInTheDocument();
  });

  it('shows an unreadable document by its path and reason', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    await userEvent.click(screen.getByRole('button', { name: 'bad.md' }));
    expect(screen.getByRole('heading', { name: 'guide/bad.md' })).toBeInTheDocument();
    expect(screen.getByText('not valid UTF-8')).toBeInTheDocument();
  });

  it('opens a relative link to a document in the main view and selects it in the tree, without navigating', async () => {
    const { client, loaded } = setup();
    await answer(client, loaded, tree);
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    await userEvent.click(screen.getByRole('link', { name: 'the guide' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Intro' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'intro.md' })).toHaveAttribute('aria-current', 'true');
    expect((scroll.mock.contexts.at(-1) as HTMLElement).id).toBe('setup');
    expect(window.location.hash).toBe('');
  });

  it('shows a link to a missing document under docs as unreadable, with its path and why', async () => {
    const { client, loaded, controller } = setup();
    await answer(client, loaded, treeFixture([readFixture('README.md', '[gone](gone.md)')]));
    await userEvent.click(screen.getByRole('link', { name: 'gone' }));
    expect(controller.shown().kind).toBe('missing');
    expect(screen.getByRole('heading', { name: 'gone.md' })).toBeInTheDocument();
    expect(screen.getByText('No document at this path in the docs tree.')).toBeInTheDocument();
  });

  it('opens a link outside the tree and an http link through the opener', async () => {
    const { client, loaded, opener } = setup();
    await answer(client, loaded, tree);
    await userEvent.click(screen.getByRole('link', { name: 'a source' }));
    await userEvent.click(screen.getByRole('link', { name: 'the site' }));
    expect(opener.paths).toEqual(['/p/src/main.rs']);
    expect(opener.urls).toEqual(['https://example.com']);
  });
});
