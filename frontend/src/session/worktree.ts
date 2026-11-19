import type { Client, SessionWorktreeShownView } from '../protocol';
import { Store } from '../shared/store';

/** The selected ticket as the worktree needs it: its branch, and whether the board says it is done. */
export interface WorktreeTicket {
  id: string;
  branch: string;
  done: boolean;
}

export interface WorktreeDeps {
  client: Client;
  ticket: () => WorktreeTicket | undefined;
  /** Reads the board again and answers the ticket `id` as it now reads; none when the board no longer has it. */
  reread: (id: string) => Promise<WorktreeTicket | undefined>;
}

/** The worktree the question is about, while it is open: its branch, which the yes removes, and the path as shown. */
export interface Asked {
  branch: string;
  shown: string;
}

interface Read {
  branch: string;
  view: SessionWorktreeShownView;
}

/**
 * A done ticket's worktree in the window (ADR 0117): the selected ticket's state as the sidecar reads it from the branch alone,
 * read again whenever something it depends on changes, one read at a time; and the question that removes it. A refusal is a result
 * shown beside the ticket, never retried by itself.
 */
export class Worktree {
  /** The last read, whichever ticket it was for: `shown` answers it only for the ticket selected now. */
  readonly read: Store<Read | undefined> = new Store<Read | undefined>(undefined);
  readonly asking: Store<Asked | undefined> = new Store<Asked | undefined>(undefined);
  /** Why the last removal removed nothing, for the ticket it was asked of. */
  readonly refusal: Store<string | undefined> = new Store<string | undefined>(undefined);
  private refusedFor: string | undefined;
  private reading = false;
  private again = false;

  constructor(private readonly deps: WorktreeDeps) {
    deps.client.subscribe('review/mergedChanged', () => this.refresh());
    deps.client.subscribe('session/ticketDoneEnded', (id) => void this.ticketDoneEnded(id));
  }

  /** The selected ticket's worktree, as read; none before the read lands, for a ticket not done, or with no worktree. */
  shown(): Exclude<SessionWorktreeShownView, { kind: 'noWorktree' }> | undefined {
    const read = this.read.get();
    const ticket = this.deps.ticket();
    if (read === undefined || ticket === undefined || !ticket.done || ticket.branch !== read.branch || read.view.kind === 'noWorktree') {
      return undefined;
    }
    return read.view;
  }

  /** The one predicate behind the key, the key bar's `d` and the row's button: the selected ticket's worktree is shown and clean. */
  removable(): boolean {
    return this.shown()?.kind === 'clearWorktree';
  }

  isAsking(): boolean {
    return this.asking.get() !== undefined;
  }

  /** Something the state depends on changed: read it again, after the read in flight if there is one. */
  refresh(): void {
    if (this.deps.ticket()?.branch !== this.refusedFor) {
      this.refusal.set(undefined);
    }
    if (this.reading) {
      this.again = true;
    } else {
      void this.readSelected();
    }
  }

  /** `d` or a click on the row: the question, where the selected ticket's worktree is clean. */
  request(): void {
    const shown = this.shown();
    const ticket = this.deps.ticket();
    if (shown?.kind === 'clearWorktree' && ticket !== undefined && !this.isAsking()) {
      this.asking.set({ branch: ticket.branch, shown: shown.shown });
    }
  }

  /** Remove takes the worktree away; any other answer keeps it. */
  answer(remove: boolean): void {
    const asked = this.asking.get();
    this.asking.set(undefined);
    if (remove && asked !== undefined) {
      void this.remove(asked.branch);
    }
  }

  /** A `/ticket done` session of this window ended: the board is read again, and the question opens at once only for a ticket that now reads done with a clean worktree. */
  async ticketDoneEnded(id: string): Promise<void> {
    const ticket = await this.deps.reread(id);
    if (ticket === undefined || !ticket.done) {
      return;
    }
    const view = await this.deps.client.call('session/worktreeOf', { branch: ticket.branch }).catch(() => undefined);
    if (view?.kind === 'clearWorktree' && !this.isAsking()) {
      this.asking.set({ branch: ticket.branch, shown: view.shown });
    }
  }

  private async readSelected(): Promise<void> {
    const ticket = this.deps.ticket();
    if (ticket === undefined || !ticket.done) {
      return;
    }
    this.reading = true;
    try {
      const view = await this.deps.client.call('session/worktreeOf', { branch: ticket.branch });
      this.read.set({ branch: ticket.branch, view });
    } catch {
      this.read.set(undefined);
    } finally {
      this.reading = false;
    }
    if (this.again) {
      this.again = false;
      void this.readSelected();
    }
  }

  private async remove(branch: string): Promise<void> {
    try {
      const outcome = await this.deps.client.call('session/removeWorktree', { branch });
      if (outcome.kind === 'removedWorktree') {
        this.read.set({ branch, view: { kind: 'noWorktree' } });
      } else {
        this.refuse(branch, outcome.refusal);
      }
    } catch (error) {
      this.refuse(branch, error instanceof Error ? error.message : String(error));
    }
    this.refresh();
  }

  private refuse(branch: string, reason: string): void {
    this.refusedFor = branch;
    this.refusal.set(this.deps.ticket()?.branch === branch ? reason : undefined);
  }
}
