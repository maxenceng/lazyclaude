import { terminalLook } from './design-system';
import type { LinkOpener } from './docs/opener';
import type { Clipboard } from './session/emulator';
import type { ModeMemory } from './session/modeMemory';
import { StorageModeMemory } from './session/modeMemory';
import { createXterm } from './session/xterm';
import type { FocusSource } from './window/windowFocus';
import { TauriClipboard, TauriOpener, closeWindow, tauriFocus } from './window/tauri';
import type { Terminal } from '@xterm/xterm';

/** What the window takes from the webview it runs in: where it keeps modes, the clipboard, how a link is opened, terminals, the window's focus and closing it. */
export interface Platform {
  memory: ModeMemory;
  clipboard: Clipboard;
  opener: LinkOpener;
  newTerminal(): { terminal: Terminal; fit: { fit(): void } };
  focus: FocusSource;
  closeWindow(): Promise<void>;
}

/** The Tauri webview: sessionStorage for modes, the navigator's clipboard, xterm.js in the design system's look, Tauri's window focus, and Tauri's opener for links. */
export class BrowserPlatform implements Platform {
  readonly memory: ModeMemory = new StorageModeMemory(sessionStorage);
  readonly clipboard: Clipboard = new TauriClipboard();
  readonly opener: LinkOpener = new TauriOpener();
  readonly focus: FocusSource = tauriFocus;

  newTerminal(): { terminal: Terminal; fit: { fit(): void } } {
    return createXterm(terminalLook(document.documentElement));
  }

  closeWindow(): Promise<void> {
    return closeWindow();
  }

  /**
   * Loads the terminal's font: a `@font-face` font is only fetched once something uses it, and xterm.js measures its cells
   * when it opens, so a pane opened first after a reload measured the fallback font and clipped its last row once the real one came.
   */
  terminalFontLoaded(): Promise<unknown> {
    const look = terminalLook(document.documentElement);
    return document.fonts.load(`${look.fontSize}px ${look.fontFamily}`);
  }
}
