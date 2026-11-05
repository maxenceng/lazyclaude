import { writeText } from '@tauri-apps/plugin-clipboard-manager';
import { describe, expect, it, vi } from 'vitest';
import { TauriClipboard } from './tauri';

vi.mock('@tauri-apps/plugin-clipboard-manager', () => ({
  readText: vi.fn(() => Promise.resolve('copied elsewhere')),
  writeText: vi.fn(() => Promise.resolve()),
}));

describe('the clipboard the window uses', () => {
  it('reads the text through Tauri\'s clipboard plugin, which WebKitGTK does not refuse', async () => {
    await expect(new TauriClipboard().readText()).resolves.toBe('copied elsewhere');
  });

  it('writes the text through Tauri\'s clipboard plugin', async () => {
    await new TauriClipboard().writeText('selected');
    expect(writeText).toHaveBeenCalledWith('selected');
  });
});
