import { BoardController } from './board/boardController';
import { i18n } from './i18n/i18n';
import { DocsController } from './docs/docsController';
import { Action, type KeyPress } from './keys/bindings';
import type { Binding } from './keys/chords';
import { ConfigurationStore } from './keys/configurationStore';
import { KeyRouter } from './keyRouting';
import { Focus, Layout, type Panel, WindowTab } from './layout';
import { WindowTabs } from './windowTabs';
import type { Platform } from './platform';
import type { Client } from './protocol';
import { ReviewController } from './review/reviewController';
import { Hosting } from './session/hosting';
import { SessionsController } from './session/sessionsController';
import { SessionStore } from './session/sessionStore';
import { WatchingReporter } from './session/watched';
import { Store } from './shared/store';
import { NoticeStore } from './shared/noticeStore';
import { WarningStore } from './shared/warningStore';
import { WindowFocus } from './window/windowFocus';
import { Quitting, hostedOf } from './quitting';

/** One window: the contexts it composes, the layout over them, and the rule that loads them whenever the sidecar becomes ready. */
export class AppModel {
  readonly board: BoardController;
  readonly docs: DocsController;
  readonly review: ReviewController;
  readonly sessionStore: SessionStore;
  readonly hosting: Hosting;
  readonly sessions: SessionsController;
  readonly configuration: ConfigurationStore;
  readonly warnings: WarningStore;
  readonly notices: NoticeStore = new NoticeStore();
  readonly layout: Store<Layout> = new Store(new Layout());
  readonly windowFocus: WindowFocus;
  readonly tabs: WindowTabs;
  readonly quitting: Quitting;
  private readonly keys: KeyRouter;
  private readonly watching: WatchingReporter;
  private watched = false;

  constructor(
    readonly client: Client,
    platform: Platform,
  ) {
    this.board = new BoardController(client);
    this.sessionStore = new SessionStore(client);
    this.configuration = new ConfigurationStore(client);
    this.warnings = new WarningStore(client);
    this.docs = new DocsController({ client, root: () => this.board.root(), opener: platform.opener, notices: this.notices, onOpened: () => this.focusOn(Focus.Docs) });
    this.review = new ReviewController({
      client,
      root: () => this.board.root(),
      notices: this.notices,
      onOpened: () => this.layout.set(this.layout.get().goUp(Focus.Diff)),
      onLeft: () => this.focusOn(Focus.Board),
    });
    this.windowFocus = new WindowFocus(platform.focus);
    this.watching = new WatchingReporter(client);
    this.hosting = new Hosting({
      client,
      memory: platform.memory,
      sessions: this.sessionStore.sessions,
      newTerminal: () => platform.newTerminal(),
      clipboard: platform.clipboard,
      notices: this.notices,
      reserved: (press) => this.reservedInPane(press),
    });
    this.sessions = new SessionsController({
      client,
      store: this.sessionStore,
      hosting: this.hosting,
      project: () => this.project(),
      ticket: () => this.board.selectedTicket(),
      onOpened: () => this.focusOn(Focus.Dock),
      onPicking: () => this.layout.set(this.layout.get().showTicket()),
    });
    this.tabs = new WindowTabs({ board: this.board, review: this.review, layout: this.layout });
    this.quitting = new Quitting({
      hosted: () => hostedOf(this.sessionStore.sessions.get(), this.hosting.dock.get().tabs.length),
      closeWindow: () => platform.closeWindow(),
      refused: (reason) => this.notices.raise(i18n.t('common:quit.refused', { reason })),
    });
    this.keys = new KeyRouter(this.board, this.docs, this.sessions, this.layout, this.review, this.tabs, this.quitting);
    this.hosting.dock.subscribe(() => this.dockChanged());
    this.layout.subscribe(() => this.tellWatching());
    this.hosting.dock.subscribe(() => this.tellWatching());
    this.windowFocus.focused.subscribe(() => this.tellWatching());
    this.board.store.state.subscribe(() => this.boardRead());
    this.board.store.state.subscribe(() => this.tabs.pullRequestsChanged());
    this.review.pullRequests.state.subscribe(() => this.tabs.pullRequestsChanged());
    this.board.selection.selected.subscribe(() => this.notices.clear());
    this.board.selection.selected.subscribe(() => this.sessions.followSelection());
  }

  reserved(): readonly Binding[] {
    return this.configuration.reserved();
  }

  /** The chord that zooms the dock from inside it, as `config.yml` binds it, for the dock's zoom button; none while no configuration is read. */
  zoomKey(): string | undefined {
    return this.reserved()
      .find((binding) => binding.action === Action.Zoom)
      ?.label();
  }

