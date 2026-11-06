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
  Range = 'range',
  Delete = 'delete',
  Submit = 'submit',
  NextFile = 'nextFile',
  PreviousFile = 'previousFile',
  Resolved = 'resolved',
  Yes = 'yes',
  ShowBase = 'showBase',
  ShowBoard = 'showBoard',
  ShowPullRequests = 'showPullRequests',
}

export type KeyPress = Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'altKey' | 'metaKey' | 'shiftKey'>;

/** A key pressed on the window, which also says where the key sits on the keyboard. */
export type WindowKeyPress = KeyPress & Pick<KeyboardEvent, 'code'>;

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
  v: Action.Range,
  x: Action.Delete,
  S: Action.Submit,
  J: Action.NextFile,
  K: Action.PreviousFile,
  r: Action.Resolved,
  y: Action.Yes,
  '1': Action.ShowBase,
  '2': Action.ShowBoard,
  '3': Action.ShowPullRequests,
}));

/** The digit row's keys by position, so a layout whose digits need shift, as AZERTY's, chooses a tab with the same keys. */
const TAB_POSITIONS: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  Digit1: Action.ShowBase,
  Digit2: Action.ShowBoard,
  Digit3: Action.ShowPullRequests,
}));

const CTRL: ReadonlyMap<string, Action> = new Map<string, Action>(Object.entries({
  c: Action.Quit,
  d: Action.HalfPageDown,
  u: Action.HalfPageUp,
}));

/** What the window does with a key: the TUI's key table matched on `KeyboardEvent.key` and the modifiers, and the digits that choose a window tab, by character or by their key on the digit row; undefined when the TUI leaves it. */
export const actionOf = (press: WindowKeyPress, reserved: readonly Binding[] = []): Action | undefined => {
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
  return press.ctrlKey || press.altKey ? undefined : (TAB_POSITIONS.get(press.code) ?? PLAIN.get(press.key));
};
