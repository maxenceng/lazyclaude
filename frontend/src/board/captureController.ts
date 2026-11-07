import type { BoardBoardAfterCaptureView, Client } from '../protocol';
import { Store } from '../shared/store';
import { type CaptureState, CaptureStateKind, ClosedCapture, ComposingCapture, CreatingCapture, RefusedCapture } from './captureState';

interface Deps {
  client: Client;
  root: () => string | undefined;
  /** Told the id GitHub gave the new ticket and the board read once it had, after the popup has closed. */
  onCaptured: (id: string, board: BoardBoardAfterCaptureView) => void;
}

/** The popup a draft ticket's title is typed in (ADR 0054): one create at a time, a refusal keeps the title, and nothing retries until enter is pressed again. */
export class CaptureController {
  readonly state: Store<CaptureState> = new Store<CaptureState>(new ClosedCapture());

  constructor(private readonly deps: Deps) {}

  /** Opens the popup, when a project is there to capture into and it is not already open. */
  open(): void {
    if (this.deps.root() !== undefined && !this.state.get().isOpen()) {
      this.state.set(new ComposingCapture(''));
    }
  }

  /** Types into the title; held still while a create is in flight. */
  edit(title: string): void {
    const current = this.state.get();
    if (current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) {
      this.state.set(new ComposingCapture(title));
    }
  }

  /** Drops the popup and its text; held still while a create is in flight. */
  cancel(): void {
    const current = this.state.get();
    if (current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) {
      this.state.set(new ClosedCapture());
    }
  }

  /** Sends the title; nothing while one is in flight, so a second enter never makes a second issue. */
  create(): void {
    const current = this.state.get();
    const root = this.deps.root();
    if ((current.kind === CaptureStateKind.Composing || current.kind === CaptureStateKind.Refused) && root !== undefined) {
      this.state.set(new CreatingCapture(current.title));
      void this.send(root, current.title);
    }
  }

  private async send(root: string, title: string): Promise<void> {
    try {
      const answer = await this.deps.client.call('board/capture', { root, title });
      if (answer.kind === 'capturedOnBoard') {
        this.state.set(new ClosedCapture());
        this.deps.onCaptured(answer.id, answer.board);
      } else {
        this.state.set(new RefusedCapture(title, answer.reason));
      }
    } catch (error) {
      this.state.set(new RefusedCapture(title, error instanceof Error ? error.message : String(error)));
    }
  }
}
