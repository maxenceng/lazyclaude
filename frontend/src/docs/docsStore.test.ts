import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { DocsStateKind } from './docsState';
import { DocsStore } from './docsStore';
import { readFixture, treeFixture } from './fixtures';

describe('the docs read', () => {
  it('is loading until docs/watch answers, and asks for the project root', async () => {
    const client = fakeClient();
    const store = new DocsStore(client);
    expect(store.state.get().kind).toBe(DocsStateKind.Loading);
    const loading = store.load('/p');
    expect(client.calls).toEqual([{ method: 'docs/watch', params: { root: '/p' } }]);
    expect(store.state.get().kind).toBe(DocsStateKind.Loading);
    client.answerNext(treeFixture([readFixture('README.md', '# Hi')]));
    await loading;
    expect(store.state.get().kind).toBe(DocsStateKind.Populated);
  });

  it('is empty when the tree holds no document and no folder', async () => {
    const client = fakeClient();
    const store = new DocsStore(client);
    const loading = store.load('/p');
    client.answerNext(treeFixture([]));
    await loading;
    expect(store.state.get().kind).toBe(DocsStateKind.Empty);
  });

  it('is unavailable with the reason the sidecar gives', async () => {
    const client = fakeClient();
    const store = new DocsStore(client);
    const loading = store.load('/p');
    client.answerNext({ kind: 'unavailableDocs', reason: 'no docs folder' });
    await loading;
    expect(store.state.get()).toMatchObject({ kind: DocsStateKind.Unavailable, reason: 'no docs folder' });
  });

  it('is a fault when the call itself fails', async () => {
    const client = fakeClient();
    const store = new DocsStore(client);
    const loading = store.load('/p');
    client.failNext(new RpcError(-32000, 'sidecar gone'));
    await loading;
    expect(store.state.get()).toMatchObject({ kind: DocsStateKind.Fault, message: 'sidecar gone' });
  });

  it('shows each docs/changed after the watch, in the order it arrives', async () => {
    const client = fakeClient();
    const store = new DocsStore(client);
    const loading = store.load('/p');
    client.answerNext(treeFixture([readFixture('a.md', 'a')]));
    await loading;
    client.push('docs/changed', treeFixture([readFixture('b.md', 'b'), readFixture('a.md', 'a')]));
    expect(store.state.get().shown()?.documents.map((document) => document.path)).toEqual(['b.md', 'a.md']);
    client.push('docs/changed', { kind: 'unavailableDocs', reason: 'folder removed' });
    expect(store.state.get().kind).toBe(DocsStateKind.Unavailable);
  });
});
