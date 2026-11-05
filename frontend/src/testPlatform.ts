import { Terminal } from '@xterm/xterm';
import type { Platform } from './platform';
import { StorageModeMemory } from './session/modeMemory';

export interface TestPlatform extends Platform {
  fits: Array<{ fit: () => void }>;
  setFocused(focused: boolean): void;
  copied: string[];
  clipboardText: string;
}

/** The webview's parts for a test: a real xterm.js without a renderer, the session storage, a clipboard in memory and a window whose focus the test sets. */
export const testPlatform = (focused = true): TestPlatform => {
  let notify: (focused: boolean) => void = () => {};
  const platform: TestPlatform = {
    memory: new StorageModeMemory(sessionStorage),
    fits: [],
    copied: [],
    clipboardText: '',
    clipboard: {
      readText: () => Promise.resolve(platform.clipboardText),
      writeText: (text) => {
        platform.copied.push(text);
        return Promise.resolve();
      },
    },
    newTerminal: () => {
      const fit = { fit: () => {} };
      platform.fits.push(fit);
      return { terminal: new Terminal(), fit };
    },
    focus: {
      isFocused: () => Promise.resolve(focused),
      onFocusChanged: (handler) => {
        notify = handler;
        return Promise.resolve(() => {});
      },
    },
    setFocused: (next) => notify(next),
  };
  return platform;
};
