import { afterEach, describe, expect, it, vi } from 'vitest';
import { Action } from './bindings';
import { bindingsOf } from './chords';
import { WindowKeys } from './windowKeys';

const press = (key: string, init: KeyboardEventInit = {}) => {
  const event = new KeyboardEvent('keydown', { key, cancelable: true, bubbles: true, ...init });
  window.dispatchEvent(event);
  return event;
};

const rightClick = () => {
  const event = new MouseEvent('contextmenu', { cancelable: true, bubbles: true });
  window.dispatchEvent(event);
  return event;
};

const terminal = () => {
  const surface = document.body.appendChild(document.createElement('div'));
  surface.setAttribute('data-terminal', '');
  return surface.appendChild(document.createElement('textarea'));
};

const pressIn = (target: HTMLElement, key: string, init: KeyboardEventInit = {}) => {
  const event = new KeyboardEvent('keydown', { key, cancelable: true, bubbles: true, ...init });
  target.dispatchEvent(event);
  return event;
};

describe('the window-level key handler', () => {
  let keys: WindowKeys | undefined;
  afterEach(() => keys?.remove());

  it('takes a key the TUI binds, preventing the webview from acting on it, and reports its action', () => {
    const taken: Action[] = [];
    keys = new WindowKeys((action) => taken.push(action));
    const event = press('j');
    expect(event.defaultPrevented).toBe(true);
    expect(taken).toEqual([Action.MoveDown]);
  });

  it('prevents a bound key even when no screen acts on it yet', () => {
    keys = new WindowKeys(() => {});
    expect(press('c', { ctrlKey: true }).defaultPrevented).toBe(true);
    expect(press('Escape').defaultPrevented).toBe(true);
  });

  it('leaves a key the TUI does not bind alone', () => {
    const taken = vi.fn();
    keys = new WindowKeys(taken);
    expect(press('F12').defaultPrevented).toBe(false);
    expect(taken).not.toHaveBeenCalled();
  });

  it('stops listening once removed', () => {
    const taken = vi.fn();
    new WindowKeys(taken).remove();
    press('j');
    expect(taken).not.toHaveBeenCalled();
  });

  it('swallows the reload keys when told it is a packaged build', () => {
    keys = new WindowKeys(() => {}, { packaged: true });
    expect(press('F5').defaultPrevented).toBe(true);
    expect(press('r', { ctrlKey: true }).defaultPrevented).toBe(true);
  });

  it('swallows every key the TUI leaves in a packaged build, so the webview moves no focus and scrolls nothing', () => {
    const taken = vi.fn();
    keys = new WindowKeys(taken, { packaged: true });
    const unbound = [press('Tab'), press('ArrowDown'), press(' '), press('PageDown'), press('F7'), press('I', { ctrlKey: true, shiftKey: true })];
    expect(unbound.map((event) => event.defaultPrevented)).toEqual([true, true, true, true, true, true]);
    expect(taken).not.toHaveBeenCalled();
  });

  it('lets a development build reload', () => {
    keys = new WindowKeys(() => {}, { packaged: false });
    expect(press('F5').defaultPrevented).toBe(false);
  });

  it('takes a chord the configuration reserved, read at the time of the press', () => {
    let reserved = bindingsOf({ chords: [], desktopNotifications: true, mouse: true });
    const taken: Action[] = [];
    keys = new WindowKeys((action) => taken.push(action), { reserved: () => reserved });
    expect(press('x', { altKey: true }).defaultPrevented).toBe(false);
    reserved = bindingsOf({
      chords: [{ action: 'ZOOM', chord: { modifiers: ['ALT'], key: { kind: 'typedCharacter', codePoint: 120 } } }],
      desktopNotifications: true,
      mouse: true,
    });
    expect(press('x', { altKey: true }).defaultPrevented).toBe(true);
    expect(taken).toEqual([Action.Zoom]);
  });

  it('prevents the right-click menu in a packaged build', () => {
    keys = new WindowKeys(() => {}, { packaged: true });
    expect(rightClick().defaultPrevented).toBe(true);
  });

  it('leaves the right-click menu to a development build', () => {
    keys = new WindowKeys(() => {});
    expect(rightClick().defaultPrevented).toBe(false);
  });

  it('stops preventing the right-click menu once removed', () => {
    new WindowKeys(() => {}, { packaged: true }).remove();
    expect(rightClick().defaultPrevented).toBe(false);
  });

  describe('a key typed into a terminal', () => {
    it('is left to the terminal: neither taken nor prevented, whatever the TUI binds', () => {
      const taken = vi.fn();
      keys = new WindowKeys(taken, { packaged: true });
      const event = pressIn(terminal(), 'j');
      expect(event.defaultPrevented).toBe(false);
      expect(taken).not.toHaveBeenCalled();
    });

    it('leaves a reload key to the terminal too, which sends it to claude and cancels it itself', () => {
      keys = new WindowKeys(() => {}, { packaged: true });
      expect(pressIn(terminal(), 'F5').defaultPrevented).toBe(false);
    });

    it('still has the right-click menu prevented in a packaged build', () => {
      keys = new WindowKeys(() => {}, { packaged: true });
      const target = terminal();
      const event = new MouseEvent('contextmenu', { cancelable: true, bubbles: true });
      target.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(true);
    });
  });
});
