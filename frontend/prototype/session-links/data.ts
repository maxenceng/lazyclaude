// PROTOTYPE: canned Claude Code output with a plain URL and an OSC 8 hyperlink. Not shipped.
const ESC = '\x1b';
const OSC8 = (url: string, text: string) => `${ESC}]8;;${url}${ESC}\\${text}${ESC}]8;;${ESC}\\`;
const DIM = (s: string) => `${ESC}[2m${s}${ESC}[0m`;
const BOLD = (s: string) => `${ESC}[1m${s}${ESC}[0m`;

export const PLAIN_URL = 'https://github.com/maxenceng/lazyclaude/pull/227';
export const OSC8_URL = 'https://github.com/maxenceng/lazyclaude/actions/runs/19362844107';

export const LINES = [
  `${BOLD('>')} open the pull request and tell me when CI is green`,
  '',
  `${ESC}[32m●${ESC}[0m Bash(gh pr create --fill)`,
  `  ⎿  ${PLAIN_URL}`,
  '',
  `${ESC}[32m●${ESC}[0m The pull request is open. CI is running — see the ${OSC8('' + OSC8_URL, 'checks run')} for progress.`,
  `  ${DIM('Docs: https://docs.github.com/en/actions')}`,
  '',
  `${ESC}[2m────────────────────────────────────────────────────────────────────────────────${ESC}[0m`,
  `${BOLD('>')} `,
];

export const PLAIN_AT = { row: 3, col: 5 };
export const OSC8_AT = { row: 5, col: 54 };
