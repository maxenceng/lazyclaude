import { BoardController } from './board/boardController';
import { DocsController } from './docs/docsController';
import type { Action, KeyPress } from './keys/bindings';
import type { Binding } from './keys/chords';
import { ConfigurationStore } from './keys/configurationStore';
import { KeyRouter } from './keyRouting';
import { Focus, Layout } from './layout';
import type { Platform } from './platform';
import type { Client } from './protocol';
import { Hosting } from './session/hosting';
import { SessionsController } from './session/sessionsController';
import { SessionStore } from './session/sessionStore';
import { WatchingReporter } from './session/watched';
import { Store } from './shared/store';
import { WarningStore } from './shared/warningStore';
import { WindowFocus } from './window/windowFocus';

/** One window: the contexts it composes, the layout over them, and the rule that loads them whenever the sidecar becomes ready. */
export class AppModel {
  readonly board: BoardController;
  readonly docs: DocsController;
  readonly sessionStore: SessionStore;
  readonly hosting: Hosting;
  readonly sessions: SessionsController;
  readonly configuration: ConfigurationStore;
  readonly warnings: WarningStore;
  readonly layout: Store<Layout> = new Store(new Layout());
  readonly windowFocus: WindowFocus;
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
    this.docs = new DocsController({ client, root: () => this.board.root(), opener: platform.opener, warn: (text) => this.warnings.raise(text), onOpened: () => this.focusOn(Focus.Docs) });
    this.windowFocus = new WindowFocus(platform.focus);
    this.watching = new WatchingReporter(client);
    this.hosting = new Hosting({
      client,
      memory: platform.memory,
      sessions: this.sessionStore.sessions,
      newTerminal: () => platform.newTerminal(),
      clipboard: platform.clipboard,
      reserved: (press) => this.reservedInPane(press),
    });
    this.sessions = new SessionsController({
      client,
      store: this.sessionStore,
      hosting: this.hosting,
      project: () => this.project(),
      ticket: () => this.board.selectedTicket(),
      onOpened: () => this.focusOn(Focus.Dock),
      onPicking: () => this.layout.set(this.layout.get().leaveZoom()),
    });
    this.keys = new KeyRouter(this.board, this.docs, this.sessions, this.layout);
    this.hosting.dock.subscribe(() => this.dockChanged());
    this.layout.subscribe(() => this.tellWatching());
    this.hosting.dock.subscribe(() => this.tellWatching());
    this.windowFocus.focused.subscribe(() => this.tellWatching());
    this.board.store.state.subscribe(() => this.boardRead());
  }

  reserved(): readonly Binding[] {
    return this.configuration.reserved();
  }

  press(action: Action): void {
    this.keys.press(action);
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
    if (!this.hosting.hasTabs() && this.layout.get().focus === Focus.Dock) {
      this.focusOn(Focus.Board);
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
  }
}
