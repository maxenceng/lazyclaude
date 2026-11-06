import { describe, expect, it } from 'vitest';
import { NoticeStore } from '../shared/noticeStore';
import { type FakeClient, fakeClient } from '../test/fakeClient';
import { pullRequestFixture } from './fixtures';
import { ReviewController } from './reviewController';

const setup = async (rooted = true) => {
  const client = fakeClient();
  const notices = new NoticeStore();
  const events: string[] = [];
  const controller = new ReviewController({
    client,
    root: () => (rooted ? '/p' : undefined),
    notices,
    onOpened: () => events.push('opened'),
    onLeft: () => events.push('left'),
  });
  const watching = controller.pullRequests.watch('/p');
  client.answerNext({ kind: 'openPullRequests', pullRequests: [pullRequestFixture({ number: 7, title: 'Add the thing', head: 'proj-1-first' })] });
  await watching;
  client.calls.length = 0;
  return { client, controller, notices, events };
};

const HEADED = { kind: 'headedPullRequest', pullRequest: pullRequestFixture({ number: 7, title: 'Add the thing', head: 'proj-1-first' }) };

const openOn = (client: FakeClient, controller: ReviewController, branch: string, answer: unknown = HEADED) => {
  const opening = controller.open(branch);
  client.answer('review/pullRequestOn', answer);
  return opening;
};

const SETTLED = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('opening a review', () => {
  it('opens the review of the open pull request whose head is the ticket\'s branch, and reads it', async () => {
    const { client, controller, events } = await setup();
    expect(await openOn(client, controller, 'proj-1-first')).toBe(true);
    const review = controller.current.get();
    expect(review?.number()).toBe(7);
    expect(review?.title()).toBe('Add the thing');
    expect(client.calls.map((call) => call.method)).toEqual(['review/pullRequestOn', 'review/pendingOf', 'review/diffOf', 'review/threadsOf']);
    expect(client.calls[0]?.params).toEqual({
      pullRequests: { kind: 'openPullRequests', pullRequests: [pullRequestFixture({ number: 7, title: 'Add the thing', head: 'proj-1-first' })] },
      branch: 'proj-1-first',
    });
    expect(events).toEqual(['opened']);
  });

  it('opens nothing for a branch that heads no open pull request', async () => {
    const { client, controller, events } = await setup();
    expect(await openOn(client, controller, 'proj-2-second', { kind: 'noHeadedPullRequest' })).toBe(false);
    expect(controller.current.get()).toBeUndefined();
    expect(events).toEqual([]);
  });

  it('opens nothing before the board names the project', async () => {
    const { client, controller } = await setup(false);
    expect(await controller.open('proj-1-first')).toBe(false);
    expect(client.calls).toEqual([]);
  });

  it('opens nothing, and says why, when the sidecar cannot be asked which pull request the branch heads', async () => {
    const { client, controller, notices } = await setup();
    const opening = controller.open('proj-1-first');
    client.fail('review/pullRequestOn', new Error('sidecar gone'));
    expect(await opening).toBe(false);
    expect(notices.notice.get()).toBe('sidecar gone');
  });

  it('opens nothing while a review is open', async () => {
    const { client, controller, events } = await setup();
    await openOn(client, controller, 'proj-1-first');
    expect(await controller.open('proj-1-first')).toBe(false);
    expect(events).toEqual(['opened']);
  });

  it('is left by leave, which drops the reads still under way', async () => {
    const { client, controller, events } = await setup();
    await openOn(client, controller, 'proj-1-first');
    const review = controller.current.get();
    controller.leave();
    expect(controller.current.get()).toBeUndefined();
    expect(events).toEqual(['opened', 'left']);
    client.answer('review/diffOf', { kind: 'readDiff', files: [] });
    await Promise.resolve();
    expect(review?.diffState().kind).toBe('loading');
  });

  it('is left when GitHub takes the submit', async () => {
    const { client, controller, events } = await setup();
    await openOn(client, controller, 'proj-1-first');
    const review = controller.current.get();
    const opening = review?.openSummary();
    client.answer('review/summaryToSubmit', '');
    await opening;
    review?.saveBox();
    review?.answerSubmit(true);
    client.answerAll('review/summaryToSubmit', '');
    await SETTLED();
    client.answer('review/submit', { kind: 'writtenReview', pending: { kind: 'noPendingReview' } });
    await SETTLED();
    expect(events).toEqual(['opened', 'left']);
  });

  it('raises what the review refuses as a notice', async () => {
    const { client, controller, notices } = await setup();
    await openOn(client, controller, 'proj-1-first');
    const review = controller.current.get();
    const opening = review?.openSummary();
    client.answer('review/summaryToSubmit', '');
    await opening;
    review?.saveBox();
    review?.answerSubmit(true);
    client.answerAll('review/summaryToSubmit', '');
    await SETTLED();
    client.answer('review/submit', { kind: 'refusedWrite', reason: 'nothing to submit' });
    await SETTLED();
    expect(notices.notice.get()).toBe('not submitted (nothing to submit)');
  });
});
