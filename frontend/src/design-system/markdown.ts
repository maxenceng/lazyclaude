export enum BlockKind {
  Heading = 'heading',
  Paragraph = 'paragraph',
  Code = 'code',
  List = 'list',
  Quote = 'quote',
  Rule = 'rule',
  Table = 'table',
}

export interface Heading {
  kind: BlockKind.Heading;
  level: number;
  text: string;
  slug: string;
}

export interface Paragraph {
  kind: BlockKind.Paragraph;
  text: string;
}

export interface Code {
  kind: BlockKind.Code;
  text: string;
}

export interface List {
  kind: BlockKind.List;
  ordered: boolean;
  items: string[];
}

export interface Quote {
  kind: BlockKind.Quote;
  text: string;
}

export interface Rule {
  kind: BlockKind.Rule;
}

export interface Table {
  kind: BlockKind.Table;
  header: string[];
  rows: string[][];
}

export type Block = Heading | Paragraph | Code | List | Quote | Rule | Table;

export enum InlineKind {
  Text = 'text',
  Code = 'code',
  Strong = 'strong',
  Emphasis = 'emphasis',
  Link = 'link',
}

export interface TextRun {
  kind: InlineKind.Text;
  text: string;
}

export interface CodeRun {
  kind: InlineKind.Code;
  text: string;
}

export interface Styled {
  kind: InlineKind.Strong | InlineKind.Emphasis;
  children: Inline[];
}

export interface LinkRun {
  kind: InlineKind.Link;
  href: string;
  children: Inline[];
}

export type Inline = TextRun | CodeRun | Styled | LinkRun;

const FENCE = /^\s{0,3}(```|~~~)/;
const HEADING = /^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$/;
const RULE = /^\s{0,3}([-*_])(\s*\1){2,}\s*$/;
const BULLET = /^\s*[-*+]\s+(.*)$/;
const NUMBERED = /^\s*\d+[.)]\s+(.*)$/;
const QUOTE = /^\s{0,3}>\s?(.*)$/;
const TABLE_RULE = /^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)*\|?\s*$/;
const TABLE_ROW = /^(.*\|.*)$/;
const LEADING_PIPE = /^\|/;
const TRAILING_PIPE = /\|$/;
const LINE_BREAK = /\r\n?/g;
/** What GitHub drops from a heading to make its slug: anything but a letter, digit, combining mark, `_`, `-` or a space. */
const NOT_IN_SLUG = /[^\p{L}\p{N}\p{M}_\s-]/gu;
/** A space in a heading, which its slug writes as `-`. */
const SLUG_SPACE = /\s/g;
const INLINE = /(`+)([^`]|[^`][\s\S]*?[^`])\1(?!`)|!?\[((?:[^\][]|\[[^\]]*\])*)\]\(\s*<?([^\s)>]*)>?(?:\s+"[^"]*")?\s*\)|\*\*(.+?)\*\*|__(.+?)__|\*([^*\s][^*]*?)\*|\b_([^_\s][^_]*?)_\b/;

const plain = (inlines: Inline[]): string => {
  return inlines.map((inline) => (inline.kind === InlineKind.Text || inline.kind === InlineKind.Code ? inline.text : plain(inline.children))).join('');
};

/** The runs of one line of text: code, links, strong, emphasis; an image reads as its alt text, and anything else as it was written. */
export const inlineOf = (source: string): Inline[] => {
  const match = INLINE.exec(source);
  if (match === null) {
    return source === '' ? [] : [{ kind: InlineKind.Text, text: source }];
  }
  const before = source.slice(0, match.index);
  const after = source.slice(match.index + match[0].length);
  const lead: Inline[] = before === '' ? [] : [{ kind: InlineKind.Text, text: before }];
  const [whole, , code, label, href, strong, strongUnderscore, emphasis, emphasisUnderscore] = match;
  let found: Inline[];
  if (code !== undefined) {
    found = [{ kind: InlineKind.Code, text: code.trim() }];
  } else if (label !== undefined) {
    found = whole.startsWith('!') ? inlineOf(label) : [{ kind: InlineKind.Link, href: href ?? '', children: inlineOf(label) }];
  } else if (strong !== undefined || strongUnderscore !== undefined) {
    found = [{ kind: InlineKind.Strong, children: inlineOf(strong ?? strongUnderscore ?? '') }];
  } else {
    found = [{ kind: InlineKind.Emphasis, children: inlineOf(emphasis ?? emphasisUnderscore ?? '') }];
  }
  return [...lead, ...found, ...inlineOf(after)];
};

