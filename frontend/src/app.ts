import { createBoardStore } from './board/boardStore';
import { selectedIn, ticketIds } from './board/rows';
import { createSelection } from './board/selection';
import { Action, type KeyPress } from './keys/bindings';
import { bindingsOf } from './keys/chords';
import { type ConfigurationState, FaultedConfiguration, ReadConfiguration, ReadingConfiguration } from './keys/configurationState';
import { Focus, Layout } from './layout';
import type { Platform } from './platform';
import type { Client } from './protocol';
import { createHosting } from './session/hosting';
import { createSessionStore } from './session/sessionStore';
import { type SessionRow, SessionRowKind, rowsFor } from './session/sessionRows';
import { VerbPicker } from './session/verbPicker';
import { watchedPayload } from './session/watched';
import { createStore } from './shared/store';
import { createWarningStore } from './shared/warningStore';
import { createWindowFocus } from './shared/windowFocus';

/** The stores one window reads, and the rule that loads them whenever the sidecar becomes ready. */
export const createApp = (client: Client, platform: Platform) => {
  const board = createBoardStore(client);
  const selection = createSelection();
  const sessions = createSessionStore(client);

  const configuration = createStore<ConfigurationState>(new ReadingConfiguration());
  const reserved = () => configuration.get().reserved();

  const warnings = createWarningStore(client);
  const layout = createStore(new Layout());
  const picker = createStore<VerbPicker | undefined>(undefined);
  const cursor = createStore(0);
  const windowFocus = createWindowFocus(platform.focus);

  const reservedPress = (press: KeyPress) => {
    const binding = reserved().find((candidate) => candidate.matches(press));
    return binding === undefined ? undefined : () => pressInPane(binding.action);
  };
  const hosting = createHosting({
    client,
    memory: platform.memory,
    sessions: sessions.sessions,
    newTerminal: platform.newTerminal,
    clipboard: platform.clipboard,
    reserved: reservedPress,
  });

  const hasDock = () => hosting.dock.get().tabs.length > 0;
  const change = (next: (current: Layout) => Layout) => layout.set(next(layout.get()));
  const root = () => board.state.get().shown()?.root;
  const selectedTicket = () => {
    const view = board.state.get().shown();
    const id = view === undefined ? undefined : selectedIn(view, selection.selected.get());
    return view?.groups.flatMap((group) => group.tickets).find((ticket) => ticket.id === id);
  };
  const rows = (): SessionRow[] => rowsFor(sessions.sessions.get(), selectedTicket()?.id);

  const opened = async (starting: Promise<boolean>) => {
    if (await starting) {
      change((current) => current.focusOn(Focus.Dock));
    }
  };

  const newSessionFor = (ticket: NonNullable<ReturnType<typeof selectedTicket>>) => {
    const project = root();
    if (project === undefined) {
      return;
    }
    if (ticket.verbs.length === 0) {
      void opened(hosting.startUnlinked(project));
    } else {
      change((current) => current.leaveZoom());
      picker.set(new VerbPicker(ticket.id, ticket.branch, ticket.verbs));
    }
  };

  const activate = (row: SessionRow | undefined) => {
    const project = root();
    const ticket = selectedTicket();
    if (row === undefined || project === undefined) {
      return;
    }
    switch (row.kind) {
      case SessionRowKind.Hosted:
        hosting.select(row.session);
        change((current) => current.focusOn(Focus.Dock));
        break;
      case SessionRowKind.Past:
        if (ticket !== undefined) {
          void opened(hosting.resumeInTicketWorktree(project, row.session.id, row.session.directory, ticket.branch));
        }
        break;
      case SessionRowKind.Unlinked:
        void opened(hosting.resume(project, row.session.id, row.session.directory));
        break;
      case SessionRowKind.New:
        if (ticket !== undefined) {
          newSessionFor(ticket);
        }
        break;
      case SessionRowKind.Live:
        break;
    }
  };

  const pressInPicker = (open: VerbPicker, action: Action) => {
    const project = root();
    if (action === Action.MoveDown) {
      picker.set(open.down());
    } else if (action === Action.MoveUp) {
      picker.set(open.up());
    } else if (action === Action.Back) {
      picker.set(undefined);
    } else if (action === Action.Open) {
      const verb = open.selected();
      picker.set(undefined);
      if (verb !== undefined && project !== undefined) {
        void opened(hosting.startTicket(project, open.ticket, open.branch, verb));
      }
    }
  };

  const move = (delta: number) => {
    if (layout.get().focus === Focus.Sessions) {
      cursor.set(Math.min(rows().length - 1, Math.max(0, cursor.get() + delta)));
      return;
    }
    const view = board.state.get().shown();
    if (view !== undefined) {
      selection.move(view, delta);
      cursor.set(0);
    }
  };

  const pressOutsidePane = (action: Action) => {
    switch (action) {
      case Action.MoveDown:
        return move(1);
      case Action.MoveUp:
        return move(-1);
      case Action.Open:
        return open();
      case Action.NewTicketSession: {
        const ticket = selectedTicket();
        return ticket === undefined ? undefined : newSessionFor(ticket);
      }
      case Action.NewSession: {
        const project = root();
        return project === undefined ? undefined : void opened(hosting.startUnlinked(project));
      }
      case Action.Back:
        return change((current) => (current.zoomed || current.focus === Focus.Board ? current.leaveZoom() : current.focusOn(Focus.Board)));
      case Action.PanelLeft:
      case Action.FocusLeft:
        return change((current) => current.focusOn(Focus.Board));
      case Action.PanelRight:
      case Action.FocusRight:
        return change((current) => current.focusOn(Focus.Sessions));
      case Action.FocusDown:
        return hasDock() ? change((current) => current.focusOn(Focus.Dock)) : undefined;
      case Action.FocusUp:
        return change((current) => current.goUp(current.focus === Focus.Dock ? Focus.Sessions : Focus.Board));
      case Action.Zoom:
        return change((current) => current.toggleZoom());
      case Action.ToggleMainView:
        return change((current) => current.toggleMainView());
      case Action.NextWaiting:
        return change((current) => current.leaveZoom());
      default:
        return undefined;
    }
  };

  const open = () => {
    if (layout.get().focus === Focus.Sessions) {
      activate(rows()[cursor.get()]);
    } else if (selectedTicket() !== undefined) {
      cursor.set(0);
      change((current) => current.focusOn(Focus.Sessions));
    }
  };

  const FOCUS_KEYS: readonly Action[] = [
    Action.FocusLeft,
    Action.FocusRight,
    Action.FocusDown,
    Action.FocusUp,
    Action.ToggleMainView,
    Action.Zoom,
    Action.NextWaiting,
  ];

  /** A key the window-level handler took: whatever the focused pane does with it. */
  const press = (action: Action) => {
    const open = picker.get();
    if (open !== undefined) {
      pressInPicker(open, action);
    } else if (layout.get().focus === Focus.Dock && hasDock()) {
      if (FOCUS_KEYS.includes(action)) {
        pressInPane(action);
      }
    } else {
      pressOutsidePane(action);
    }
  };

  /** A reserved chord pressed inside a pane, as the TUI's pane acts on it. */
  const pressInPane = (action: Action) => {
    switch (action) {
      case Action.FocusLeft:
        if (!hosting.selectPrevious()) {
          change((current) => current.focusOn(Focus.Board));
        }
        break;
      case Action.FocusRight:
        hosting.selectNext();
        break;
      case Action.FocusUp:
        change((current) => current.goUp(Focus.Sessions));
        break;
      case Action.ToggleMainView:
        change((current) => current.toggleMainView());
        break;
      case Action.Zoom:
        change((current) => current.toggleZoom());
        break;
      case Action.NextWaiting:
        change((current) => current.leaveZoom());
        break;
      default:
        break;
    }
  };

  hosting.dock.subscribe(() => {
    if (!hasDock() && layout.get().focus === Focus.Dock) {
      change((current) => current.focusOn(Focus.Board));
    }
  });

  let told: string | undefined;
  const tellWatching = () => {
    const paneFocused = layout.get().focus === Focus.Dock && hasDock();
    const payload = watchedPayload(hosting.dock.get().selectedTab(), paneFocused, windowFocus.focused.get());
    const text = JSON.stringify(payload);
    if (text !== told) {
      told = text;
      client.send('session/watching', payload);
    }
  };
  layout.subscribe(tellWatching);
  hosting.dock.subscribe(tellWatching);
  windowFocus.focused.subscribe(tellWatching);

  const loadConfiguration = async () => {
    configuration.set(new ReadingConfiguration());
    try {
      configuration.set(new ReadConfiguration(bindingsOf(await client.call('session/configuration', {}))));
    } catch (error) {
      configuration.set(new FaultedConfiguration(error instanceof Error ? error.message : String(error)));
    }
  };

  let watched = false;
  board.state.subscribe(() => {
    const view = board.state.get().shown();
    if (watched || view === undefined) {
      return;
    }
    watched = true;
    // deferred: the session watch links sessions to the tickets known now, not to ones created later — close it when a new ticket's session shows no lamp.
    void sessions.watch(view.root, ticketIds(view));
  });

  const load = async () => {
    watched = false;
    told = undefined;
    tellWatching();
    const loading = board.load();
    void loadConfiguration();
    await loading;
  };

  return {
    client,
    board,
    selection,
    sessions,
    configuration,
    reserved,
    warnings,
    hosting,
    layout,
    picker,
    cursor,
    windowFocus,
    rows,
    press,
    focusOn: (focus: Focus) => change((current) => current.focusOn(focus)),
    pressVerb(index: number) {
      const open = picker.get();
      if (open !== undefined) {
        pressInPicker(open.at(index), Action.Open);
      }
    },
    pressRow(index: number) {
      cursor.set(index);
      change((current) => current.focusOn(Focus.Sessions));
      activate(rows()[index]);
    },
    load,
    start() {
      let ready = client.status.get().isReady();
      if (ready) {
        void load();
        void warnings.watch();
      }
      client.status.subscribe(() => {
        const nowReady = client.status.get().isReady();
        if (nowReady && !ready) {
          void load();
          void warnings.watch();
        }
        ready = nowReady;
      });
    },
  };
};

export type App = ReturnType<typeof createApp>;
