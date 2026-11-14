import { Store } from '../shared/store';
import { type CaptureState, CaptureStateKind, ClosedCapture, ComposingCapture, RefusedCapture, StartingCapture } from './captureState';

/** Starts the session that creates a ticket from a description; answers why it did not start, or nothing once it has. */
export type NewTicketStart = (root: string, description: string) => Promise<string | undefined>;

interface Deps {
  root: () => string | undefined;
  start: NewTicketStart;
}

/** The popup a new ticket's description is typed in (ADR 0110): one start at a time, a refusal keeps the description, and nothing retries until enter is pressed again. */
export class CaptureController {
  readonly state: Store<CaptureState> = new Store<CaptureState>(new ClosedCapture());

  constructor(private readonly deps: Deps) {}

  /** Opens the popup, when a project is there to start in and it is not already open. */
  open(): void {
    if (this.deps.root() !== undefined && !this.state.get().isOpen()) {
      this.state.set(new ComposingCapture(''));
    }
  }

  /** Types into the description; held still while a start is in flight. */
  edit(description: string): void {
    const current = this.state.get();
    if (current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) {
      this.state.set(new ComposingCapture(description));
    }
  }

  /** Drops the popup and its text; held still while a start is in flight. */
  cancel(): void {
    const current = this.state.get();
    if (current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) {
      this.state.set(new ClosedCapture());
    }
  }

  /** Sends the description as typed; nothing while blank or while one is in flight, so a second enter never starts a second session. */
  create(): void {
    const current = this.state.get();
    const root = this.deps.root();
    if ((current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) && root !== undefined && current.description.trim() !== '') {
      this.state.set(new StartingCapture(current.description));
      void this.send(root, current.description);
    }
  }

  private async send(root: string, description: string): Promise<void> {
    const reason = await this.deps.start(root, description);
    this.state.set(reason === undefined ? new ClosedCapture() : new RefusedCapture(description, reason));
  }
}
