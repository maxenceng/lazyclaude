import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

describe('the hover on what a click picks', () => {
  it.each([
    ['a Board row', 'TicketRow.module.css', '.row:hover:not(.selected)'],
    ['a Docs row', 'DocsTreeRow.module.css', '.row:hover:not(.selected)'],
    ['a panel tab', 'PanelTabs.module.css', '.tab:hover:not(.selected)'],
    ['a window tab', 'WindowTabBar.module.css', '.tab:hover:not(.selected)'],
  ])('lifts %s under the pointer with the hover ground, and leaves the selected one as it is', (_name, sheet, selector) => {
    expect(ruleOf(stylesheet(sheet), selector)).toContain('background: var(--hover-ground)');
  });

  it('lifts a short session row under the pointer with the hover ground laid over its own ground, and leaves the selected one and a row following the pointer as they are', () => {
    expect(ruleOf(stylesheet('SessionRow.module.css'), '.row:not(.tall):not(.pointed):hover:not(.selected)'), 'a short row may carry a lamp tile; the hover ground tints it rather than replacing it').toContain(
      'background: linear-gradient(var(--hover-ground), var(--hover-ground)), var(--row-ground, transparent);',
    );
  });

  it('gives a tall session row no hover ground, since its hover lights the action instead', () => {
    const sheet = stylesheet('SessionRow.module.css');
    expect(ruleOf(sheet, '.tall:hover'), 'a tall row is a card on its lamp tile; a ground over it would hide the lamp').not.toContain('background');
    expect(ruleOf(sheet, '.tall:hover .action')).toContain('color: var(--text)');
  });

  it('takes its ground from the ink it sits under, so it reads on a dark pane and on a lit title row', () => {
    expect(stylesheet('tokens.css')).toContain('--hover-ground: color-mix(in srgb, currentColor 8%, transparent);');
  });
});
