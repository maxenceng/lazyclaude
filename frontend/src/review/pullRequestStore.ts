import type { Client, PullRequestsView, PullRequestView } from '../protocol';
import { Store } from '../shared/store';

/** The open pull requests: `review/watch`'s answer, then every `review/changed` after it. */
export class PullRequestStore {
  readonly state: Store<PullRequestsView> = new Store<PullRequestsView>({ kind: 'checkingPullRequests' });

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

  /** A ticket's pull request is the open one whose head is its ticket branch. */
  forBranch(branch: string): PullRequestView | undefined {
    const view = this.state.get();
    return view.kind === 'openPullRequests' ? view.pullRequests.find((pullRequest) => pullRequest.head === branch) : undefined;
  }
}
