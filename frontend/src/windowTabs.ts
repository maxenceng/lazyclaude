import type { BoardController } from './board/boardController';
import { cursorTicketInColumns } from './board/rows';
import { Action } from './keys/bindings';
import { Focus, type Layout, WindowTab } from './layout';
import type { BoardTicketView, ReviewPullRequestView } from './protocol';
import type { ReviewController } from './review/reviewController';
import { worstFirst } from './review/pullRequestList';
import { Store } from './shared/store';

interface Deps {
  board: BoardController;
  review: ReviewController;
  layout: Store<Layout>;
}

/** Which tab the window shows, and the keys of the Board and Pull requests tabs. The window opens on Base, and remembers no tab. */
export class WindowTabs {
  readonly selected: Store<WindowTab> = new Store<WindowTab>(WindowTab.Base);

  constructor(private readonly deps: Deps) {}

  /** The tab is shown, ending an open review as `esc` would; a tab other than Base draws over the panes, so focus goes to the Board and the dock's zoom ends. Nothing while the review's comment box or submit prompt is open, since either holds what is not written yet. */
  choose(tab: WindowTab): void {
    const review = this.deps.review.current.get();
    if (review !== undefined && (review.box() !== undefined || review.submitPrompt())) {
      return;
    }
    this.deps.review.leave();
    this.selected.set(tab);
    if (tab !== WindowTab.Base) {
      this.deps.layout.set(this.deps.layout.get().focusOn(Focus.Board));
    }
    this.pullRequestsChanged();
  }

  /** The open pull requests as the Pull requests tab lists them: failed first, none while they are not read. */
  pullRequests(): ReviewPullRequestView[] {
    const state = this.deps.review.pullRequests.state.get();
    return state.kind === 'openPullRequests' ? worstFirst(state.pullRequests) : [];
  }

  /** The Board tab draws each ticket's pull request and Base names the selected one's, so either asks `review` for them when it is shown and whenever they or the tickets change. */
  pullRequestsChanged(): void {
    if (this.selected.get() !== WindowTab.PullRequests) {
      void this.deps.review.ticketPullRequests.join(this.deps.board.branches());
    }
  }

  /** A click on a card of the Board tab: the card selected and previewed, or, already selected, gone to on Base. */
  pressTicket(ticket: BoardTicketView, cursor: string | undefined): void {
    if (ticket.id === cursor) {
      this.openTicket(ticket.id);
    } else {
      this.deps.board.selection.select(ticket.id);
    }
  }

  /** Enter, or a click on the card already selected, on the Board tab: back to Base with that ticket selected. */
  openTicket(id: string): void {
    this.deps.board.selection.select(id);
    this.selected.set(WindowTab.Base);
    this.deps.layout.set(this.deps.layout.get().focusOn(Focus.Board));
  }

  /** Enter or a click on a row of the Pull requests tab: the Review screen on that pull request, the tab and the row kept for when it ends. */
  openPullRequest(pullRequest: ReviewPullRequestView): void {
    this.deps.review.cursor.select(pullRequest.number);
    this.deps.review.openPullRequest(pullRequest);
  }

  /** A key pressed on the Board or Pull requests tab. */
  press(action: Action): void {
    if (action === Action.Back) {
      this.selected.set(WindowTab.Base);
    } else if (this.selected.get() === WindowTab.Board) {
      this.pressOnBoard(action);
    } else {
      this.pressOnPullRequests(action);
    }
  }

  private pressOnBoard(action: Action): void {
    const view = this.deps.board.shown();
    const selection = this.deps.board.selection;
    if (action === Action.Capture) {
      return this.deps.board.capture.open();
    }
    if (view === undefined) {
      return;
    }
    switch (action) {
      case Action.MoveDown:
        return selection.moveInColumn(view, 1);
      case Action.MoveUp:
        return selection.moveInColumn(view, -1);
      case Action.PanelRight:
        return selection.moveAcross(view, 1);
      case Action.PanelLeft:
        return selection.moveAcross(view, -1);
      case Action.Open:
        return this.openCursorTicket();
      default:
        return undefined;
    }
  }

  private openCursorTicket(): void {
    const view = this.deps.board.shown();
    const ticket = view === undefined ? undefined : cursorTicketInColumns(view, this.deps.board.selection.selected.get());
    if (ticket !== undefined) {
      this.openTicket(ticket.id);
    }
  }


  private pressOnPullRequests(action: Action): void {
    const list = this.pullRequests();
    const cursor = this.deps.review.cursor;
    switch (action) {
      case Action.MoveDown:
        return cursor.move(list, 1);
      case Action.MoveUp:
        return cursor.move(list, -1);
      case Action.Open: {
        const under = cursor.cursorIn(list);
        return under === undefined ? undefined : this.openPullRequest(under);
      }
      default:
        return undefined;
    }
  }
}
