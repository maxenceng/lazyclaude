import { describe, expect, it } from 'vitest';
import { Action, actionOf, type KeyPress } from './bindings';

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
    ['j', Action.MoveDown],
    ['k', Action.MoveUp],
    ['h', Action.PanelLeft],
    ['l', Action.PanelRight],
    ['g', Action.Top],
    ['G', Action.Bottom],
    ['Enter', Action.Open],
    ['q', Action.Quit],
    ['R', Action.Refresh],
    ['c', Action.Capture],
    ['d', Action.RemoveWorktree],
    ['p', Action.Review],
    ['n', Action.NewTicketSession],
    ['N', Action.NewSession],
    ['m', Action.ToggleMainView],
    ['z', Action.Zoom],
    ['w', Action.NextWaiting],
    ['Escape', Action.Back],
  ])('%s is taken as %s', (key, action) => {
    expect(actionOf(press(key, { shiftKey: key !== key.toLowerCase() }))).toBe(action);
  });

  it.each([
    ['c', Action.Quit],
    ['d', Action.HalfPageDown],
    ['u', Action.HalfPageUp],
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