  /** Whether a popup is open that takes every key and click, so nothing behind it answers either. */
  isModal(): boolean {
    return this.keys.isModal();
  }

  /** The status line's Quit button: what `q` does. */
  quit(): void {
    this.quitting.request();
  }

  /** The capture popup's button, in the Board pane's title: the Board takes focus and `c` is done there. */
  capture(): void {
    this.focusOn(Focus.Board);
    this.board.capture.open();
  }

  press(action: Action): void {
    if (this.review.isOpen()) {
      this.notices.clear();
    }
    this.keys.press(action);
  }

  /** A click on a window tab. */
  chooseTab(tab: WindowTab): void {
    this.tabs.choose(tab);
  }

  /** A click on a session's tab in the dock folded under a window tab: back to Base, with that session's pane focused. */
  openDockTab(id: string): void {
    this.hosting.select(id);
    this.tabs.choose(WindowTab.Base);
    this.focusOn(Focus.Dock);
  }

  /** A click on a session's tab in the minimised dock strip: the dock unfolds with that session's pane focused, on whatever the window shows. */
  focusDockTab(id: string): void {
    this.hosting.select(id);
    this.focusOn(Focus.Dock);
  }

  /** The dock's zoom button: the reserved zoom chord's, the dock taking focus first from any other pane; zoomed already, the zoom ends. */
  toggleZoom(): void {
    this.layout.set(this.layout.get().zoomDock());
  }

  /** The dock's minimise or restore button: the dock folds to its strip, or unfolds. */
  toggleDockMinimised(): void {
    this.keys.toggleDockMinimised();
  }

  /** The close button of a shown document: the main view goes back to the selected ticket and focus goes to it. */
  closeDocument(): void {
    this.layout.set(this.layout.get().closeDocument());
  }

  /** The ticket's hide button: the main view hides and the dock takes the column, as `m` does; `m` shows it again. */
  hideMainView(): void {
    this.layout.set(this.layout.get().hideMainView());
  }

  /** Retry on the Pull requests tab: reads the open pull requests again. */
  retryPullRequests(): void {
    const root = this.board.root();
    if (root !== undefined) {
      void this.review.pullRequests.watch(root);
    }
  }

  /** A click on a panel tab of the left column: that tab is shown and takes focus. */
  showPanel(panel: Panel): void {
    this.layout.set(this.layout.get().showPanel(panel));
  }

  focusOn(focus: Focus): void {
    this.layout.set(this.layout.get().focusOn(focus));
  }

  /** A row clicked in the ticket's sessions: the cursor goes to it and it opens. */
  pressRow(index: number): void {
    this.sessions.moveTo(index);
    this.focusOn(Focus.Sessions);
    this.sessions.activate();
  }

  async load(): Promise<void> {
    this.watched = false;
    this.watching.forget();
    this.tellWatching();
    const loading = this.board.load();
    void this.configuration.load();
    await loading;
  }

  start(): void {
    let ready = this.client.status.get().isReady();
    if (ready) {
      this.ready();
    }
    this.client.status.subscribe(() => {
      const nowReady = this.client.status.get().isReady();
      if (nowReady && !ready) {
        this.ready();
      }
      ready = nowReady;
    });
  }

  private ready(): void {
    void this.load();
    void this.warnings.watch();
  }

  private project(): { root: string; ticketIds: string[] } | undefined {
    const root = this.board.root();
    return root === undefined ? undefined : { root, ticketIds: this.board.ticketIds() };
  }

  private reservedInPane(press: KeyPress): (() => void) | undefined {
    const binding = this.reserved().find((candidate) => candidate.matches(press));
    return binding === undefined ? undefined : () => this.keys.pressInPane(binding.action);
  }

  private dockChanged(): void {
    if (!this.hosting.hasTabs() && this.layout.get().dockMinimised) {
      this.layout.set(this.layout.get().restoreDock());
    }
    if (!this.hosting.hasTabs() && this.layout.get().focus === Focus.Dock) {
      this.focusOn(this.review.isOpen() ? Focus.Diff : Focus.Board);
    }
  }

  private tellWatching(): void {
    const paneFocused = this.layout.get().focus === Focus.Dock && this.hosting.hasTabs();
    this.watching.tell(this.hosting.dock.get().selectedTab(), paneFocused, this.windowFocus.focused.get());
  }

  private boardRead(): void {
    const project = this.project();
    if (this.watched || project === undefined) {
      return;
    }
    this.watched = true;
    // deferred: the session watch links sessions to the tickets known now, not to ones created later — close it when a new ticket's session shows no lamp.
    void this.sessionStore.watch(project.root, project.ticketIds);
    void this.docs.load();
    void this.review.pullRequests.watch(project.root);
  }
}
