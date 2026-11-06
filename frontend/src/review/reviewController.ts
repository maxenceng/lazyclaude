import type { Client, ReviewPullRequestView } from '../protocol';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';
import { PullRequestStore } from './pullRequestStore';
import { PullRequestSelection } from './pullRequestSelection';
import { TicketPullRequests } from './ticketPullRequests';
import { OpenReview } from './openReview';

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
  readonly ticketPullRequests: TicketPullRequests;
  readonly cursor: PullRequestSelection = new PullRequestSelection();
  readonly current: Store<OpenReview | undefined> = new Store<OpenReview | undefined>(undefined);

  constructor(private readonly deps: ReviewControllerDeps) {
    this.pullRequests = new PullRequestStore(deps.client);
    this.ticketPullRequests = new TicketPullRequests(this.pullRequests);
  }

  isOpen(): boolean {
    return this.current.get() !== undefined;
  }

  /**
   * `p`: opens the review of the open pull request `review` says `branch` heads, and reads it; false when there is none, when the board
   * is not read, when a review is already open or opened while asking, and when the ask failed, whose reason is a notice.
   */
  async open(branch: string): Promise<boolean> {
    const root = this.deps.root();
    if (root === undefined || this.isOpen()) {
      return false;
    }
    const pullRequest = await this.pullRequests.forBranch(branch).catch((error: unknown) => {
      this.deps.notices.raise(error instanceof Error ? error.message : String(error));
      return undefined;
    });
    if (pullRequest === undefined || this.isOpen()) {
      return false;
    }
    return this.openPullRequest(pullRequest);
  }

  /** Opens the review of a pull request as listed, one whose head is no ticket's branch included; false while a review is open or the board is not read. */
  openPullRequest(pullRequest: ReviewPullRequestView): boolean {
    const root = this.deps.root();
    if (root === undefined || this.isOpen()) {
      return false;
    }
    const review = new OpenReview({
      client: this.deps.client,
      root,
      number: pullRequest.number,
      title: pullRequest.title,
      refuse: (text) => this.deps.notices.raise(text),
      onSubmitted: () => this.leave(),
    });
    this.current.set(review);
    review.read();
    this.deps.onOpened();
    return true;
  }

  leave(): void {
    const review = this.current.get();
    if (review !== undefined) {
      review.close();
      this.current.set(undefined);
      this.deps.onLeft();
    }
  }
}
