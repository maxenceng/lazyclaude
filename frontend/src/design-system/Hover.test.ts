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

  it('takes its ground from the ink it sits under, so it reads on a dark pane and on a lit title row', () => {
    expect(stylesheet('tokens.css')).toContain('--hover-ground: color-mix(in srgb, currentColor 8%, transparent);');
  });
});
