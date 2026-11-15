import { describe, expect, it } from 'vitest';
import { BlockKind, blocksOf, headingSlugs } from './markdown';

describe('the slugs of a document\'s headings', () => {
  it('lowercases a heading, drops its punctuation and joins its words with hyphens', () => {
    expect(headingSlugs('# Getting Started\n\n## What\'s new? (v2)')).toEqual(['getting-started', 'whats-new-v2']);
  });

  it('numbers a repeated slug, as GitHub does', () => {
    expect(headingSlugs('# Setup\n## Setup\n### Setup')).toEqual(['setup', 'setup-1', 'setup-2']);
  });

  it('reads the text of a heading, not its markup', () => {
    expect(headingSlugs('## The `docs` **tree** and [links](x.md)')).toEqual(['the-docs-tree-and-links']);
  });

  it('keeps letters of any script and underscores', () => {
    expect(headingSlugs('# Über_café 日本')).toEqual(['über_café-日本']);
  });

  it('skips a heading line inside a fenced block', () => {
    expect(headingSlugs('```\n# not a heading\n```\n# real')).toEqual(['real']);
  });
});

describe('a fenced block', () => {
  it('keeps the language its opening fence names', () => {
    expect(blocksOf('```mermaid\ngraph TD\n```')).toEqual([{ kind: BlockKind.Code, language: 'mermaid', text: 'graph TD' }]);
  });

  it('keeps only the first word of the info string', () => {
    expect(blocksOf('~~~java title="A"\nint x;\n~~~')).toEqual([{ kind: BlockKind.Code, language: 'java', text: 'int x;' }]);
  });

  it('keeps the language of a fence longer than three backticks', () => {
    expect(blocksOf('````mermaid\ngraph TD\n````')).toEqual([{ kind: BlockKind.Code, language: 'mermaid', text: 'graph TD' }]);
  });

  it('has an empty language when its fence names none', () => {
    expect(blocksOf('```\nplain\n```')).toEqual([{ kind: BlockKind.Code, language: '', text: 'plain' }]);
  });
});

describe('a table', () => {
  it('splits a row into cells at each pipe', () => {
    expect(blocksOf('| a | b |\n|---|---|\n| 1 | 2 |')).toEqual([{ kind: BlockKind.Table, header: ['a', 'b'], rows: [['1', '2']] }]);
  });

  it('reads an escaped pipe as a pipe inside its cell, code spans included', () => {
    expect(blocksOf('| a | b |\n|---|---|\n| x | `A \\| B` and C \\| D |')).toEqual([
      { kind: BlockKind.Table, header: ['a', 'b'], rows: [['x', '`A | B` and C | D']] },
    ]);
  });

  it('drops the cells of a row beyond its header, as GitHub does', () => {
    expect(blocksOf('| a | b |\n|---|---|\n| 1 | `2 | 3` |')).toEqual([{ kind: BlockKind.Table, header: ['a', 'b'], rows: [['1', '`2']] }]);
  });

  it('fills a row shorter than its header with empty cells', () => {
    expect(blocksOf('| a | b | c |\n|---|---|---|\n| 1 |')).toEqual([{ kind: BlockKind.Table, header: ['a', 'b', 'c'], rows: [['1', '', '']] }]);
  });
});

describe('a list', () => {
  it('joins an indented continuation line to its item', () => {
    expect(blocksOf('- first line\n  goes on\n- second')).toEqual([{ kind: BlockKind.List, ordered: false, items: ['first line goes on', 'second'] }]);
  });

  it('joins an unindented continuation line to its item, as GitHub does', () => {
    expect(blocksOf('1. first line\ngoes on\n2. second')).toEqual([{ kind: BlockKind.List, ordered: true, items: ['first line goes on', 'second'] }]);
  });

  it('ends at a blank line', () => {
    expect(blocksOf('- item\n\nafter')).toEqual([
      { kind: BlockKind.List, ordered: false, items: ['item'] },
      { kind: BlockKind.Paragraph, text: 'after' },
    ]);
  });

  it('ends where another block starts', () => {
    expect(blocksOf('- item\n## Next')).toEqual([
      { kind: BlockKind.List, ordered: false, items: ['item'] },
      { kind: BlockKind.Heading, level: 2, text: 'Next', slug: 'next' },
    ]);
  });
});
