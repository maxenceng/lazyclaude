import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

const sheet = stylesheet('TerminalSurface.module.css');

describe('the terminal surface', () => {
  it('grounds the whole terminal, under its last row too, in the terminal ground', () => {
    expect(ruleOf(sheet, '.surface :global(.xterm-viewport)'), 'xterm.css paints its viewport black, which shows under a last row that does not fill the pane').toContain(
      'background-color: var(--terminal-ground)',
    );
  });
});
