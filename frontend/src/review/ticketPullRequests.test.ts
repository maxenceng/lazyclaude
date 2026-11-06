import { describe, expect, it } from 'vitest';
import { fakeClient } from '../test/fakeClient';
import { pullRequestFixture } from './fixtures';
import { PullRequestStore } from './pullRequestStore';
import { TicketPullRequests } from './ticketPullRequests';

const setup = () => {
  const client = fakeClient();
  return { client, joined: new TicketPullRequests(new PullRequestStore(client)) };
};

const settled = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('the pull requests of the tickets', () => {
  it('holds none until a join is asked', () => {
    expect(setup().joined.of('proj-1-first')).toBeUndefined();
  });

  it('asks review which pull request each branch heads, and holds the ones that answered', async () => {
    const { client, joined } = setup();
    const joining = joined.join(['proj-1-first', 'proj-2-second']);
    expect(client.calls.map((call) => call.params)).toMatchObject([{ branch: 'proj-1-first' }, { branch: 'proj-2-second' }]);
    client.answerNext({ kind: 'headedPullRequest', pullRequest: pullRequestFixture({ number: 7 }) });
    client.answerNext({ kind: 'noHeadedPullRequest' });
    await joining;
    expect(joined.of('proj-1-first')?.number).toBe(7);
    expect(joined.of('proj-2-second')).toBeUndefined();
  });

  it('takes a branch whose ask failed for one with no pull request', async () => {
    const { client, joined } = setup();
    const joining = joined.join(['proj-1-first']);
    client.failNext(new Error('refused'));
    await joining;
    expect(joined.of('proj-1-first')).toBeUndefined();
  });

  it('drops the answers of a join that a later one overtook', async () => {
    const { client, joined } = setup();
    const first = joined.join(['proj-1-first']);
    const second = joined.join(['proj-1-first']);
    client.answerNext({ kind: 'headedPullRequest', pullRequest: pullRequestFixture({ number: 1 }) });
    client.answerNext({ kind: 'headedPullRequest', pullRequest: pullRequestFixture({ number: 2 }) });
    await Promise.all([first, second]);
    await settled();
    expect(joined.of('proj-1-first')?.number).toBe(2);
  });
});
