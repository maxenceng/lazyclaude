import { describe, expect, it } from 'vitest';
import { Action, actionOf, type WindowKeyPress } from './bindings';

const press = (key: string, held: Partial<WindowKeyPress> = {}): WindowKeyPress => ({
  key,
  code: '',
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
    ['M', Action.ToggleDockMinimised],
    ['z', Action.Zoom],
    ['w', Action.NextWaiting],
    ['Escape', Action.Back],
    ['v', Action.Range],
    ['x', Action.Delete],
    ['S', Action.Submit],
    ['J', Action.NextFile],
    ['K', Action.PreviousFile],
    ['r', Action.Resolved],
    ['y', Action.Yes],
    ['1', Action.ShowBase],
    ['2', Action.ShowBoard],
    ['3', Action.ShowPullRequests],
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

  it.each([
    ['&', 'Digit1', Action.ShowBase],
    ['é', 'Digit2', Action.ShowBoard],
    ['"', 'Digit3', Action.ShowPullRequests],
  ])('takes %s typed on the digit row key %s, as on AZERTY, as %s', (key, code, action) => {
    expect(actionOf(press(key, { code }))).toBe(action);
  });

  it('leaves a key the TUI does not bind to the webview', () => {
    expect(actionOf(press('a'))).toBeUndefined();
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
