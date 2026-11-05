import { invoke } from '@tauri-apps/api/core';
import { getCurrentWindow } from '@tauri-apps/api/window';
import { readText, writeText } from '@tauri-apps/plugin-clipboard-manager';
import type { Clipboard } from '../session/emulator';
import type { FocusSource } from './windowFocus';

/** The window's focus as Tauri reports it. */
export const tauriFocus: FocusSource = {
  isFocused: () => getCurrentWindow().isFocused(),
  onFocusChanged: (handler) => getCurrentWindow().onFocusChanged(({ payload }) => handler(payload)),
};

/** One beat of the page's heartbeat, for the window to hear (ADR 0083). */
export const pageAlive = (): Promise<void> => invoke('page_alive');

/**
 * The clipboard through Tauri's plugin: WebKitGTK refuses `navigator.clipboard` to the page, so a paste read nothing and a
 * copy wrote nothing (ADR 0086).
 */
export class TauriClipboard implements Clipboard {
  readText(): Promise<string> {
    return readText();
  }

  writeText(text: string): Promise<void> {
    return writeText(text);
  }
}
