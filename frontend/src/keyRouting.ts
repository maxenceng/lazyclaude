import type { BoardController } from './board/boardController';
import type { DocsController } from './docs/docsController';
import type { ReviewController } from './review/reviewController';
import type { OpenReview } from './review/openReview';
import { Action } from './keys/bindings';
import { Focus, type Layout, WindowTab } from './layout';
import type { WindowTabs } from './windowTabs';
import type { SessionsController } from './session/sessionsController';
import type { Store } from './shared/store';

/** The keys a pane takes while it has focus and claude has the keyboard: only these move out of it. */
const FOCUS_KEYS: readonly Action[] = [
  Action.FocusLeft,
  Action.FocusRight,
  Action.FocusDown,
  Action.FocusUp,
  Action.ToggleMainView,
  Action.Zoom,
  Action.NextWaiting,
];

const TABS: ReadonlyMap<Action, WindowTab> = new Map([
  [Action.ShowBase, WindowTab.Base],
  [Action.ShowBoard, WindowTab.Board],
  [Action.ShowPullRequests, WindowTab.PullRequests],
]);

/** Where a key goes: to the verb picker while it is open, to the focused pane's own handling, or to the layout. */
export class KeyRouter {
  constructor(
    private readonly board: BoardController,
    private readonly docs: DocsController,
    private readonly sessions: SessionsController,
    private readonly layout: Store<Layout>,
    private readonly review: ReviewController,
    private readonly tabs: WindowTabs,
  ) {
    layout.subscribe(() => {
      const focus = layout.get().focus;
      if (focus === Focus.Files || focus === Focus.Comments) {
        this.lastLeft = focus;
      }
    });
  }

  private lastLeft: Focus = Focus.Files;

  /** A key the window-level handler took: whatever the focused pane does with it. */
  press(action: Action): void {
    const current = this.review.current.get();
    if (this.sessions.isPicking()) {
      this.pressInPicker(action);
    } else if (this.focus() === Focus.Dock && this.sessions.hosting.hasTabs()) {
      if (FOCUS_KEYS.includes(action)) {
        this.pressInPane(action);
      }
    } else if (current !== undefined) {
      this.pressInReview(current, action);
    } else {
      this.pressInWindow(action);
    }
  }

  /** A key outside every pane that takes its own: a digit chooses a tab, and the tab shown takes the rest. */
  private pressInWindow(action: Action): void {
    const chosen = TABS.get(action);
    if (chosen !== undefined) {
      this.tabs.choose(chosen);
    } else if (this.tabs.selected.get() === WindowTab.Base) {
      this.pressOutsidePane(action);
    } else {
      this.tabs.press(action);
    }
  }

  /** A reserved chord pressed inside a pane, as the TUI's pane acts on it. */
  pressInPane(action: Action): void {
    switch (action) {
      case Action.FocusLeft:
        if (!this.sessions.hosting.selectPrevious()) {
          this.change((current) => current.focusOn(this.review.isOpen() ? this.lastLeft : Focus.Board));
        }
        break;
      case Action.FocusRight:
        this.sessions.hosting.selectNext();
        break;
      case Action.FocusUp:
        this.change((current) => current.goUp(this.review.isOpen() ? Focus.Diff : Focus.Sessions));
        break;
      case Action.ToggleMainView:
        this.change((current) => current.toggleMainView());
        break;
      case Action.Zoom:
        this.change((current) => current.toggleZoom());
        break;
      case Action.NextWaiting:
        this.change((current) => current.leaveZoom());
        break;
      default:
        break;
    }
  }

  /** Opens the row under the sessions' cursor, or moves focus to the sessions of the ticket under the Board's. */
  open(): void {
    if (this.focus() === Focus.Docs) {
      this.docs.activate();
    } else if (this.focus() === Focus.Sessions) {
      this.sessions.activate();
    } else if (this.board.selectedTicket() !== undefined) {
      this.sessions.moveTo(0);
      this.change((current) => current.focusOn(Focus.Sessions));
    }
  }

  /** `p` on the Board or a ticket's sessions: the review of the selected ticket's pull request, if its branch heads an open one. */
  private openReview(): void {
    const ticket = this.board.selectedTicket();
    if ((this.focus() === Focus.Board || this.focus() === Focus.Sessions) && ticket !== undefined) {
      this.lastLeft = Focus.Files;
      void this.review.open(ticket.branch);
    }
  }

  /** The keys of an open review: the confirm after the summary answers first, a comment box takes none, then a digit choosing a tab, then the focused panel's. */
  private pressInReview(current: OpenReview, action: Action): void {
    if (current.submitPrompt()) {
      current.answerSubmit(action === Action.Yes);
    } else if (current.box() === undefined) {
      this.pressInReviewPanel(current, action);
    }
  }

