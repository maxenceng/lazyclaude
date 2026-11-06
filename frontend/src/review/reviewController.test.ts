import { describe, expect, it } from 'vitest';
import { NoticeStore } from '../shared/noticeStore';
import { fakeClient } from '../test/fakeClient';
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

describe('opening a review', () => {
  it('opens the review of the open pull request whose head is the ticket\'s branch, and reads it', async () => {
    const { client, controller, events } = await setup();
    expect(controller.open('proj-1-first')).toBe(true);
    const review = controller.current.get();
    expect(review?.number()).toBe(7);
    expect(review?.title()).toBe('Add the thing');
    expect(client.calls.map((call) => call.method)).toEqual(['review/pendingOf', 'review/diffOf', 'review/threadsOf']);
    expect(events).toEqual(['opened']);
  });

  it('opens nothing for a branch that heads no open pull request', async () => {
    const { controller, events } = await setup();
    expect(controller.open('proj-2-second')).toBe(false);
    expect(controller.current.get()).toBeUndefined();
    expect(events).toEqual([]);
  });

  it('opens nothing before the board names the project', async () => {
    const { controller } = await setup(false);
    expect(controller.open('proj-1-first')).toBe(false);
  });

  it('opens nothing while a review is open', async () => {
    const { controller, events } = await setup();
    controller.open('proj-1-first');
    expect(controller.open('proj-1-first')).toBe(false);
    expect(events).toEqual(['opened']);
  });

  it('is left by leave, which drops the reads still under way', async () => {
    const { client, controller, events } = await setup();
    controller.open('proj-1-first');
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
    controller.open('proj-1-first');
    const review = controller.current.get();
    review?.openSummary();
    review?.saveBox();
    review?.answerSubmit(true);
    client.answer('review/submit', { kind: 'writtenReview', pending: { kind: 'noPendingReview' } });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(events).toEqual(['opened', 'left']);
  });

  it('raises what the review refuses as a notice', async () => {
    const { client, controller, notices } = await setup();
    controller.open('proj-1-first');
    const review = controller.current.get();
    review?.openSummary();
    review?.saveBox();
    review?.answerSubmit(true);
    client.answer('review/submit', { kind: 'refusedWrite', reason: 'nothing to submit' });
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(notices.notice.get()).toBe('not submitted (nothing to submit)');
  });
});
