import { describe, expect, it } from 'vitest';
import type { SessionConfigurationView } from '../protocol';
import { bindingsOf } from './chords';
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

const configuration = (...chords: SessionConfigurationView['chords']): SessionConfigurationView => {
  return { chords, desktopNotifications: true, mouse: true, ticketPlugin: 'ddd-workflow', statusVerbs: [] };
};

const alt = (character: string): SessionConfigurationView['chords'][number]['chord'] => ({
  modifiers: ['ALT'],
  key: { kind: 'typedCharacter', codePoint: character.codePointAt(0) ?? 0 },
});

describe('the reserved chords from the configuration', () => {
  it('takes a configured typed character with its exact modifiers as its action', () => {
    const reserved = bindingsOf(configuration({ action: 'FOCUS_LEFT', chord: alt('h') }));
    expect(actionOf(press('h', { altKey: true }), reserved)).toBe(Action.FocusLeft);
  });

  it('takes a rebound chord and no longer the default one', () => {
    const reserved = bindingsOf(configuration({ action: 'ZOOM', chord: alt('x') }));
    expect(actionOf(press('x', { altKey: true }), reserved)).toBe(Action.Zoom);
    expect(actionOf(press('z', { altKey: true }), reserved)).toBeUndefined();
  });

  it('does not match when a modifier is missing or extra', () => {
    const reserved = bindingsOf(configuration({ action: 'FOCUS_LEFT', chord: alt('h') }));
    expect(actionOf(press('h'), reserved)).toBe(Action.PanelLeft);
    expect(actionOf(press('h', { altKey: true, ctrlKey: true }), reserved)).toBeUndefined();
    expect(actionOf(press('H', { altKey: true, shiftKey: true }), reserved)).toBeUndefined();
  });

  it('matches a shifted character stored lower case against the upper case key', () => {
    const reserved = bindingsOf(
      configuration({ action: 'NEXT_WAITING', chord: { modifiers: ['ALT', 'SHIFT'], key: { kind: 'typedCharacter', codePoint: 104 } } }),
    );
    expect(actionOf(press('H', { altKey: true, shiftKey: true }), reserved)).toBe(Action.NextWaiting);
    expect(actionOf(press('h', { altKey: true }), reserved)).toBeUndefined();
  });

  it('maps a named key to the KeyboardEvent key name', () => {
    const reserved = bindingsOf(
      configuration(
        { action: 'FOCUS_UP', chord: { modifiers: ['CTRL'], key: { kind: 'pressedKey', key: 'PAGE_UP' } } },
        { action: 'FOCUS_DOWN', chord: { modifiers: [], key: { kind: 'pressedKey', key: 'F5' } } },
      ),
    );
    expect(actionOf(press('PageUp', { ctrlKey: true }), reserved)).toBe(Action.FocusUp);
    expect(actionOf(press('F5'), reserved)).toBe(Action.FocusDown);
    expect(actionOf(press('PAGE_UP', { ctrlKey: true }), reserved)).toBeUndefined();
  });

  it('takes a reserved chord before a fixed key it overlaps', () => {
    const reserved = bindingsOf(configuration({ action: 'ZOOM', chord: { modifiers: ['CTRL'], key: { kind: 'typedCharacter', codePoint: 100 } } }));
    expect(actionOf(press('d', { ctrlKey: true }), reserved)).toBe(Action.Zoom);
  });

  it('takes no alt chord before the configuration has arrived', () => {
    expect(actionOf(press('h', { altKey: true }), [])).toBeUndefined();
  });
});
