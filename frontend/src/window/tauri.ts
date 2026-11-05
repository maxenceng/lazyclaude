import { invoke } from '@tauri-apps/api/core';
import { getCurrentWindow } from '@tauri-apps/api/window';
import type { FocusSource } from './windowFocus';

/** The window's focus as Tauri reports it. */
export const tauriFocus: FocusSource = {
  isFocused: () => getCurrentWindow().isFocused(),
  onFocusChanged: (handler) => getCurrentWindow().onFocusChanged(({ payload }) => handler(payload)),
};

/** One beat of the page's heartbeat, for the window to hear (ADR 0083). */
export const pageAlive = (): Promise<void> => invoke('page_alive');
