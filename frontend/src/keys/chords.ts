import type { ConfigurationView } from '../protocol';
import type { Action, KeyPress } from './bindings';

export interface Binding {
  action: Action;
  modifiers: { ctrl: boolean; alt: boolean; shift: boolean };
  key: { kind: 'character'; character: string } | { kind: 'named'; name: string };
}

type Reserved = ConfigurationView['chords'][number]['action'];
type NamedKey = Extract<ConfigurationView['chords'][number]['chord']['key'], { kind: 'pressedKey' }>['key'];

const ACTIONS: Readonly<Record<Reserved, Action>> = {
  FOCUS_LEFT: 'focusLeft',
  FOCUS_RIGHT: 'focusRight',
  FOCUS_DOWN: 'focusDown',
  FOCUS_UP: 'focusUp',
  HIDE_MAIN_VIEW: 'toggleMainView',
  ZOOM: 'zoom',
  NEXT_WAITING: 'nextWaiting',
};

const NAMES: Readonly<Record<string, string>> = {
  ENTER: 'Enter',
  ESCAPE: 'Escape',
  BACKSPACE: 'Backspace',
  TAB: 'Tab',
  DELETE: 'Delete',
  INSERT: 'Insert',
  UP: 'ArrowUp',
  DOWN: 'ArrowDown',
  LEFT: 'ArrowLeft',
  RIGHT: 'ArrowRight',
  HOME: 'Home',
  END: 'End',
  PAGE_UP: 'PageUp',
  PAGE_DOWN: 'PageDown',
};

function keyName(key: NamedKey): string {
  return NAMES[key] ?? key;
}

export function bindingsOf(configuration: ConfigurationView): Binding[] {
  return configuration.chords.map(({ action, chord }) => ({
    action: ACTIONS[action],
    modifiers: {
      ctrl: chord.modifiers.includes('CTRL'),
      alt: chord.modifiers.includes('ALT'),
      shift: chord.modifiers.includes('SHIFT'),
    },
    key:
      chord.key.kind === 'typedCharacter'
        ? { kind: 'character', character: String.fromCodePoint(chord.key.codePoint) }
        : { kind: 'named', name: keyName(chord.key.key) },
  }));
}

/** A chord is pressed when the key and the modifier set are exactly its own; characters are stored lower case, so a shifted one is compared lower case. */
export function matches(binding: Binding, press: KeyPress): boolean {
  const { ctrl, alt, shift } = binding.modifiers;
  if (press.metaKey || press.ctrlKey !== ctrl || press.altKey !== alt || press.shiftKey !== shift) {
    return false;
  }
  return binding.key.kind === 'character' ? press.key.toLowerCase() === binding.key.character : press.key === binding.key.name;
}
