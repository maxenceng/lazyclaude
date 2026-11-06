import type { ReviewPullRequestView } from '../protocol';
import { Store } from '../shared/store';

/** The pull request the Pull requests tab's cursor is on, held by number so a changed list keeps it where it can. */
export class PullRequestSelection {
  readonly selected: Store<number | undefined> = new Store<number | undefined>(undefined);

  select(number: number): void {
    this.selected.set(number);
  }

  /** The row under the cursor: the selected pull request while it is listed, else the first. */
  cursorIn(list: readonly ReviewPullRequestView[]): ReviewPullRequestView | undefined {
    return list.find((pullRequest) => pullRequest.number === this.selected.get()) ?? list[0];
  }

  move(list: readonly ReviewPullRequestView[], delta: number): void {
    const cursor = this.cursorIn(list);
    if (cursor !== undefined) {
      this.selected.set(list[Math.min(list.length - 1, Math.max(0, list.indexOf(cursor) + delta))]?.number);
    }
  }
}
