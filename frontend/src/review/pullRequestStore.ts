import type { Client, ReviewPullRequestsView, ReviewPullRequestView } from '../protocol';
import { Store } from '../shared/store';

/** The open pull requests: `review/watch`'s answer, then every `review/changed` after it. */
export class PullRequestStore {
  readonly state: Store<ReviewPullRequestsView> = new Store<ReviewPullRequestsView>({ kind: 'checkingPullRequests' });

  constructor(private readonly client: Client) {
    client.subscribe('review/changed', (view) => this.state.set(view));
  }

  async watch(root: string): Promise<void> {
    this.state.set({ kind: 'checkingPullRequests' });
    try {
      this.state.set(await this.client.call('review/watch', { root }));
    } catch (error) {
      this.state.set({ kind: 'unavailablePullRequests', reason: error instanceof Error ? error.message : String(error) });
    }
  }

  /** A ticket's pull request, as `review` joins its branch to the open pull requests last pushed; none when no open one has it as head. */
  async forBranch(branch: string): Promise<ReviewPullRequestView | undefined> {
    const answer = await this.client.call('review/pullRequestOn', { pullRequests: this.state.get(), branch });
    return answer.kind === 'headedPullRequest' ? answer.pullRequest : undefined;
  }
}
