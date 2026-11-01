import { describe, expect, it } from 'vitest';
import { actionOf, type KeyPress } from './bindings';

const press = (key: string, held: Partial<KeyPress> = {}): KeyPress => ({
  key,
  ctrlKey: false,
  altKey: false,
  metaKey: false,
  shiftKey: false,
  ...held,
});

describe('the keys the TUI binds', () => {
  it.each([
    ['j', 'moveDown'],
    ['k', 'moveUp'],
    ['h', 'panelLeft'],
    ['l', 'panelRight'],
    ['g', 'top'],
    ['G', 'bottom'],
    ['Enter', 'open'],
    ['q', 'quit'],
    ['R', 'refresh'],
    ['c', 'capture'],
    ['d', 'removeWorktree'],
    ['p', 'review'],
    ['n', 'newTicketSession'],
    ['N', 'newSession'],
    ['m', 'toggleMainView'],
    ['z', 'zoom'],
    ['w', 'nextWaiting'],
    ['Escape', 'back'],
  ])('%s is taken as %s', (key, action) => {
    expect(actionOf(press(key, { shiftKey: key !== key.toLowerCase() }))).toBe(action);
  });

  it.each([
    ['c', 'quit'],
    ['d', 'halfPageDown'],
    ['u', 'halfPageUp'],
  ])('ctrl+%s is taken as %s', (key, action) => {
    expect(actionOf(press(key, { ctrlKey: true }))).toBe(action);
  });

  it('does not take an alt chord on its own, since only the configuration reserves them', () => {
    expect(actionOf(press('h', { altKey: true }))).toBeUndefined();
  });

  it('leaves a key the TUI does not bind to the webview', () => {
    expect(actionOf(press('x'))).toBeUndefined();
    expect(actionOf(press('F12'))).toBeUndefined();
  });

  it('does not take a plain letter when the meta key is held', () => {
    expect(actionOf(press('j', { metaKey: true }))).toBeUndefined();
  });

  it('does not take ctrl+j, which is not the TUI key j', () => {
    expect(actionOf(press('j', { ctrlKey: true }))).toBeUndefined();
  });

  it('does not mistake a key named like an object property for a binding', () => {
    expect(actionOf(press('constructor'))).toBeUndefined();
  });
});
