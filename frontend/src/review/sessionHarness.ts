import { act } from '@testing-library/react';
import { fakeClient } from '../test/fakeClient';
import { diffFixture, threadsFixture } from './fixtures';
import { ReviewSession } from './reviewSession';

export interface Reads {
  diff?: unknown;
  threads?: unknown;
  pending?: unknown;
  placedThreads?: unknown;
  placedPending?: unknown;
}

export const settle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

export class Harness {
  readonly client = fakeClient();
  readonly refusals: string[] = [];
  submitted = 0;
  readonly session: ReviewSession;

  constructor() {
    this.session = new ReviewSession({
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
    this.session.read();
    await this.answer('review/pendingOf', reads.pending ?? { kind: 'noPendingReview' });
    await this.answer('review/threadsOf', reads.threads ?? threadsFixture([]));
    await this.answer('review/diffOf', reads.diff ?? diffFixture([]));
    await this.answer('review/threadsInDiff', reads.placedThreads ?? []);
    await this.answer('review/pendingInDiff', reads.placedPending ?? []);
  }

  async fail(method: string, error: Error): Promise<void> {
    this.client.fail(method, error);
    await settle();
  }
}
