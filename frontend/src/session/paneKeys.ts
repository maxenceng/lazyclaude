export enum PaneKey {
  Reserved = 'reserved',
  Suspend = 'suspend',
  Copy = 'copy',
  Paste = 'paste',
  Terminal = 'terminal',
}

export interface PaneKeyPress {
  key: string;
  ctrlKey: boolean;
  altKey: boolean;
  metaKey: boolean;
  shiftKey: boolean;
}

const onlyCtrl = (press: PaneKeyPress) => press.ctrlKey && !press.altKey && !press.metaKey && !press.shiftKey;
const ctrlShift = (press: PaneKeyPress) => press.ctrlKey && press.shiftKey && !press.altKey && !press.metaKey;
const onlyShift = (press: PaneKeyPress) => press.shiftKey && !press.ctrlKey && !press.altKey && !press.metaKey;

/**
 * What a pane does with a key (ADR 0058): a reserved chord and `ctrl+z` never reach `claude`, `ctrl+shift+c` copies,
 * `ctrl+shift+v` and `shift+insert` paste, and every other key, `ctrl+c` and a plain `ctrl+v` among them, is the terminal's.
 */
export const paneKeyOf = (press: PaneKeyPress, isReserved: (press: PaneKeyPress) => boolean): PaneKey => {
  const key = press.key.toLowerCase();
  if (isReserved(press)) {
    return PaneKey.Reserved;
  }
  if (onlyCtrl(press) && key === 'z') {
    return PaneKey.Suspend;
  }
  if (ctrlShift(press) && key === 'c') {
    return PaneKey.Copy;
  }
  if ((ctrlShift(press) && key === 'v') || (onlyShift(press) && key === 'insert')) {
    return PaneKey.Paste;
  }
  return PaneKey.Terminal;
};
