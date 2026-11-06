import type { ReviewPullRequestView } from '../protocol';
import { Store } from '../shared/store';
import type { PullRequestStore } from './pullRequestStore';

/** The open pull request each ticket branch heads, as `review` joins them; asked for when a screen shows them, none before. */
export class TicketPullRequests {
  readonly joined: Store<ReadonlyMap<string, ReviewPullRequestView>> = new Store<ReadonlyMap<string, ReviewPullRequestView>>(new Map());
  private generation = 0;

  constructor(private readonly pullRequests: PullRequestStore) {}

  of(branch: string): ReviewPullRequestView | undefined {
    return this.joined.get().get(branch);
  }

  /** Asks `review` for each branch's pull request; the answers of a join a later one overtook are dropped. */
  async join(branches: readonly string[]): Promise<void> {
    this.generation += 1;
    const mine = this.generation;
    const asked = await Promise.all(
      branches.map(async (branch) => [branch, await this.pullRequests.forBranch(branch).catch(() => undefined)] as const),
    );
    if (mine === this.generation) {
      this.joined.set(new Map(asked.filter((entry): entry is readonly [string, ReviewPullRequestView] => entry[1] !== undefined)));
    }
  }
}