  private pressInReviewPanel(current: OpenReview, action: Action): void {
    const focus = this.focus();
    const chosen = TABS.get(action);
    if (chosen !== undefined) {
      return this.tabs.choose(chosen);
    }
    switch (action) {
      case Action.Back:
        if (current.clearRange()) {
          return;
        }
        return this.layout.get().zoomed ? this.change((current) => current.leaveZoom()) : this.review.leave();
      case Action.Refresh:
        return current.read();
      case Action.Submit:
        return void current.openSummary();
      case Action.Resolved:
        return current.toggleResolved();
      case Action.Open:
        return this.enterInReview(current, focus);
      case Action.MoveDown:
        return this.moveInReview(current, focus, 1);
      case Action.MoveUp:
        return this.moveInReview(current, focus, -1);
      case Action.Capture:
        return focus === Focus.Comments ? current.commentOnSelectedComment() : focus === Focus.Diff ? void current.comment() : undefined;
      case Action.Delete:
        return focus === Focus.Comments ? current.deleteSelectedComment() : focus === Focus.Diff ? current.deleteAtCursor() : undefined;
      case Action.Range:
        return focus === Focus.Diff ? void current.markRangeKey() : undefined;
      case Action.NextFile:
        return focus === Focus.Diff ? current.nextFile() : undefined;
      case Action.PreviousFile:
        return focus === Focus.Diff ? current.previousFile() : undefined;
      case Action.HalfPageDown:
        return focus === Focus.Diff ? current.halfPage(true) : undefined;
      case Action.HalfPageUp:
        return focus === Focus.Diff ? current.halfPage(false) : undefined;
      default:
        return this.moveFocusInReview(action);
    }
  }

  private enterInReview(current: OpenReview, focus: Focus): void {
    if (focus === Focus.Diff) {
      current.enterInDiff();
    } else if (focus === Focus.Files) {
      current.goToSelectedFile();
    } else if (focus === Focus.Comments) {
      current.enterOnComment();
    }
  }

  private moveInReview(current: OpenReview, focus: Focus, delta: number): void {
    if (focus === Focus.Diff) {
      current.moveLine(delta);
    } else if (focus === Focus.Files) {
      current.selectFile(current.selectedFile() + delta);
    } else if (focus === Focus.Comments) {
      current.moveComment(delta);
    }
  }

  private moveFocusInReview(action: Action): void {
    const focus = this.focus();
    switch (action) {
      case Action.PanelLeft:
      case Action.FocusLeft:
        return this.focusOnReview(this.lastLeft);
      case Action.PanelRight:
      case Action.FocusRight:
        return this.focusOnReview(Focus.Diff);
      case Action.FocusDown:
        if (focus === Focus.Files) {
          this.focusOnReview(Focus.Comments);
        } else if (focus === Focus.Diff && this.sessions.hosting.hasTabs()) {
          this.change((current) => current.focusOn(Focus.Dock));
        }
        return;
      case Action.FocusUp:
        if (focus === Focus.Comments) {
          this.focusOnReview(Focus.Files);
        }
        return;
      case Action.Zoom:
        return this.change((current) => current.toggleZoom());
      case Action.ToggleMainView:
        return this.change((current) => current.toggleMainView());
      case Action.NextWaiting:
        return this.change((current) => current.leaveZoom());
      default:
        return undefined;
    }
  }

  private focusOnReview(target: Focus): void {
    this.change((current) => current.focusOn(target));
  }

  private pressInPicker(action: Action): void {
    switch (action) {
      case Action.MoveDown:
        return this.sessions.pickDown();
      case Action.MoveUp:
        return this.sessions.pickUp();
      case Action.Back:
        return this.sessions.cancelPick();
      case Action.Open:
        return void this.sessions.pick();
      default:
        return undefined;
    }
  }

  private pressOutsidePane(action: Action): void {
    switch (action) {
      case Action.MoveDown:
        return this.move(1);
      case Action.MoveUp:
        return this.move(-1);
      case Action.Open:
        return this.open();
      case Action.Review:
        return this.openReview();
      case Action.NewTicketSession:
        return void this.sessions.newSession();
      case Action.NewSession:
        return void this.sessions.newUnlinked();
      case Action.Back:
        return this.change((current) => (current.zoomed || current.focus === Focus.Board ? current.leaveZoom() : current.focusOn(Focus.Board)));
      case Action.PanelLeft:
      case Action.FocusLeft:
        return this.change((current) => current.focusOn(Focus.Board));
      case Action.PanelRight:
      case Action.FocusRight:
        return this.change((current) => current.focusOn(Focus.Sessions));
      case Action.FocusDown:
        return this.focusDown();
      case Action.FocusUp:
        return this.change((current) => current.goUp(current.focus === Focus.Dock ? Focus.Sessions : Focus.Board));
      case Action.Zoom:
        return this.change((current) => current.toggleZoom());
      case Action.ToggleMainView:
        return this.change((current) => current.toggleMainView());
      case Action.NextWaiting:
        return this.change((current) => current.leaveZoom());
      default:
        return undefined;
    }
  }

  private focusDown(): void {
    if (this.focus() === Focus.Board) {
      this.change((current) => current.focusOn(Focus.Docs));
    } else if (this.sessions.hosting.hasTabs()) {
      this.change((current) => current.focusOn(Focus.Dock));
    }
  }

  private move(delta: number): void {
    if (this.focus() === Focus.Docs) {
      this.docs.move(delta);
    } else if (this.focus() === Focus.Sessions) {
      this.sessions.move(delta);
    } else if (this.board.move(delta)) {
      this.sessions.moveTo(0);
    }
  }

  private focus(): Focus {
    return this.layout.get().focus;
  }

  private change(next: (current: Layout) => Layout): void {
    this.layout.set(next(this.layout.get()));
  }
}
