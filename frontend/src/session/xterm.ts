import { FitAddon } from '@xterm/addon-fit';
import { Terminal } from '@xterm/xterm';
import type { TerminalLook } from '../design-system';

const SCROLLBACK = 5000;

/** A terminal in the look the design system gives it, with the addon that fits it to its pane. */
export const createXterm = (look: TerminalLook): { terminal: Terminal; fit: FitAddon } => {
  const terminal = new Terminal({
    theme: look.theme,
    fontFamily: look.fontFamily,
    fontSize: look.fontSize,
    scrollback: SCROLLBACK,
    cursorBlink: false,
  });
  const fit = new FitAddon();
  terminal.loadAddon(fit);
  return { terminal, fit };
};
