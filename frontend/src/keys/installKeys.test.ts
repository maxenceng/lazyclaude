import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Action } from './bindings';
import { bindingsOf } from './chords';
import { installKeys } from './installKeys';

function press(key: string, init: KeyboardEventInit = {}) {
  const event = new KeyboardEvent('keydown', { key, cancelable: true, bubbles: true, ...init });
  window.dispatchEvent(event);
  return event;
}

describe('the window-level key handler', () => {
  let remove = () => {};
  afterEach(() => remove());

  it('takes a key the TUI binds, preventing the webview from acting on it, and reports its action', () => {
    const taken: Action[] = [];
    remove = installKeys((action) => taken.push(action));
    const event = press('j');
    expect(event.defaultPrevented).toBe(true);
    expect(taken).toEqual(['moveDown']);
  });

  it('prevents a bound key even when no screen acts on it yet', () => {
    remove = installKeys(() => {});
    expect(press('c', { ctrlKey: true }).defaultPrevented).toBe(true);
    expect(press('Escape').defaultPrevented).toBe(true);
  });

  it('leaves a key the TUI does not bind alone', () => {
    const taken = vi.fn();
    remove = installKeys(taken);
    expect(press('F12').defaultPrevented).toBe(false);
    expect(taken).not.toHaveBeenCalled();
  });

  it('stops listening once removed', () => {
    const taken = vi.fn();
    installKeys(taken)();
    press('j');
    expect(taken).not.toHaveBeenCalled();
  });

  it('swallows the reload keys when told it is a packaged build', () => {
    remove = installKeys(() => {}, { packaged: true });
    expect(press('F5').defaultPrevented).toBe(true);
    expect(press('r', { ctrlKey: true }).defaultPrevented).toBe(true);
  });

  it('swallows every key the TUI leaves in a packaged build, so the webview moves no focus and scrolls nothing', () => {
    const taken = vi.fn();
    remove = installKeys(taken, { packaged: true });
    const unbound = [press('Tab'), press('ArrowDown'), press(' '), press('PageDown'), press('F7'), press('I', { ctrlKey: true, shiftKey: true })];
    expect(unbound.map((event) => event.defaultPrevented)).toEqual([true, true, true, true, true, true]);
    expect(taken).not.toHaveBeenCalled();
  });

  it('lets a development build reload', () => {
    remove = installKeys(() => {}, { packaged: false });
    expect(press('F5').defaultPrevented).toBe(false);
  });

  it('takes a chord the configuration reserved, read at the time of the press', () => {
    let reserved = bindingsOf({ chords: [], desktopNotifications: true, mouse: true });
    const taken: Action[] = [];
    remove = installKeys((action) => taken.push(action), { reserved: () => reserved });
    expect(press('x', { altKey: true }).defaultPrevented).toBe(false);
    reserved = bindingsOf({
      chords: [{ action: 'ZOOM', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 120 } } }],
      desktopNotifications: true,
      mouse: true,
    });
    expect(press('x', { altKey: true }).defaultPrevented).toBe(true);
    expect(taken).toEqual(['zoom']);
  });
});
