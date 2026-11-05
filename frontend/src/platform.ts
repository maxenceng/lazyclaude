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
  newTerminal: () => { terminal: Terminal; fit: { fit(): void } };
  focus: FocusSource;
}

export const browserPlatform = (): Platform => ({
  memory: new StorageModeMemory(sessionStorage),
  clipboard: { readText: () => navigator.clipboard.readText(), writeText: (text) => navigator.clipboard.writeText(text) },
  newTerminal: () => createXterm(terminalLook(document.documentElement)),
  focus: tauriFocus,
});
