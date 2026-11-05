import { afterEach, describe, expect, it, vi } from 'vitest';
import { BrowserPlatform } from './platform';

const loaded: string[] = [];

describe('the webview the window runs in', () => {
  afterEach(() => {
    document.documentElement.removeAttribute('style');
    loaded.length = 0;
  });

  it('loads the terminal\'s font in the size the terminal draws it, before a pane can measure the fallback', async () => {
    document.documentElement.style.setProperty('--font-mono', "'JetBrains Mono', monospace");
    document.documentElement.style.setProperty('--size-terminal', '13px');
    Object.defineProperty(document, 'fonts', {
      configurable: true,
      value: { load: vi.fn((font: string) => Promise.resolve(loaded.push(font))) },
    });
    await new BrowserPlatform().terminalFontLoaded();
    expect(loaded).toEqual(["13px 'JetBrains Mono', monospace"]);
  });
});
