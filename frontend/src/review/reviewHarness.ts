import { act } from '@testing-library/react';
import { fakeClient } from '../test/fakeClient';
import { commentsFixture, diffFixture, threadsFixture } from './fixtures';
import { OpenReview } from './openReview';

export interface Reads {
  diff?: unknown;
  threads?: unknown;
  pending?: unknown;
  comments?: unknown;
  summary?: string;
}

export const settle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

export class Harness {
  readonly client = fakeClient();
  readonly refusals: string[] = [];
  submitted = 0;
  readonly review: OpenReview;

  constructor() {
    this.review = new OpenReview({
      client: this.client,
      root: '/p',
      number: 7,
      title: 'Add the thing',
      refuse: (text) => this.refusals.push(text),
      onSubmitted: () => {
        this.submitted += 1;
      },
    });
  }

  methods(): string[] {
    return this.client.calls.map((call) => call.method);
  }

  async answer(method: string, result: unknown): Promise<void> {
    this.client.answer(method, result);
    await settle();
  }

  /** Reads the review and answers everything it asks, the diff last. */
  async readAll(reads: Reads = {}): Promise<void> {
    this.review.read();
    await this.answer('review/pendingOf', reads.pending ?? { kind: 'noPendingReview' });
    await this.answer('review/threadsOf', reads.threads ?? threadsFixture([]));
    await this.answer('review/diffOf', reads.diff ?? diffFixture([]));
    await this.answerPlacement(reads);
  }

  /** Answers every ask still waiting of where the comments show and of the summary to submit; only the latest lands. */
  async answerPlacement(reads: Pick<Reads, 'comments' | 'summary'> = {}): Promise<void> {
    this.client.answerAll('review/commentsInDiff', reads.comments ?? commentsFixture());
    this.client.answerAll('review/summaryToSubmit', reads.summary ?? '');
    await settle();
  }

  /** `S`, answered with what `review` says a submit sends now. */
  async openSummary(summary = ''): Promise<void> {
    const opening = this.review.openSummary();
    this.client.answer('review/summaryToSubmit', summary);
    await opening;
  }

  /** The confirm answered yes, the summary to send asked of `review` and answered with `summary`. */
  async confirmSubmit(summary: string): Promise<void> {
    this.review.answerSubmit(true);
    await this.answerPlacement({ summary });
  }

  async fail(method: string, error: Error): Promise<void> {
    this.client.fail(method, error);
    await settle();
  }
}
