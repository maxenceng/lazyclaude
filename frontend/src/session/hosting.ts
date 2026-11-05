import type { Terminal } from '@xterm/xterm';
import type { Client, HostStart, PaneSize, SessionsView, Verb } from '../protocol';
import { createStore, type Store } from '../shared/store';
import { Dock } from './dock';
import { type Clipboard, Emulator } from './emulator';
import type { ModeMemory } from './modeMemory';
import type { PaneKeyPress } from './paneKeys';

const DEFAULT_PANE: PaneSize = { width: 80, height: 24 };

export interface HostingDeps {
  client: Client;
  memory: ModeMemory;
  sessions: Store<SessionsView | undefined>;
  newTerminal: () => { terminal: Terminal; fit: { fit(): void } };
  reserved: (press: PaneKeyPress) => (() => void) | undefined;
  clipboard: Clipboard;
}

/** A start the sidecar could not answer: what it said, and the same start again. */
export interface StartFailure {
  message: string;
  retry: () => Promise<void>;
}

export interface Hosting {
  readonly dock: Store<Dock>;
  readonly failure: Store<StartFailure | undefined>;
  emulatorOf(session: string): Emulator | undefined;
  select(session: string): void;
  selectPrevious(): boolean;
  selectNext(): boolean;
  startUnlinked(root: string): Promise<void>;
  startTicket(root: string, ticket: string, branch: string, verb: Verb): Promise<void>;
  resume(root: string, session: string, recordedDirectory: string): Promise<void>;
  resumeInTicketWorktree(root: string, session: string, recordedDirectory: string, branch: string): Promise<void>;
}

/** The hosted sessions: their tabs, one emulator each, the starts that open them and the pushes that feed and close them. */
export const createHosting = (deps: HostingDeps): Hosting => {
  const { client } = deps;
  const dock = createStore(new Dock());
  const failure = createStore<StartFailure | undefined>(undefined);
  const emulators = new Map<string, Emulator>();
  const ended = new Set<string>();

  const emulatorFor = (session: string): Emulator => {
    const existing = emulators.get(session);
    if (existing !== undefined) {
      return existing;
    }
    const { terminal, fit } = deps.newTerminal();
    const emulator = new Emulator(session, terminal, { client, memory: deps.memory, fit, reserved: deps.reserved, clipboard: deps.clipboard });
    emulators.set(session, emulator);
    return emulator;
  };

  const settle = (next: Dock) => {
    const before = dock.get().tabs;
    dock.set(next);
    next.tabs.forEach(emulatorFor);
    before
      .filter((session) => !next.tabs.includes(session))
      .forEach((session) => {
        emulators.get(session)?.end();
        emulators.delete(session);
      });
  };

  const paneSize = (): PaneSize => {
    const fitted = [...emulators.values()].find((emulator) => emulator.terminal.element !== undefined);
    return fitted === undefined ? DEFAULT_PANE : { width: fitted.terminal.cols, height: fitted.terminal.rows };
  };

  const opened = (start: HostStart) => {
    if (start.kind === 'startedHost') {
      ended.delete(start.id);
      settle(dock.get().opened(start.id));
    }
  };

  const start = async (ask: () => Promise<HostStart>): Promise<void> => {
    try {
      opened(await ask());
      failure.set(undefined);
    } catch (error) {
      failure.set({ message: error instanceof Error ? error.message : String(error), retry: () => start(ask) });
    }
  };

  client.subscribe('pty/output', (view) => {
    if (!ended.has(view.session)) {
      emulatorFor(view.session).write(view.data);
    }
  });
  client.subscribe('session/exited', (view) => {
    ended.add(view.session);
    settle(dock.get().closed(view.session));
  });
  deps.sessions.subscribe(() => {
    const view = deps.sessions.get();
    if (view !== undefined) {
      settle(dock.get().listing(view.hosted.map((hosted) => hosted.id)));
    }
  });

  return {
    dock,
    failure,
    emulatorOf: (session) => emulators.get(session),
    select: (session) => dock.set(dock.get().select(session)),
    selectPrevious() {
      const previous = dock.get().previous();
      if (previous !== undefined) {
        dock.set(previous);
      }
      return previous !== undefined;
    },
    selectNext() {
      const before = dock.get().selectedTab();
      dock.set(dock.get().next());
      return dock.get().selectedTab() !== before;
    },
    startUnlinked: (root) => start(() => client.call('session/startUnlinked', { root, size: paneSize() })),
    startTicket: (root, ticket, branch, verb) => start(() => client.call('session/startTicket', { root, ticket, branch, verb, size: paneSize() })),
    resume: (root, session, recordedDirectory) => start(() => client.call('session/resume', { root, session, recordedDirectory, size: paneSize() })),
    resumeInTicketWorktree: (root, session, recordedDirectory, branch) =>
      start(() => client.call('session/resumeInTicketWorktree', { session, recordedDirectory, root, branch, size: paneSize() })),
  };
};
