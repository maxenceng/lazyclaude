import type { SessionConfigurationView } from '../protocol';
import { Action, type KeyPress } from './bindings';

export enum KeyKind {
  Character = 'character',
  Named = 'named',
}

/** A typed character, stored lower case so a shifted press still reads as it. */
export class CharacterKey {
  readonly kind = KeyKind.Character;

  constructor(readonly character: string) {}

  matches(key: string): boolean {
    return key.toLowerCase() === this.character;
  }
}

/** A key with a name, as `KeyboardEvent.key` spells it. */
export class NamedKey {
  readonly kind = KeyKind.Named;

  constructor(readonly name: string) {}

  matches(key: string): boolean {
    return key === this.name;
  }
}

export type ChordKey = CharacterKey | NamedKey;

/** The exact set of modifiers a chord is pressed with; Meta is never one of them. */
export class Modifiers {
  constructor(
    readonly ctrl: boolean,
    readonly alt: boolean,
    readonly shift: boolean,
  ) {}

  heldIn(press: KeyPress): boolean {
    return !press.metaKey && press.ctrlKey === this.ctrl && press.altKey === this.alt && press.shiftKey === this.shift;
  }
}

/** A reserved chord from `config.yml` and the action it stands for. */
export class Binding {
  constructor(
    readonly action: Action,
    readonly modifiers: Modifiers,
    readonly key: ChordKey,
  ) {}

  /** Pressed when the key and the modifier set are exactly its own. */
  matches(press: KeyPress): boolean {
    return this.modifiers.heldIn(press) && this.key.matches(press.key);
  }
}

type Reserved = SessionConfigurationView['chords'][number]['action'];
type PressedKeyName = Extract<SessionConfigurationView['chords'][number]['chord']['key'], { kind: 'pressedKey' }>['key'];

const ACTIONS: Readonly<Record<Reserved, Action>> = {
  FOCUS_LEFT: Action.FocusLeft,
  FOCUS_RIGHT: Action.FocusRight,
  FOCUS_DOWN: Action.FocusDown,
  FOCUS_UP: Action.FocusUp,
  HIDE_MAIN_VIEW: Action.ToggleMainView,
  ZOOM: Action.Zoom,
  NEXT_WAITING: Action.NextWaiting,
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

const keyName = (key: PressedKeyName): string => NAMES[key] ?? key;

export const bindingsOf = (configuration: SessionConfigurationView): Binding[] =>
  configuration.chords.map(
    ({ action, chord }) =>
      new Binding(
        ACTIONS[action],
        new Modifiers(chord.modifiers.includes('CTRL'), chord.modifiers.includes('ALT'), chord.modifiers.includes('SHIFT')),
        chord.key.kind === 'typedCharacter' ? new CharacterKey(String.fromCodePoint(chord.key.codePoint)) : new NamedKey(keyName(chord.key.key)),
      ),
  );
