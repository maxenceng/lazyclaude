import type { Client, SessionMergeState, SessionNewSessionView, SessionSessionPayload, BoardTicketView } from '../protocol';
import { Store } from '../shared/store';
import type { Hosting } from './hosting';
import { type SessionRow, SessionRowKind, rowKey, rowsFor } from './sessionRows';
import type { SessionStore } from './sessionStore';
import { VerbPicker } from './verbPicker';

// deferred: the desktop does not read review's merged pull requests, so `done` is never preselected here — close it when the window reads them.
const MERGE_UNKNOWN: SessionMergeState = 'UNKNOWN';

export interface SessionsDeps {
  client: Client;
  store: SessionStore;
  hosting: Hosting;
  /** The project's root and the tickets on its board, none before the board is read. */
  project: () => SessionSessionPayload | undefined;
  /** The ticket the Board's cursor is on. */
  ticket: () => BoardTicketView | undefined;
  /** A session was opened or selected: its tab should take focus. */
  onOpened: () => void;
  /** The verb picker opened over the ticket's pane. */
  onPicking: () => void;
}

/** The selected ticket's sessions: the rows under it, the cursor over them, the verb picker, and what opening each row starts. */
export class SessionsController {
  readonly picker: Store<VerbPicker | undefined> = new Store<VerbPicker | undefined>(undefined);
  readonly cursor: Store<number> = new Store(0);

  /** The ticket and the row the cursor was last on, so a read that reorders the rows keeps it on the same session. */
  private under: { ticket: string | undefined; row: string | undefined } = { ticket: undefined, row: undefined };

  constructor(private readonly deps: SessionsDeps) {
    deps.store.sessions.subscribe(() => this.followRow());
    this.cursor.subscribe(() => this.remember());
  }

  get hosting(): Hosting {
    return this.deps.hosting;
  }

  rows(): SessionRow[] {
    return rowsFor(this.deps.store.sessions.get(), this.deps.ticket()?.id);
  }

  private remember(): void {
    const row = this.rows()[this.cursor.get()];
    this.under = { ticket: this.deps.ticket()?.id, row: row === undefined ? undefined : rowKey(row) };
  }

  /** Sessions were read again: the cursor goes to where its row now is, if it is still listed under the same ticket. */
  private followRow(): void {
    const { ticket, row } = this.under;
    const at = ticket === this.deps.ticket()?.id ? this.rows().findIndex((listed) => rowKey(listed) === row) : -1;
    if (at >= 0 && at !== this.cursor.get()) {
      this.cursor.set(at);
    } else {
      this.remember();
    }
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

  /** `n`: what starting a session from the ticket opens is `session`'s to say — the verb picker, or an unlinked session; a choice answered after another ticket was selected is dropped. */
  async newSession(): Promise<void> {
    const ticket = this.deps.ticket();
    const project = this.deps.project();
    if (ticket === undefined || project === undefined) {
      return;
    }
    const answer = await this.ask(() => this.deps.client.call('session/newSessionFor', { sessions: project, ticket: ticket.id, verbs: ticket.verbs, merged: MERGE_UNKNOWN }));
    if (answer?.kind === 'unlinkedStart') {
      await this.opened(this.hosting.startUnlinked(project.root));
    } else if (answer?.kind === 'verbChoice' && this.deps.ticket()?.id === ticket.id) {
      this.deps.onPicking();
      this.picker.set(VerbPicker.opening(ticket.id, ticket.branch, answer.verbs, answer.preselected));
    }
  }

  /** `N`: a session in the project root, linked to no ticket; nothing while no ticket is under the cursor, as on an empty board, whose key bar does not offer it. */
  async newUnlinked(): Promise<void> {
    const project = this.deps.project();
    if (project !== undefined && this.deps.ticket() !== undefined) {
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

  /** The picker offers the verbs of the ticket it opened on, so selecting another ticket closes it. */
  followSelection(): void {
    const open = this.picker.get();
    if (open !== undefined && open.ticket !== this.deps.ticket()?.id) {
      this.cancelPick();
    }
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
        if (project !== undefined && ticket !== undefined) {
          void this.opened(this.hosting.resumePast(project, row.session.id, row.session.directory, { kind: 'ticketListingPayload', branch: ticket.branch }));
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

  private async ask(call: () => Promise<SessionNewSessionView>): Promise<SessionNewSessionView | undefined> {
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
