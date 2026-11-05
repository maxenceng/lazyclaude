import type { Client, MergeState, NewSessionView, SessionPayload, TicketView } from '../protocol';
import { createStore, type Store } from '../shared/store';
import type { Hosting } from './hosting';
import { type SessionRow, SessionRowKind, rowsFor } from './sessionRows';
import type { SessionStore } from './sessionStore';
import { VerbPicker } from './verbPicker';

// deferred: the desktop does not read review's merged pull requests, so `done` is never preselected here — close it when the window reads them.
const MERGE_UNKNOWN: MergeState = 'UNKNOWN';

export interface SessionsDeps {
  client: Client;
  store: SessionStore;
  hosting: Hosting;
  /** The project's root and the tickets on its board, none before the board is read. */
  project: () => SessionPayload | undefined;
  /** The ticket the Board's cursor is on. */
  ticket: () => TicketView | undefined;
  /** A session was opened or selected: its tab should take focus. */
  onOpened: () => void;
  /** The verb picker opened over the ticket's pane. */
  onPicking: () => void;
}

/** The selected ticket's sessions: the rows under it, the cursor over them, the verb picker, and what opening each row starts. */
export class SessionsController {
  readonly picker: Store<VerbPicker | undefined> = createStore<VerbPicker | undefined>(undefined);
  readonly cursor: Store<number> = createStore(0);

  constructor(private readonly deps: SessionsDeps) {}

  get hosting(): Hosting {
    return this.deps.hosting;
  }

  rows(): SessionRow[] {
    return rowsFor(this.deps.store.sessions.get(), this.deps.ticket()?.id);
  }

  move(delta: number): void {
    this.cursor.set(Math.min(this.rows().length - 1, Math.max(0, this.cursor.get() + delta)));
  }

  moveTo(index: number): void {
    this.cursor.set(index);
  }

  /** Opens the row under the cursor: focuses a hosted session's tab, resumes a past one, or starts a new one. */
  activate(): void {
    const row = this.rows()[this.cursor.get()];
    if (row !== undefined) {
      this.open(row);
    }
  }

  /** `n`: what starting a session from the ticket opens is `session`'s to say — the verb picker, or an unlinked session. */
  async newSession(): Promise<void> {
    const ticket = this.deps.ticket();
    const project = this.deps.project();
    if (ticket === undefined || project === undefined) {
      return;
    }
    const answer = await this.ask(() => this.deps.client.call('session/newSessionFor', { sessions: project, ticket: ticket.id, verbs: ticket.verbs, merged: MERGE_UNKNOWN }));
    if (answer?.kind === 'unlinkedStart') {
      await this.opened(this.hosting.startUnlinked(project.root));
    } else if (answer?.kind === 'verbChoice') {
      this.deps.onPicking();
      this.picker.set(VerbPicker.opening(ticket.id, ticket.branch, answer.verbs, answer.preselected));
    }
  }

  /** `N`: a session in the project root, linked to no ticket. */
  async newUnlinked(): Promise<void> {
    const project = this.deps.project();
    if (project !== undefined) {
      await this.opened(this.hosting.startUnlinked(project.root));
    }
  }

  isPicking(): boolean {
    return this.picker.get() !== undefined;
  }

  pickDown(): void {
    this.picker.set(this.picker.get()?.down());
  }

  pickUp(): void {
    this.picker.set(this.picker.get()?.up());
  }

  cancelPick(): void {
    this.picker.set(undefined);
  }

  /** Starts the verb under the picker's cursor, or at `index`, and closes the picker. */
  async pick(index?: number): Promise<void> {
    const open = this.picker.get();
    const project = this.deps.project();
    const chosen = index === undefined ? open : open?.at(index);
    const verb = chosen?.selected();
    this.picker.set(undefined);
    if (chosen !== undefined && verb !== undefined && project !== undefined) {
      await this.opened(this.hosting.startTicket(project.root, chosen.ticket, chosen.branch, verb));
    }
  }

  private open(row: SessionRow): void {
    const project = this.deps.project();
    const ticket = this.deps.ticket();
    switch (row.kind) {
      case SessionRowKind.Hosted:
        this.hosting.select(row.session);
        this.deps.onOpened();
        break;
      case SessionRowKind.Past:
      case SessionRowKind.Unlinked:
        if (project !== undefined) {
          const listing = ticket === undefined ? { kind: 'noTicketListingPayload' as const } : { kind: 'ticketListingPayload' as const, branch: ticket.branch };
          void this.opened(this.hosting.resumePast(project, row.session.id, row.session.directory, listing));
        }
        break;
      case SessionRowKind.New:
        void this.newSession();
        break;
      case SessionRowKind.Live:
        break;
    }
  }

  private async opened(starting: Promise<boolean>): Promise<void> {
    if (await starting) {
      this.deps.onOpened();
    }
  }

  private async ask(call: () => Promise<NewSessionView>): Promise<NewSessionView | undefined> {
    try {
      return await call();
    } catch (error) {
      this.hosting.failed(error instanceof Error ? error.message : String(error), async () => {
        await this.newSession();
        return true;
      });
      return undefined;
    }
  }
}
