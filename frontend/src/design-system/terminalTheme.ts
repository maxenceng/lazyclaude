const COLOURS = [
  'black',
  'red',
  'green',
  'yellow',
  'blue',
  'magenta',
  'cyan',
  'white',
  'bright-black',
  'bright-red',
  'bright-green',
  'bright-yellow',
  'bright-blue',
  'bright-magenta',
  'bright-cyan',
  'bright-white',
] as const;

export interface TerminalTheme {
  background: string;
  foreground: string;
  cursor: string;
  selectionBackground: string;
  extendedAnsi: string[];
  black: string;
  red: string;
  green: string;
  yellow: string;
  blue: string;
  magenta: string;
  cyan: string;
  white: string;
  brightBlack: string;
  brightRed: string;
  brightGreen: string;
  brightYellow: string;
  brightBlue: string;
  brightMagenta: string;
  brightCyan: string;
  brightWhite: string;
}

export interface TerminalLook {
  theme: TerminalTheme;
  fontFamily: string;
  fontSize: number;
}

/** The terminal's palette and type, read from the tokens in effect on `element`, so the 16 ANSI colours are the board's and no other. */
export const terminalLook = (element: Element): TerminalLook => {
  const style = getComputedStyle(element);
  const token = (name: string) => style.getPropertyValue(`--${name}`).trim();
  const [black, red, green, yellow, blue, magenta, cyan, white, brightBlack, brightRed, brightGreen, brightYellow, brightBlue, brightMagenta, brightCyan, brightWhite] =
    COLOURS.map((colour) => token(`ansi-${colour}`));
  return {
    theme: {
      background: token('terminal-ground'),
      foreground: token('text'),
      cursor: token('text'),
      selectionBackground: token('terminal-selection'),
      extendedAnsi: [],
      black: black ?? '',
      red: red ?? '',
      green: green ?? '',
      yellow: yellow ?? '',
      blue: blue ?? '',
      magenta: magenta ?? '',
      cyan: cyan ?? '',
      white: white ?? '',
      brightBlack: brightBlack ?? '',
      brightRed: brightRed ?? '',
      brightGreen: brightGreen ?? '',
      brightYellow: brightYellow ?? '',
      brightBlue: brightBlue ?? '',
      brightMagenta: brightMagenta ?? '',
      brightCyan: brightCyan ?? '',
      brightWhite: brightWhite ?? '',
    },
    fontFamily: token('font-mono'),
    fontSize: Number.parseInt(token('size-terminal'), 10),
  };
};
