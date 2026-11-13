import type { BoardTicketView, Client, SessionSessionPayload, SessionWorktreeView } from '../protocol';
import { Store } from '../shared/store';

/** How long the cursor rests on a ticket before its worktree is read, since each read runs git. */
export const WORKTREE_DEBOUNCE_MS = 150;

interface Answer {
  ticket: string;
  view: SessionWorktreeView;
}

interface Deps {
  client: Client;
  /** The project's root and its tickets; the read is asked only once `session/watch` has been, so none is asked before then. */
  project: () => SessionSessionPayload | undefined;
}

/** The worktree of the ticket under the cursor, as `session/worktreeOf` reads it: asked once the cursor rests, an answer for a ticket the cursor has left dropped. */
export class TicketWorktrees {
  readonly answer: Store<Answer | undefined> = new Store<Answer | undefined>(undefined);
  private wanted: string | undefined;
  private timer: ReturnType<typeof setTimeout> | undefined;

  constructor(private readonly deps: Deps) {}

  /** What was read for `ticket`; none while it is being read or was never asked. */
  of(ticket: string): SessionWorktreeView | undefined {
    const answer = this.answer.get();
    return answer?.ticket === ticket ? answer.view : undefined;
  }

  /** The cursor is on `ticket`, or on none: a read still waiting for the cursor to rest is replaced. */
  ask(ticket: Pick<BoardTicketView, 'id' | 'branch'> | undefined): void {
    clearTimeout(this.timer);
    this.wanted = ticket?.id;
    if (ticket !== undefined) {
      this.timer = setTimeout(() => void this.read(ticket), WORKTREE_DEBOUNCE_MS);
    }
  }

  private async read(ticket: Pick<BoardTicketView, 'id' | 'branch'>): Promise<void> {
    const project = this.deps.project();
    if (project === undefined) {
      return;
    }
    const view = await this.deps.client
      .call('session/worktreeOf', { sessions: project, branch: ticket.branch, mergedHead: { kind: 'noMergedHead' } })
      .catch((error: unknown): SessionWorktreeView => ({ kind: 'unreadableWorktree', reason: error instanceof Error ? error.message : String(error) }));
    if (this.wanted === ticket.id) {
      this.answer.set({ ticket: ticket.id, view });
    }
  }
}
