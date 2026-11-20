import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

const menu = stylesheet('WindowMenu.module.css');

describe('the window menu', () => {
  it('inks its item, whose only entry is Quit, in the destructive-action ink rather than a CI one', () => {
    expect(ruleOf(menu, '.item')).toContain('color: var(--danger)');
  });
});
