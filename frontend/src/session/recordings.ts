export const ESC = '\x1b';

const set = (...modes: number[]) => `${ESC}[?${modes.join(';')}h`;
const reset = (...modes: number[]) => `${ESC}[?${modes.join(';')}l`;

/** What `claude`'s fullscreen UI sends at start, in the order the byte census of docs/research/embedding-claude-on-a-pty.md saw it. */
export const CLAUDE_START = [
  `${ESC}[?u`,
  `${ESC}[>1u`,
  set(1049),
  `${ESC}[2J${ESC}[H`,
  set(2004),
  set(1004),
  set(2031),
  set(1000, 1002, 1003),
  set(1006),
  reset(25),
  'Welcome to Claude Code',
].join('');

export const CLAUDE_LEAVES_ALTERNATE_SCREEN = [reset(1049), set(25)].join('');
export const CLAUDE_STOPS_REPORTING_THE_MOUSE = reset(1000, 1002, 1003, 1006);
export const CLAUDE_POPS_KITTY_KEYBOARD = `${ESC}[<u`;
export const CLAUDE_PUSHES_ANOTHER_KITTY_KEYBOARD = `${ESC}[>5u`;
export const SOFT_RESET = `${ESC}[!p`;
export const FULL_RESET = `${ESC}c`;
