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

  it('has an empty language when its fence names none', () => {
    expect(blocksOf('```\nplain\n```')).toEqual([{ kind: BlockKind.Code, language: '', text: 'plain' }]);
  });
});
