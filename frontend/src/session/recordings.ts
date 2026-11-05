import { CsiFinal, CsiPrefix, FULL_RESET_FINAL, KittyFlag, PrivateMode, SOFT_RESET_INTERMEDIATE } from './privateModes';

export const ESC = '\x1b';
const CSI = `${ESC}[`;

const set = (...modes: PrivateMode[]) => `${CSI}${CsiPrefix.PrivateMode}${modes.join(';')}${CsiFinal.Set}`;
const reset = (...modes: PrivateMode[]) => `${CSI}${CsiPrefix.PrivateMode}${modes.join(';')}${CsiFinal.Reset}`;
const pushKeyboard = (flags: number) => `${CSI}${CsiPrefix.KeyboardPush}${flags}${CsiFinal.Keyboard}`;
const QUERY_KEYBOARD = `${CSI}${CsiPrefix.PrivateMode}${CsiFinal.Keyboard}`;
const CLEAR_SCREEN_AND_HOME = `${CSI}2J${CSI}H`;

/** What `claude`'s fullscreen UI sends at start, in the order the byte census of docs/research/embedding-claude-on-a-pty.md saw it. */
export const CLAUDE_START = [
  QUERY_KEYBOARD,
  pushKeyboard(KittyFlag.Disambiguate),
  set(PrivateMode.AlternateScreen),
  CLEAR_SCREEN_AND_HOME,
  set(PrivateMode.BracketedPaste),
  set(PrivateMode.FocusReports),
  set(PrivateMode.ColourSchemeReports),
  set(PrivateMode.MouseButtons, PrivateMode.MouseDrag, PrivateMode.MouseMotion),
  set(PrivateMode.SgrMouse),
  reset(PrivateMode.CursorVisible),
  'Welcome to Claude Code',
].join('');

export const CLAUDE_LEAVES_ALTERNATE_SCREEN = [reset(PrivateMode.AlternateScreen), set(PrivateMode.CursorVisible)].join('');
export const CLAUDE_STOPS_REPORTING_THE_MOUSE = reset(PrivateMode.MouseButtons, PrivateMode.MouseDrag, PrivateMode.MouseMotion, PrivateMode.SgrMouse);
export const CLAUDE_POPS_KITTY_KEYBOARD = `${CSI}${CsiPrefix.KeyboardPop}${CsiFinal.Keyboard}`;
export const CLAUDE_PUSHES_ANOTHER_KITTY_KEYBOARD = pushKeyboard(KittyFlag.Disambiguate | KittyFlag.ReportAlternateKeys);
export const SOFT_RESET = `${CSI}${SOFT_RESET_INTERMEDIATE}${CsiFinal.SoftReset}`;
export const FULL_RESET = `${ESC}${FULL_RESET_FINAL}`;
