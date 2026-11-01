import { describe, expect, it } from 'vitest';
import type { ConfigurationView } from '../protocol';
import { bindingsOf } from './chords';
import { actionOf, type KeyPress } from './bindings';

const press = (key: string, held: Partial<KeyPress> = {}): KeyPress => ({
  key,
  ctrlKey: false,
  altKey: false,
  metaKey: false,
  shiftKey: false,
  ...held,
});

function configuration(...chords: ConfigurationView['chords']): ConfigurationView {
  return { chords, desktopNotifications: true, mouse: true };
}

const alt = (character: string): ConfigurationView['chords'][number]['chord'] => ({
  modifiers: ['ALT'],
  key: { kind: 'typedCharacter', codePoint: character.codePointAt(0) ?? 0 },
});

describe('the reserved chords from the configuration', () => {
  it('takes a configured typed character with its exact modifiers as its action', () => {
    const reserved = bindingsOf(configuration({ action: 'FOCUS_LEFT', chord: alt('h') }));
    expect(actionOf(press('h', { altKey: true }), reserved)).toBe('focusLeft');
  });

  it('takes a rebound chord and no longer the default one', () => {
    const reserved = bindingsOf(configuration({ action: 'ZOOM', chord: alt('x') }));
    expect(actionOf(press('x', { altKey: true }), reserved)).toBe('zoom');
    expect(actionOf(press('z', { altKey: true }), reserved)).toBeUndefined();
  });

  it('does not match when a modifier is missing or extra', () => {
    const reserved = bindingsOf(configuration({ action: 'FOCUS_LEFT', chord: alt('h') }));
    expect(actionOf(press('h'), reserved)).toBe('panelLeft');
    expect(actionOf(press('h', { altKey: true, ctrlKey: true }), reserved)).toBeUndefined();
    expect(actionOf(press('H', { altKey: true, shiftKey: true }), reserved)).toBeUndefined();
  });

  it('matches a shifted character stored lower case against the upper case key', () => {
    const reserved = bindingsOf(
      configuration({ action: 'NEXT_WAITING', chord: { modifiers: ['ALT', 'SHIFT'], key: { kind: 'typedCharacter', codePoint: 104 } } }),
    );
    expect(actionOf(press('H', { altKey: true, shiftKey: true }), reserved)).toBe('nextWaiting');
    expect(actionOf(press('h', { altKey: true }), reserved)).toBeUndefined();
  });

  it('maps a named key to the KeyboardEvent key name', () => {
    const reserved = bindingsOf(
      configuration(
        { action: 'FOCUS_UP', chord: { modifiers: ['CTRL'], key: { kind: 'pressedKey', key: 'PAGE_UP' } } },
        { action: 'FOCUS_DOWN', chord: { modifiers: [], key: { kind: 'pressedKey', key: 'F5' } } },
      ),
    );
    expect(actionOf(press('PageUp', { ctrlKey: true }), reserved)).toBe('focusUp');
    expect(actionOf(press('F5'), reserved)).toBe('focusDown');
    expect(actionOf(press('PAGE_UP', { ctrlKey: true }), reserved)).toBeUndefined();
  });

  it('takes a reserved chord before a fixed key it overlaps', () => {
    const reserved = bindingsOf(configuration({ action: 'ZOOM', chord: { modifiers: ['CTRL'], key: { kind: 'typedCharacter', codePoint: 100 } } }));
    expect(actionOf(press('d', { ctrlKey: true }), reserved)).toBe('zoom');
  });

  it('takes no alt chord before the configuration has arrived', () => {
    expect(actionOf(press('h', { altKey: true }), [])).toBeUndefined();
  });
});
