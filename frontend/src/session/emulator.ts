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

/** Where a reloaded emulator is on its way to `session/repaint`: its kept modes written, then fitted to its pane. */
enum Repaint {
  None,
  Restoring,
  Restored,
}

/** One hosted session's xterm.js: fed by its `pty/output`, sending its keys, pastes and mouse reports as `pty/input`, and keeping its private modes for a reload. */
export class Emulator {
  readonly mouseReported: Store<boolean> = createStore(false);
  private readonly kept = new KeptModes();
  private repaint = Repaint.None;
  private fitted = false;
  private readonly subscriptions: Array<() => void> = [];

  constructor(
    readonly session: string,
    readonly terminal: Terminal,
    private readonly deps: EmulatorDeps,
  ) {
    const recalled = deps.memory.recall(session);
    trackModes(terminal, this.kept, () => {
      deps.memory.remember(session, this.kept);
      this.mouseReported.set(this.kept.reportsMouse());
    });
    this.subscriptions.push(
      disposer(terminal.onData((data) => this.send(bytesOfText(data)))),
      disposer(terminal.onBinary((data) => this.binary(data))),
    );
    terminal.attachCustomKeyEventHandler((event) => this.handleKey(event));
    if (recalled !== undefined) {
      this.repaint = Repaint.Restoring;
      terminal.write(recalled.restoration(), () => {
        this.repaint = Repaint.Restored;
        this.repaintWhenFitted();
      });
    }
  }

  /** What `claude` wrote, as the base64 text of `pty/output`. */
  write(data: string): void {
    this.terminal.write(fromBase64(data));
  }

  /** Puts the terminal into `container` in place of whichever was there, fits it and tells `claude` the size it came to; focus is the dock's to give. */
  attach(container: HTMLElement): void {
    if (this.terminal.element === undefined) {
      this.terminal.open(container);
    }
    if (this.terminal.element !== undefined) {
      container.replaceChildren(this.terminal.element);
    }
    this.resize();
    this.fitted = true;
    this.repaintWhenFitted();
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

  /** ADR 0049: `claude` redraws at the pty's size, so a reloaded emulator asks for the repaint only once its kept modes are in and it has been fitted to that size. */
  private repaintWhenFitted(): void {
    if (this.repaint === Repaint.Restored && this.fitted) {
      this.repaint = Repaint.None;
      this.deps.client.send('session/repaint', { session: this.session });
    }
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
    if (selection === '') {
      return;
    }
    try {
      await this.deps.clipboard.writeText(selection);
    } catch {
      // deferred: a clipboard the webview refuses is dropped without a word — close it when the status line can carry the window's own warnings.
    }
  }

  /** Text goes through `term.paste()`, so it arrives bracketed whenever `claude` asked for bracketed paste. */
  private async paste(): Promise<void> {
    try {
      const text = await this.deps.clipboard.readText();
      if (text !== '') {
        this.terminal.paste(text);
      }
    } catch {
      // deferred: a clipboard the webview refuses is dropped without a word — close it when the status line can carry the window's own warnings.
    }
  }

  private send(bytes: Uint8Array): void {
    this.deps.client.send('pty/input', { session: this.session, data: toBase64(bytes) });
  }
}
