import type { IDisposable, Terminal } from '@xterm/xterm';
import { binaryBytes, bytesOfText, type Client, fromBase64, toBase64 } from '../protocol';
import { createStore, type Store } from '../shared/store';
import type { ModeMemory } from './modeMemory';
import { PaneKey, type PaneKeyPress, paneKeyOf } from './paneKeys';
import { KeptModes, trackModes } from './privateModes';

export interface Clipboard {
  readText(): Promise<string>;
  writeText(text: string): Promise<void>;
}

export interface EmulatorDeps {
  client: Client;
  memory: ModeMemory;
  fit: { fit(): void };
  /** What a reserved chord does, when the press is one; the composition side knows the chords and their meaning. */
  reserved: (press: PaneKeyPress) => (() => void) | undefined;
  clipboard: Clipboard;
}

const disposer = (subscription: IDisposable) => () => subscription.dispose();

/** One hosted session's xterm.js: fed by its `pty/output`, sending its keys, pastes and mouse reports as `pty/input`, and keeping its private modes for a reload. */
export class Emulator {
  readonly mouseReported: Store<boolean> = createStore(false);
  private readonly kept: KeptModes;
  private readonly subscriptions: Array<() => void> = [];

  constructor(
    readonly session: string,
    readonly terminal: Terminal,
    private readonly deps: EmulatorDeps,
  ) {
    const recalled = deps.memory.recall(session);
    this.kept = recalled ?? new KeptModes();
    this.mouseReported.set(this.kept.reportsMouse());
    trackModes(terminal, this.kept, () => {
      deps.memory.remember(session, this.kept);
      this.mouseReported.set(this.kept.reportsMouse());
    });
    this.subscriptions.push(
      deps.client.subscribe('pty/output', (view) => {
        if (view.session === session) {
          terminal.write(fromBase64(view.data));
        }
      }),
      disposer(terminal.onData((data) => this.send(bytesOfText(data)))),
      disposer(terminal.onBinary((data) => this.binary(data))),
    );
    terminal.attachCustomKeyEventHandler((event) => this.handleKey(event));
    if (recalled !== undefined) {
      terminal.write(recalled.restoration(), () => deps.client.send('session/repaint', { session }));
    }
  }

  /** Puts the terminal into `container`, fits it and tells `claude` the size it came to. */
  attach(container: HTMLElement): void {
    if (this.terminal.element === undefined) {
      this.terminal.open(container);
    } else if (this.terminal.element.parentElement !== container) {
      container.appendChild(this.terminal.element);
    }
    this.resize();
    this.terminal.focus();
  }

  /** ADR 0049: the emulator is fitted first, then the pty is told, so what `claude` redraws lands in a buffer of the new size. */
  resize(): void {
    this.deps.fit.fit();
    this.deps.client.send('pty/resize', { session: this.session, cols: this.terminal.cols, rows: this.terminal.rows });
  }

  binary(data: string): void {
    this.send(binaryBytes(data));
  }

  /** The key handler of the pane: false keeps the key from xterm.js, true leaves it to the terminal. */
  handleKey(event: KeyboardEvent): boolean {
    const act = this.deps.reserved(event);
    const kind = paneKeyOf(event, () => act !== undefined);
    if (kind === PaneKey.Terminal) {
      return true;
    }
    event.preventDefault();
    if (event.type === 'keydown') {
      this.take(kind, act);
    }
    return false;
  }

  /** The session ended: nothing of it is kept, and the terminal goes. */
  end(): void {
    this.dispose();
    this.deps.memory.forget(this.session);
  }

  dispose(): void {
    this.subscriptions.forEach((unsubscribe) => unsubscribe());
    this.terminal.dispose();
  }

  private take(kind: PaneKey, reserved: (() => void) | undefined): void {
    if (kind === PaneKey.Reserved) {
      reserved?.();
    } else if (kind === PaneKey.Copy) {
      void this.copy();
    } else if (kind === PaneKey.Paste) {
      void this.paste();
    }
  }

  private async copy(): Promise<void> {
    const selection = this.terminal.getSelection();
    if (selection !== '') {
      await this.deps.clipboard.writeText(selection);
    }
  }

  /** Text goes through `term.paste()`, so it arrives bracketed whenever `claude` asked for bracketed paste. */
  private async paste(): Promise<void> {
    const text = await this.deps.clipboard.readText();
    if (text !== '') {
      this.terminal.paste(text);
    }
  }

  private send(bytes: Uint8Array): void {
    this.deps.client.send('pty/input', { session: this.session, data: toBase64(bytes) });
  }
}
