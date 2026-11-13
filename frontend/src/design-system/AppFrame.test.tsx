import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

const frame = stylesheet('AppFrame.module.css');

describe('the window frame', () => {
  it('gives the main view more of the right column than the dock', () => {
    expect(ruleOf(frame, '.full')).toContain('grid-template-rows: minmax(0, 62fr) minmax(0, 38fr)');
  });
});
