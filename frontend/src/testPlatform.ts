import { Terminal } from '@xterm/xterm';
import type { Platform } from './platform';
import type { Clipboard } from './session/emulator';
import { StorageModeMemory } from './session/modeMemory';
import { RecordingOpener } from './docs/recordingOpener';
import type { FocusSource } from './window/windowFocus';

/** A clipboard in memory: what was copied, and the text a paste reads. */
class MemoryClipboard implements Clipboard {
  readonly copied: string[] = [];
  text = '';

  readText(): Promise<string> {
    return Promise.resolve(this.text);
  }

  writeText(text: string): Promise<void> {
    this.copied.push(text);
    return Promise.resolve();
  }
}

/** A window whose focus the test sets. */
class SetFocus implements FocusSource {
  private notify: (focused: boolean) => void = () => {};

  constructor(private readonly initial: boolean) {}

  isFocused(): Promise<boolean> {
    return Promise.resolve(this.initial);
  }

  onFocusChanged(handler: (focused: boolean) => void): Promise<() => void> {
    this.notify = handler;
    return Promise.resolve(() => {});
  }

  set(focused: boolean): void {
    this.notify(focused);
  }
}

/** The webview's parts for a test: a real xterm.js without a renderer, the session storage, a clipboard in memory, a window whose focus the test sets and whose closes it counts. */
export class TestPlatform implements Platform {
  readonly memory = new StorageModeMemory(sessionStorage);
  readonly fits: Array<{ fit: () => void }> = [];
  readonly clipboard = new MemoryClipboard();
  readonly opener = new RecordingOpener();
  readonly focus: SetFocus;
  closes = 0;
  closeRefusal: string | undefined = undefined;

  constructor(focused: boolean) {
    this.focus = new SetFocus(focused);
  }

  get copied(): string[] {
    return this.clipboard.copied;
  }

  set clipboardText(text: string) {
    this.clipboard.text = text;
  }

  newTerminal(): { terminal: Terminal; fit: { fit(): void } } {
    const fit = { fit: () => {} };
    this.fits.push(fit);
    return { terminal: new Terminal(), fit };
  }

  closeWindow(): Promise<void> {
    this.closes += 1;
    return this.closeRefusal === undefined ? Promise.resolve() : Promise.reject(new Error(this.closeRefusal));
  }

  setFocused(focused: boolean): void {
    this.focus.set(focused);
  }
}

export const testPlatform = (focused = true): TestPlatform => new TestPlatform(focused);
