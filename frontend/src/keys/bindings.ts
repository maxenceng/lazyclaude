import type { Binding } from './chords';

export enum Action {
  MoveDown = 'moveDown',
  MoveUp = 'moveUp',
  PanelLeft = 'panelLeft',
  PanelRight = 'panelRight',
  Top = 'top',
  Bottom = 'bottom',
  Open = 'open',
  Quit = 'quit',
  Refresh = 'refresh',
  Capture = 'capture',
  RemoveWorktree = 'removeWorktree',
  Review = 'review',
  NewTicketSession = 'newTicketSession',
  NewSession = 'newSession',
  ToggleMainView = 'toggleMainView',
  Zoom = 'zoom',
  NextWaiting = 'nextWaiting',
  Back = 'back',
  HalfPageDown = 'halfPageDown',
  HalfPageUp = 'halfPageUp',
  FocusLeft = 'focusLeft',
  FocusRight = 'focusRight',
  FocusDown = 'focusDown',
  FocusUp = 'focusUp',
}

export type KeyPress = Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'altKey' | 'metaKey' | 'shiftKey'>;

const PLAIN: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  j: Action.MoveDown,
  k: Action.MoveUp,
  h: Action.PanelLeft,
  l: Action.PanelRight,
  g: Action.Top,
  G: Action.Bottom,
  Enter: Action.Open,
  q: Action.Quit,
  R: Action.Refresh,
  c: Action.Capture,
  d: Action.RemoveWorktree,
  p: Action.Review,
  n: Action.NewTicketSession,
  N: Action.NewSession,
  m: Action.ToggleMainView,
  z: Action.Zoom,
  w: Action.NextWaiting,
  Escape: Action.Back,
}));

const CTRL: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  c: Action.Quit,
  d: Action.HalfPageDown,
  u: Action.HalfPageUp,
}));

/** What the TUI's key table does with a key, matched on `KeyboardEvent.key` and the modifiers; undefined when the TUI leaves it. */
export const actionOf = (press: KeyPress, reserved: readonly Binding[] = []): Action | undefined => {
  if (press.metaKey) {
    return undefined;
  }
  const chord = reserved.find((binding) => binding.matches(press));
  if (chord !== undefined) {
    return chord.action;
  }
  if (press.ctrlKey && !press.altKey) {
    return CTRL.get(press.key);
  }
  return press.ctrlKey || press.altKey ? undefined : PLAIN.get(press.key);
};
