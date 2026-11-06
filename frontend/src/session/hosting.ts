import type { Terminal } from '@xterm/xterm';
import type { Client, SessionHostStart, SessionListingPayload, SessionPaneSize, SessionSessionPayload, SessionSessionsView, BoardVerb } from '../protocol';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';
import { Dock } from './dock';
import { type Clipboard, Emulator } from './emulator';
import type { ModeMemory } from './modeMemory';
import type { PaneKeyPress } from './paneKeys';

const DEFAULT_PANE: SessionPaneSize = { width: 80, height: 24 };

export interface HostingDeps {
  client: Client;
  memory: ModeMemory;
  sessions: Store<SessionSessionsView | undefined>;
  newTerminal: () => { terminal: Terminal; fit: { fit(): void } };
  reserved: (press: PaneKeyPress) => (() => void) | undefined;
  clipboard: Clipboard;
  notices: NoticeStore;
}

/** A start the sidecar could not answer: what it said, and the same start again. */
export interface StartFailure {
  message: string;
  retry: () => Promise<boolean>;
}

/** The hosted sessions: their tabs, one emulator each, the starts that open them and the pushes that feed and close them. */
export class Hosting {
  readonly dock: Store<Dock> = new Store(new Dock());
  readonly failure: Store<StartFailure | undefined> = new Store<StartFailure | undefined>(undefined);
  readonly mouseReported: Store<boolean> = new Store(false);
  private readonly emulators = new Map<string, Emulator>();
  private readonly ended = new Set<string>();

  constructor(private readonly deps: HostingDeps) {
    deps.client.subscribe('pty/output', (view) => {
      if (!this.ended.has(view.session)) {
        this.emulatorFor(view.session).write(view.data);
      }
    });
    deps.client.subscribe('session/exited', (view) => {
      this.ended.add(view.session);
      this.settle(this.dock.get().closed(view.session));
    });
    deps.sessions.subscribe(() => this.listed());
  }

  emulatorOf(session: string): Emulator | undefined {
    return this.emulators.get(session);
  }

  select(session: string): void {
    this.dock.set(this.dock.get().select(session));
    this.refreshMouse();
  }

  selectPrevious(): boolean {
    const previous = this.dock.get().previous();
    if (previous !== undefined) {
      this.dock.set(previous);
      this.refreshMouse();
    }
    return previous !== undefined;
  }

  selectNext(): boolean {
    const before = this.dock.get().selectedTab();
    this.dock.set(this.dock.get().next());
    this.refreshMouse();
    return this.dock.get().selectedTab() !== before;
  }

  hasTabs(): boolean {
    return this.dock.get().tabs.length > 0;
  }

  startUnlinked(root: string): Promise<boolean> {
    return this.start(() => this.deps.client.call('session/startUnlinked', { root, size: this.paneSize() }));
  }

  startTicket(root: string, ticket: string, branch: string, verb: BoardVerb): Promise<boolean> {
    return this.start(() => this.deps.client.call('session/startTicket', { root, ticket, branch, verb, size: this.paneSize() }));
  }

  /** Resumes a past session from its row; `session` decides whether in the ticket's worktree or where it ran. */
  resumePast(sessions: SessionSessionPayload, session: string, recordedDirectory: string, listing: SessionListingPayload): Promise<boolean> {
    return this.start(() => this.deps.client.call('session/resumePast', { sessions, session, recordedDirectory, listing, size: this.paneSize() }));
  }

  /** A call that should have led to a start failed: the screen shows `message`, and `retry` makes the call again. */
  failed(message: string, retry: () => Promise<boolean>): void {
    this.failure.set({ message, retry });
  }

  private emulatorFor(session: string): Emulator {
    const existing = this.emulators.get(session);
    if (existing !== undefined) {
      return existing;
    }
    const { terminal, fit } = this.deps.newTerminal();
    const { client, memory, reserved, clipboard, notices } = this.deps;
    const emulator = new Emulator(session, terminal, { client, memory, fit, reserved, clipboard, notices });
    this.emulators.set(session, emulator);
    emulator.mouseReported.subscribe(() => this.refreshMouse());
    return emulator;
  }

  private refreshMouse(): void {
    const selected = this.dock.get().selectedTab();
    this.mouseReported.set(selected !== undefined && this.emulators.get(selected)?.mouseReported.get() === true);
  }

  private listed(): void {
    const view = this.deps.sessions.get();
    if (view !== undefined) {
      this.settle(this.dock.get().listing(view.hosted.map((hosted) => hosted.id)));
    }
  }

  private settle(next: Dock): void {
    const before = this.dock.get().tabs;
    this.dock.set(next);
    next.tabs.forEach((session) => this.emulatorFor(session));
    before
      .filter((session) => !next.tabs.includes(session))
      .forEach((session) => {
        this.emulators.get(session)?.end();
        this.emulators.delete(session);
      });
    this.refreshMouse();
  }

  private paneSize(): SessionPaneSize {
    const fitted = [...this.emulators.values()].find((emulator) => emulator.terminal.element !== undefined);
    return fitted === undefined ? DEFAULT_PANE : { width: fitted.terminal.cols, height: fitted.terminal.rows };
  }

  private opened(start: SessionHostStart): boolean {
    if (start.kind !== 'startedHost') {
      return false;
    }
    this.ended.delete(start.id);
    this.settle(this.dock.get().opened(start.id));
    return true;
  }

  private async start(ask: () => Promise<SessionHostStart>): Promise<boolean> {
    try {
      const started = this.opened(await ask());
      this.failure.set(undefined);
      return started;
    } catch (error) {
      this.failed(error instanceof Error ? error.message : String(error), () => this.start(ask));
      return false;
    }
  }
}
