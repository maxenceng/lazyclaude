import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

const Z_INDEX = /z-index: (\d+)/;

const layerOf = (sheet: string, selector: string): number => Number(Z_INDEX.exec(ruleOf(stylesheet(sheet), selector))?.[1] ?? 0);

describe('what stacks over the diff', () => {
  it.each([
    ['a dialog', 'Dialog.module.css', '.scrim'],
    ['a popup', 'Popup.module.css', '.popup'],
  ])('draws %s over the sticky rule of a file, whatever comes first in the page', (_name, sheet, selector) => {
    expect(layerOf(sheet, selector), 'a positioned box with no z-index paints under any z-index of 1, so the file name covered the comment box').toBeGreaterThan(
      layerOf('DiffRule.module.css', '.sticky'),
    );
  });
});