const slugged = (text: string): string => {
  return plain(inlineOf(text))
    .toLowerCase()
    .replace(NOT_IN_SLUG, '')
    .trim()
    .replace(SLUG_SPACE, '-');
};

const cells = (line: string): string[] => {
  return line
    .trim()
    .replace(LEADING_PIPE, '')
    .replace(TRAILING_PIPE, '')
    .split('|')
    .map((cell) => cell.trim());
};

interface Reading {
  blocks: Block[];
  next: number;
}

const readFence = (lines: string[], start: number): Reading => {
  const close = lines.findIndex((line, index) => index > start && FENCE.test(line));
  const end = close === -1 ? lines.length : close;
  return { blocks: [{ kind: BlockKind.Code, text: lines.slice(start + 1, end).join('\n') }], next: end + 1 };
};

const readRun = (lines: string[], start: number, pattern: RegExp): string[] => {
  const end = lines.findIndex((line, index) => index >= start && !pattern.test(line));
  return lines.slice(start, end === -1 ? lines.length : end).map((line) => pattern.exec(line)?.[1] ?? '');
};

const startsBlock = (line: string, following: string | undefined): boolean => {
  return (
    FENCE.test(line) ||
    HEADING.test(line) ||
    RULE.test(line) ||
    BULLET.test(line) ||
    NUMBERED.test(line) ||
    QUOTE.test(line) ||
    (line.includes('|') && following !== undefined && TABLE_RULE.test(following))
  );
};

const readBlock = (lines: string[], start: number): Reading => {
  const line = lines[start] ?? '';
  const heading = HEADING.exec(line);
  if (FENCE.test(line)) {
    return readFence(lines, start);
  }
  if (heading !== null) {
    return { blocks: [{ kind: BlockKind.Heading, level: heading[1]?.length ?? 1, text: heading[2] ?? '', slug: '' }], next: start + 1 };
  }
  if (RULE.test(line)) {
    return { blocks: [{ kind: BlockKind.Rule }], next: start + 1 };
  }
  if (BULLET.test(line) || NUMBERED.test(line)) {
    const ordered = !BULLET.test(line);
    const items = readRun(lines, start, ordered ? NUMBERED : BULLET);
    return { blocks: [{ kind: BlockKind.List, ordered, items }], next: start + items.length };
  }
  if (QUOTE.test(line)) {
    const quoted = readRun(lines, start, QUOTE);
    return { blocks: [{ kind: BlockKind.Quote, text: quoted.join(' ') }], next: start + quoted.length };
  }
  const following = lines[start + 1];
  if (line.includes('|') && following !== undefined && TABLE_RULE.test(following)) {
    const rows = readRun(lines, start + 2, TABLE_ROW);
    return { blocks: [{ kind: BlockKind.Table, header: cells(line), rows: rows.map(cells) }], next: start + 2 + rows.length };
  }
  const end = lines.findIndex((candidate, index) => index > start && (candidate.trim() === '' || startsBlock(candidate, lines[index + 1])));
  const last = end === -1 ? lines.length : end;
  return { blocks: [{ kind: BlockKind.Paragraph, text: lines.slice(start, last).map((text) => text.trim()).join(' ') }], next: last };
};

const blocksFrom = (lines: string[], start: number): Block[] => {
  if (start >= lines.length) {
    return [];
  }
  if ((lines[start] ?? '').trim() === '') {
    return blocksFrom(lines, start + 1);
  }
  const reading = readBlock(lines, start);
  return [...reading.blocks, ...blocksFrom(lines, reading.next)];
};

const withSlugs = (blocks: Block[]): Block[] => {
  const seen = new Map<string, number>();
  return blocks.map((block) => {
    if (block.kind !== BlockKind.Heading) {
      return block;
    }
    const base = slugged(block.text);
    const count = seen.get(base) ?? 0;
    seen.set(base, count + 1);
    return { ...block, slug: count === 0 ? base : `${base}-${count}` };
  });
};

/** A document's blocks, its headings carrying the slug a link's fragment names. */
export const blocksOf = (source: string): Block[] => withSlugs(blocksFrom(source.replace(LINE_BREAK, '\n').split('\n'), 0));

/** The slugs of a document's headings, in order. */
export const headingSlugs = (source: string): string[] => {
  return blocksOf(source).flatMap((block) => (block.kind === BlockKind.Heading ? [block.slug] : []));
};
