import { describe, expect, it } from 'vitest';
import { ruleOf, stylesheet } from './stylesheet';

const markdown = stylesheet('Markdown.module.css');
const detail = stylesheet('Detail.module.css');
const document = stylesheet('Document.module.css');

describe('the markdown column', () => {
  it('is set in the reading size, so the measure is 80 characters of that type', () => {
    expect(ruleOf(markdown, '.markdown')).toContain('font-size: var(--reading-size)');
  });

  it('is centred by tracks that split a wide pane evenly and leave a narrow one no offset', () => {
    expect(ruleOf(markdown, '.markdown')).toContain('grid-template-columns: var(--reading-columns)');
    expect(ruleOf(markdown, '.markdown > *')).toContain('grid-column: column-start / column-end');
  });

  it('lets code and tables start at the column and run right to the pane edge, no wider than their content', () => {
    const breakout = ruleOf(markdown, '.markdown > :is(pre, .tableScroll)');
    expect(breakout).toContain('grid-column: column-start / full-end');
    expect(breakout).toContain('justify-self: start');
    expect(breakout).toContain('max-width: 100%');
    expect(breakout).toContain('width: fit-content');
  });

  it('keeps a code line wider than the pane scrolling inside it', () => {
    expect(ruleOf(markdown, '.markdown pre')).toContain('overflow-x: auto');
    expect(ruleOf(markdown, '.tableScroll')).toContain('overflow-x: auto');
  });

  it('sets headings and code in proportion to the reading size', () => {
    expect(ruleOf(markdown, '.markdown h1')).toContain('font-size: var(--reading-title-em)');
    expect(ruleOf(markdown, '.markdown pre')).toContain('var(--reading-code-em)');
  });
});

describe('the frame of a main view', () => {
  it.each([
    ['a ticket detail', detail, '.pane'],
    ['a document', document, '.pane'],
  ])('measures %s against the pane, not its own box', (_name, sheet, selector) => {
    expect(ruleOf(sheet, selector)).toContain('container-type: inline-size');
  });

  it('puts the facts and the title on the column the text is on', () => {
    expect(ruleOf(detail, '.detail')).toContain('grid-template-columns: var(--reading-columns)');
    expect(ruleOf(detail, '.detail > *')).toContain('grid-column: column-start / column-end');
  });
});
