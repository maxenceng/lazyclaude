import { afterEach, describe, expect, it, vi } from 'vitest';
import { ProtocolClient, RpcError } from './client';
import { PROTOCOL_VERSION } from './generated/protocol';
import { type Attached, type Relay, type RelayMessage, RelayMessageKind } from './relay';
import { Connecting, Exited, Ready, Refused } from './status';

class FakeRelay implements Relay {
  sent: string[] = [];
  attachedWith: Attached = { ready: null, exit: null };
  private onMessage: (message: RelayMessage) => void = () => {};

  attach(onMessage: (message: RelayMessage) => void) {
    this.onMessage = onMessage;
    return Promise.resolve(this.attachedWith);
  }

  restart(onMessage: (message: RelayMessage) => void) {
    this.onMessage = onMessage;
    return Promise.resolve({ ready: null, exit: null });
  }

  send(line: string) {
    this.sent.push(line);
    return Promise.resolve();
  }

  push(...lines: string[]) {
    this.onMessage({ kind: RelayMessageKind.Lines, lines });
  }

  exit(code: number | null, stderr: string[]) {
    this.onMessage({ kind: RelayMessageKind.Exited, code, stderr });
  }

  sentRequests() {
    return this.sent.map((line) => JSON.parse(line) as { id: number; method: string; params: unknown });
  }
}

const ready = (protocol: number) => JSON.stringify({ jsonrpc: '2.0', method: 'ready', params: { protocol } });
const answer = (id: number, result: unknown) => JSON.stringify({ jsonrpc: '2.0', id, result });
const noProject = { kind: 'noProject', reason: 'not a project' };

const readyClient = async () => {
  const relay = new FakeRelay();
  const client = new ProtocolClient(relay);
  await client.connect();
  relay.push(ready(PROTOCOL_VERSION));
  return { relay, client };
};

describe('the protocol client', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('sends nothing until the sidecar is ready, then sends what waited', async () => {
    const relay = new FakeRelay();
    const client = new ProtocolClient(relay);
    await client.connect();
    void client.call('board/watch', { from: '.' });
    await Promise.resolve();
    expect(relay.sent).toEqual([]);
    relay.push(ready(PROTOCOL_VERSION));
    await Promise.resolve();
    expect(relay.sentRequests().map((request) => request.method)).toEqual(['board/watch']);
  });

  it('resolves each call with the response carrying its id, whatever the order', async () => {
    const { relay, client } = await readyClient();
    const first = client.call('board/boardFor', { from: 'a' });
    const second = client.call('board/watch', { from: 'b' });
    await Promise.resolve();
    const [one, two] = relay.sentRequests();
    relay.push(answer(two!.id, { kind: 'noProject', reason: 'second' }), answer(one!.id, { kind: 'noProject', reason: 'first' }));
    expect(await first).toEqual({ kind: 'noProject', reason: 'first' });
    expect(await second).toEqual({ kind: 'noProject', reason: 'second' });
  });

  it('rejects a call with the error the sidecar answered', async () => {
    const { relay, client } = await readyClient();
    const call = client.call('board/watch', { from: '.' });
    await Promise.resolve();
    relay.push(JSON.stringify({ jsonrpc: '2.0', id: relay.sentRequests()[0]!.id, error: { code: -32603, message: 'boom' } }));
    await expect(call).rejects.toEqual(new RpcError(-32603, 'boom'));
  });

  it('routes a notification to the handlers of its method only', async () => {
    const { relay, client } = await readyClient();
    const changed: unknown[] = [];
    const other: unknown[] = [];
    client.subscribe('board/changed', (view) => changed.push(view));
    client.subscribe('docs/changed', (view) => other.push(view));
    relay.push(JSON.stringify({ jsonrpc: '2.0', method: 'board/changed', params: noProject }));
    expect(changed).toEqual([noProject]);
    expect(other).toEqual([]);
  });

  it('stops routing to a handler once it unsubscribed', async () => {
    const { relay, client } = await readyClient();
    const changed: unknown[] = [];
    const unsubscribe = client.subscribe('board/changed', (view) => changed.push(view));
    unsubscribe();
    relay.push(JSON.stringify({ jsonrpc: '2.0', method: 'board/changed', params: noProject }));
    expect(changed).toEqual([]);
  });

  it('refuses a sidecar speaking another protocol, naming both versions, and sends nothing', async () => {
    const relay = new FakeRelay();
    const client = new ProtocolClient(relay);
    await client.connect();
    const call = client.call('board/watch', { from: '.' });
    relay.push(ready(PROTOCOL_VERSION + 1));
    expect(client.status.get()).toEqual(new Refused(PROTOCOL_VERSION, PROTOCOL_VERSION + 1));
    await expect(call).rejects.toBeInstanceOf(Error);
    expect(relay.sent).toEqual([]);
  });

  it('takes the ready line kept for a page that reloaded', async () => {
    const relay = new FakeRelay();
    relay.attachedWith = { ready: ready(PROTOCOL_VERSION), exit: null };
    const client = new ProtocolClient(relay);
    await client.connect();
    expect(client.status.get()).toEqual(new Ready());
  });

  it('reports the exit of the sidecar with its stderr and fails the calls waiting', async () => {
    const { relay, client } = await readyClient();
    const call = client.call('board/watch', { from: '.' });
    await Promise.resolve();
    relay.exit(1, ['out of memory']);
    expect(client.status.get()).toEqual(new Exited(1, ['out of memory']));
    await expect(call).rejects.toBeInstanceOf(Error);
  });

  it('reports an exit that happened before the page attached', async () => {
    const relay = new FakeRelay();
    relay.attachedWith = { ready: null, exit: { code: null, stderr: ['no java'] } };
    const client = new ProtocolClient(relay);
    await client.connect();
    expect(client.status.get()).toEqual(new Exited(null, ['no java']));
  });

  it('connects again on restart and waits for the new ready', async () => {
    const { relay, client } = await readyClient();
    relay.exit(1, []);
    await client.restart();
    expect(client.status.get()).toEqual(new Connecting());
    relay.push(ready(PROTOCOL_VERSION));
    expect(client.status.get()).toEqual(new Ready());
  });

  it('ignores a line that is not JSON', async () => {
    const { relay, client } = await readyClient();
    relay.push('not json');
    expect(client.status.get()).toEqual(new Ready());
  });

  it('numbers a reloaded page past the ids the page before it sent to the same sidecar', async () => {
    vi.useFakeTimers({ now: 1_000 });
    const before = await readyClient();
    void before.client.call('board/watch', { from: '.' });
    void before.client.call('board/watch', { from: '.' });
    vi.setSystemTime(1_001);
    const after = await readyClient();
    void after.client.call('board/watch', { from: '.' });
    await Promise.resolve();
    const earlier = before.relay.sentRequests().map((request) => request.id);
    const later = after.relay.sentRequests()[0]!.id;
    expect(earlier.every((id) => id < later)).toBe(true);
  });
});
