import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { pullRequestFixture } from './fixtures';
import { PullRequestStore } from './pullRequestStore';

describe('the pull requests', () => {
  it('are being checked until review/watch answers, and then the open one whose head is a branch is that branch\'s', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    expect(store.forBranch('proj-1-first')).toBeUndefined();
    const watching = store.watch('/p');
    expect(client.calls).toEqual([{ method: 'review/watch', params: { root: '/p' } }]);
    expect(store.state.get().kind).toBe('checkingPullRequests');
    client.answerNext({ kind: 'openPullRequests', pullRequests: [pullRequestFixture({ number: 7, head: 'proj-1-first' }), pullRequestFixture({ number: 8, head: 'proj-2-second' })] });
    await watching;
    expect(store.forBranch('proj-2-second')?.number).toBe(8);
    expect(store.forBranch('proj-3-third')).toBeUndefined();
  });

  it('follow each review/changed after the watch', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    client.answerNext({ kind: 'openPullRequests', pullRequests: [] });
    await watching;
    client.push('review/changed', { kind: 'openPullRequests', pullRequests: [pullRequestFixture({ head: 'b' })] });
    expect(store.forBranch('b')?.number).toBe(7);
  });

  it('are unavailable, with the reason, when the sidecar says so, and none is anyone\'s', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    client.answerNext({ kind: 'unavailablePullRequests', reason: 'gh is not signed in' });
    await watching;
    expect(store.state.get()).toEqual({ kind: 'unavailablePullRequests', reason: 'gh is not signed in' });
    expect(store.forBranch('b')).toBeUndefined();
  });

  it('are unavailable, with the message, when the call itself fails', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    client.failNext(new RpcError(-32000, 'sidecar gone'));
    await watching;
    expect(store.state.get()).toEqual({ kind: 'unavailablePullRequests', reason: 'sidecar gone' });
  });
});
