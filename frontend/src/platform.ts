import { terminalLook } from './design-system';
import type { Clipboard } from './session/emulator';
import type { ModeMemory } from './session/modeMemory';
import { StorageModeMemory } from './session/modeMemory';
import { createXterm } from './session/xterm';
import type { FocusSource } from './window/windowFocus';
import { tauriFocus } from './window/tauri';
import type { Terminal } from '@xterm/xterm';

/** What the window takes from the webview it runs in: where it keeps modes, the clipboard, terminals and the window's focus. */
export interface Platform {
  memory: ModeMemory;
  clipboard: Clipboard;
  newTerminal(): { terminal: Terminal; fit: { fit(): void } };
  focus: FocusSource;
}

/** The Tauri webview: sessionStorage for modes, the navigator's clipboard, xterm.js in the design system's look, Tauri's window focus. */
export class BrowserPlatform implements Platform {
  readonly memory: ModeMemory = new StorageModeMemory(sessionStorage);
  readonly clipboard: Clipboard = navigator.clipboard;
  readonly focus: FocusSource = tauriFocus;

  newTerminal(): { terminal: Terminal; fit: { fit(): void } } {
    return createXterm(terminalLook(document.documentElement));
  }
}
