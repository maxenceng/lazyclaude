import { getCurrentWindow } from '@tauri-apps/api/window';
import type { FocusSource } from './windowFocus';

/** The window's focus as Tauri reports it. */
export const tauriFocus: FocusSource = {
  isFocused: () => getCurrentWindow().isFocused(),
  onFocusChanged: (handler) => getCurrentWindow().onFocusChanged(({ payload }) => handler(payload)),
};
