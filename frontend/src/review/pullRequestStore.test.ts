import { describe, expect, it } from 'vitest';
import { RpcError } from '../protocol';
import { fakeClient } from '../test/fakeClient';
import { pullRequestFixture } from './fixtures';
import { PullRequestStore } from './pullRequestStore';

describe('the pull requests', () => {
  it('are being checked until review/watch answers, and then held as it answered', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    expect(client.calls).toEqual([{ method: 'review/watch', params: { root: '/p' } }]);
    expect(store.state.get().kind).toBe('checkingPullRequests');
    const open = { kind: 'openPullRequests' as const, pullRequests: [pullRequestFixture({ number: 7, head: 'proj-1-first' })] };
    client.answerNext(open);
    await watching;
    expect(store.state.get()).toEqual(open);
  });

  it('ask review which one a branch heads, from the pull requests last pushed', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const asking = store.forBranch('proj-2-second');
    expect(client.calls).toEqual([{ method: 'review/pullRequestOn', params: { pullRequests: { kind: 'checkingPullRequests' }, branch: 'proj-2-second' } }]);
    client.answerNext({ kind: 'headedPullRequest', pullRequest: pullRequestFixture({ number: 8 }) });
    expect((await asking)?.number).toBe(8);
    const none = store.forBranch('proj-3-third');
    client.answerNext({ kind: 'noHeadedPullRequest' });
    expect(await none).toBeUndefined();
  });

  it('follow each review/changed after the watch', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    client.answerNext({ kind: 'openPullRequests', pullRequests: [] });
    await watching;
    client.push('review/changed', { kind: 'openPullRequests', pullRequests: [pullRequestFixture({ head: 'b' })] });
    expect(store.state.get()).toEqual({ kind: 'openPullRequests', pullRequests: [pullRequestFixture({ head: 'b' })] });
  });

  it('are unavailable, with the reason, when the sidecar says so', async () => {
    const client = fakeClient();
    const store = new PullRequestStore(client);
    const watching = store.watch('/p');
    client.answerNext({ kind: 'unavailablePullRequests', reason: 'gh is not signed in' });
    await watching;
    expect(store.state.get()).toEqual({ kind: 'unavailablePullRequests', reason: 'gh is not signed in' });
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
