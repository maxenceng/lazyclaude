import { invoke } from '@tauri-apps/api/core';
import { getCurrentWindow } from '@tauri-apps/api/window';
import { readText, writeText } from '@tauri-apps/plugin-clipboard-manager';
import { openPath, openUrl } from '@tauri-apps/plugin-opener';
import type { LinkOpener } from '../docs/opener';
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

/**
 * Links through Tauri's opener plugin: a web address in the system browser, a path in the system's default application
 * (`xdg-open` on Linux), so the webview itself never navigates (ADR 0087).
 */
export class TauriOpener implements LinkOpener {
  openUrl(url: string): Promise<void> {
    return openUrl(url);
  }

  openPath(path: string): Promise<void> {
    return openPath(path);
  }
}
