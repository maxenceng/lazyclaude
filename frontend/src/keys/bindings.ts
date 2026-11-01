import { type Binding, matches } from './chords';

export type Action =
  | 'moveDown'
  | 'moveUp'
  | 'panelLeft'
  | 'panelRight'
  | 'top'
  | 'bottom'
  | 'open'
  | 'quit'
  | 'refresh'
  | 'capture'
  | 'removeWorktree'
  | 'review'
  | 'newTicketSession'
  | 'newSession'
  | 'toggleMainView'
  | 'zoom'
  | 'nextWaiting'
  | 'back'
  | 'halfPageDown'
  | 'halfPageUp'
  | 'focusLeft'
  | 'focusRight'
  | 'focusDown'
  | 'focusUp';

export type KeyPress = Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'altKey' | 'metaKey' | 'shiftKey'>;

const PLAIN: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  j: 'moveDown',
  k: 'moveUp',
  h: 'panelLeft',
  l: 'panelRight',
  g: 'top',
  G: 'bottom',
  Enter: 'open',
  q: 'quit',
  R: 'refresh',
  c: 'capture',
  d: 'removeWorktree',
  p: 'review',
  n: 'newTicketSession',
  N: 'newSession',
  m: 'toggleMainView',
  z: 'zoom',
  w: 'nextWaiting',
  Escape: 'back',
}));

const CTRL: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  c: 'quit',
  d: 'halfPageDown',
  u: 'halfPageUp',
}));

/** What the TUI's key table does with a key, matched on `KeyboardEvent.key` and the modifiers; undefined when the TUI leaves it. */
export function actionOf(press: KeyPress, reserved: readonly Binding[] = []): Action | undefined {
  if (press.metaKey) {
    return undefined;
  }
  const chord = reserved.find((binding) => matches(binding, press));
  if (chord !== undefined) {
    return chord.action;
  }
  if (press.ctrlKey && !press.altKey) {
    return CTRL.get(press.key);
  }
  return press.ctrlKey || press.altKey ? undefined : PLAIN.get(press.key);
}
