import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { WarningStore } from './warningStore';

describe('the warning on the status line', () => {
  it('shows none until a watch answers', () => {
    expect(new WarningStore(fakeClient()).warning.get()).toBeUndefined();
  });

  it('asks warning/watch and shows the warning it answers', async () => {
    const client = fakeClient();
    const store = new WarningStore(client);
    const watching = store.watch();
    client.answerNext({ kind: 'warning', text: 'hosted session: claude exited with code 2.', count: 1 });
    await watching;
    expect(client.calls).toEqual([{ method: 'warning/watch', params: {} }]);
    expect(store.warning.get()).toEqual({ text: 'hosted session: claude exited with code 2.', count: 1 });
  });

  it('shows each warning/changed after it, and none once the last is withdrawn', async () => {
    const client = fakeClient();
    const store = new WarningStore(client);
    const watching = store.watch();
    client.answerNext({ kind: 'noWarning' });
    await watching;
    client.push('warning/changed', { kind: 'warning', text: 'resume: gone', count: 2 });
    expect(store.warning.get()).toEqual({ text: 'resume: gone', count: 2 });
    client.push('warning/changed', { kind: 'noWarning' });
    expect(store.warning.get()).toBeUndefined();
  });

  it('shows none when the watch fails, since a warning that cannot be read is not one to invent', async () => {
    const client = fakeClient();
    const store = new WarningStore(client);
    const watching = store.watch();
    client.failNext(new RpcError(-32000, 'broken'));
    await watching;
    expect(store.warning.get()).toBeUndefined();
  });
});
