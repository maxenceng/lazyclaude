import type { Client } from '../protocol';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';
import { PullRequestStore } from './pullRequestStore';
import { ReviewSession } from './reviewSession';

export interface ReviewControllerDeps {
  client: Client;
  /** The project's root, none before the board is read. */
  root: () => string | undefined;
  notices: NoticeStore;
  /** A review opened, so its panels are to replace the Board and the main view. */
  onOpened: () => void;
  /** The review ended, by `esc` or a submit. */
  onLeft: () => void;
}

/** The Review: the pull requests that can be reviewed, and the review that is open, if one is. */
export class ReviewController {
  readonly pullRequests: PullRequestStore;
  readonly session: Store<ReviewSession | undefined> = new Store<ReviewSession | undefined>(undefined);

  constructor(private readonly deps: ReviewControllerDeps) {
    this.pullRequests = new PullRequestStore(deps.client);
  }

  isOpen(): boolean {
    return this.session.get() !== undefined;
  }

  /** `p`: opens the review of the open pull request whose head is `branch`, and reads it; false when there is none. */
  open(branch: string): boolean {
    const root = this.deps.root();
    const pullRequest = this.pullRequests.forBranch(branch);
    if (root === undefined || pullRequest === undefined || this.isOpen()) {
      return false;
    }
    const session = new ReviewSession({
      client: this.deps.client,
      root,
      number: pullRequest.number,
      title: pullRequest.title,
      refuse: (text) => this.deps.notices.raise(text),
      onSubmitted: () => this.leave(),
    });
    this.session.set(session);
    session.read();
    this.deps.onOpened();
    return true;
  }

  leave(): void {
    const session = this.session.get();
    if (session !== undefined) {
      session.close();
      this.session.set(undefined);
      this.deps.onLeft();
    }
  }
}
