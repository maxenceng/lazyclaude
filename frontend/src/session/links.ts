import type { IBuffer, IBufferCellPosition, IBufferRange, IDisposable, ILink, ILinkDecorations, ILinkProvider, Terminal } from '@xterm/xterm';
import { i18n } from '../i18n/i18n';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';
import type { UnderlineLayer } from './underlines';

/** One web address in a line of output, and the index of its first character in that line. */
export interface WebAddress {
  url: string;
  start: number;
}

const WEB_ADDRESS = /https?:\/\/[^\s<>"'`]+/g;
const SENTENCE_PUNCTUATION = /[.,;:!?}]$/;
const WEB_SCHEMES = ['http:', 'https:'];

/** Whether a link is one this pane opens: an http or https address the URL parser reads. */
const isWeb = (target: string): boolean => {
  try {
    return WEB_SCHEMES.includes(new URL(target).protocol);
  } catch {
    return false;
  }
};

const count = (text: string, character: string): number => text.split(character).length - 1;

const closesUnopened = (url: string, opening: string, closing: string): boolean =>
  url.endsWith(closing) && count(url, closing) > count(url, opening);

/** Drops what ends a sentence rather than the address: punctuation, and a closing bracket the address never opened. */
const trimmed = (url: string): string =>
  closesUnopened(url, '(', ')') || closesUnopened(url, '[', ']') || SENTENCE_PUNCTUATION.test(url) ? trimmed(url.slice(0, -1)) : url;

/** The http and https addresses in one line, left to right; any other scheme, or a scheme with no host, is not a link here. */
export const webAddressesIn = (line: string): WebAddress[] =>
  [...line.matchAll(WEB_ADDRESS)].map((found) => ({ url: trimmed(found[0]), start: found.index })).filter((address) => isWeb(address.url));

/** What opens a web address in the system browser. */
export interface LinkOpener {
  openUrl(url: string): Promise<void>;
}

export interface SessionLinksDeps {
  opener: LinkOpener;
  notices: NoticeStore;
}

/** The links of one session pane: followed with ctrl held, and the web address the pointer is on. */
export class SessionLinks {
  readonly hovered: Store<string | undefined> = new Store<string | undefined>(undefined);

  constructor(private readonly deps: SessionLinksDeps) {}

  /** Names only a link ctrl+click would open, so the key bar never offers one it then refuses. */
  hover(url: string): void {
    this.hovered.set(isWeb(url) ? url : undefined);
  }

  leave(): void {
    this.hovered.set(undefined);
  }

  activate(event: MouseEvent, url: string): void {
    if (!event.ctrlKey) {
      return;
    }
    this.deps.notices.clear();
    if (isWeb(url)) {
      this.deps.opener.openUrl(url).catch((error: unknown) => {
        const reason = error instanceof Error ? error.message : String(error);
        this.deps.notices.raise(i18n.t('session:links.openFailed', { url, reason }));
      });
    } else {
      this.deps.notices.raise(i18n.t('session:links.refused', { url }));
    }
  }
}

/** One logical line of a pane, its wrapped rows joined: its text, and the one-based cell each of its UTF-16 units starts in. */
interface LogicalLine {
  text: string;
  cells: IBufferCellPosition[];
}

const ADDRESS_CHARACTER = /[^\s<>"'`]/;
/** How far short of the right edge a cut row may stop: Claude Code's boxed input leaves its last two columns empty. */
const EDGE_SLACK_COLUMNS = 2;

const unitsOfRow = (buffer: IBuffer, row: number): LogicalLine => {
  const line = buffer.getLine(row);
  const columns = Array.from({ length: line?.length ?? 0 }, (_unused, column) => column);
  return columns.reduce<LogicalLine>(
    (joined, column) => {
      const cell = line?.getCell(column);
      if (cell === undefined || cell.getWidth() === 0) {
        return joined;
      }
      const chars = cell.getChars() === '' ? ' ' : cell.getChars();
      const at = { x: column + 1, y: row + 1 };
      return { text: joined.text + chars, cells: [...joined.cells, ...Array.from({ length: chars.length }, () => at)] };
    },
    { text: '', cells: [] },
  );
};

const withoutTrailingBlanks = (line: LogicalLine): LogicalLine => {
  const length = line.text.trimEnd().length;
  return { text: line.text.slice(0, length), cells: line.cells.slice(0, length) };
};

const withoutLeadingBlanks = (line: LogicalLine): LogicalLine => {
  const indent = line.text.length - line.text.trimStart().length;
  return { text: line.text.slice(indent), cells: line.cells.slice(indent) };
};

const softWrapsOnto = (buffer: IBuffer, row: number): boolean => buffer.getLine(row + 1)?.isWrapped === true;

/**
 * Whether the program wrote the row's text up to the edge and carried it on after a line break and an indent, as Claude Code
 * wraps its own output: the row ends in an address character at most EDGE_SLACK_COLUMNS short of the edge, and the next row,
 * once indented, starts with one.
 */
// deferred: an address that ends at the edge by chance takes in the next row's first word — closed by a report of a wrong link opened.
const hardWrapsOnto = (buffer: IBuffer, row: number): boolean => {
  const columns = buffer.getLine(row)?.length ?? 0;
  const ending = withoutTrailingBlanks(unitsOfRow(buffer, row));
  const lastCell = ending.cells.at(-1);
  const next = withoutLeadingBlanks(unitsOfRow(buffer, row + 1));
  return (
    lastCell !== undefined &&
    lastCell.x >= columns - EDGE_SLACK_COLUMNS &&
    ADDRESS_CHARACTER.test(ending.text.at(-1) ?? ' ') &&
    ADDRESS_CHARACTER.test(next.text.at(0) ?? ' ')
  );
};

const wrapsOnto = (buffer: IBuffer, row: number): boolean => softWrapsOnto(buffer, row) || hardWrapsOnto(buffer, row);

const firstRowOf = (buffer: IBuffer, row: number): number => (row > 0 && wrapsOnto(buffer, row - 1) ? firstRowOf(buffer, row - 1) : row);

const rowsFrom = (buffer: IBuffer, row: number): number[] => (wrapsOnto(buffer, row) ? [row, ...rowsFrom(buffer, row + 1)] : [row]);

/** One row's part of its logical line: a hard wrap's blanks, the cut row's trailing ones and the next row's indent, are not part of it. */
const partOfRow = (buffer: IBuffer, row: number, first: number, last: number): LogicalLine => {
  const units = unitsOfRow(buffer, row);
  const ended = row < last && !softWrapsOnto(buffer, row) ? withoutTrailingBlanks(units) : units;
  return row > first && !softWrapsOnto(buffer, row - 1) ? withoutLeadingBlanks(ended) : ended;
};

/** The logical line holding the zero-based buffer row `row`, read cell by cell so a wide character keeps its two columns. */
const logicalLineAt = (buffer: IBuffer, row: number): LogicalLine => {
  const rows = rowsFrom(buffer, firstRowOf(buffer, row));
  const first = rows[0] ?? row;
  const last = rows.at(-1) ?? row;
  return rows
    .map((each) => partOfRow(buffer, each, first, last))
    .reduce<LogicalLine>((joined, part) => ({ text: joined.text + part.text, cells: [...joined.cells, ...part.cells] }), { text: '', cells: [] });
};

const lastCellOf = (buffer: IBuffer, at: IBufferCellPosition): IBufferCellPosition => {
  const wide = buffer.getLine(at.y - 1)?.getCell(at.x - 1)?.getWidth() ?? 1;
  return { x: at.x + wide - 1, y: at.y };
};

/** Whether `next` is the cell right after `end`, on its row or at the start of the row a soft wrap carries it to. */
const follows = (buffer: IBuffer, end: IBufferCellPosition, next: IBufferCellPosition): boolean =>
  next.y === end.y ? next.x <= end.x + 1 : next.y === end.y + 1 && next.x === 1 && end.x === buffer.getLine(end.y - 1)?.length;

/** The ranges an address's cells fill: one per unbroken run, so the indent a hard wrap leaves is no part of any. */
const rangesOf = (buffer: IBuffer, cells: IBufferCellPosition[]): IBufferRange[] =>
  cells.reduce<IBufferRange[]>((ranges, cell) => {
    const current = ranges.at(-1);
    return current !== undefined && follows(buffer, current.end, cell)
      ? [...ranges.slice(0, -1), { start: current.start, end: lastCellOf(buffer, cell) }]
      : [...ranges, { start: cell, end: lastCellOf(buffer, cell) }];
  }, []);

/** The one-row stretches of a range, for an underline is drawn row by row. */
const rowStretchesOf = (range: IBufferRange, columns: number): IBufferRange[] =>
  Array.from({ length: range.end.y - range.start.y + 1 }, (_unused, index) => range.start.y + index).map((y) => ({
    start: { x: y === range.start.y ? range.start.x : 1, y },
    end: { x: y === range.end.y ? range.end.x : columns, y },
  }));

/** xterm's own underline would mark the hovered part alone; every part's is drawn instead. */
const OWN_UNDERLINE_ONLY: ILinkDecorations = { pointerCursor: true, underline: false };

/**
 * One row's part of a plain web address in a pane, handing what happens to it to `links`. While the pointer is on any part,
 * every part is underlined, so an address the program wrapped with an indent reads as one link without the indent.
 */
class WebAddressLink implements ILink {
  readonly decorations: ILinkDecorations = OWN_UNDERLINE_ONLY;
  private underline: IDisposable | undefined;

  constructor(
    readonly text: string,
    readonly range: IBufferRange,
    private readonly parts: IBufferRange[],
    private readonly columns: number,
    private readonly underlines: UnderlineLayer,
    private readonly links: SessionLinks,
  ) {}

  activate(event: MouseEvent, text: string): void {
    this.links.activate(event, text);
  }

  hover(_event: MouseEvent, text: string): void {
    this.links.hover(text);
    this.underline?.dispose();
    this.underline = this.underlines.draw(this.parts.flatMap((part) => rowStretchesOf(part, this.columns)));
  }

  leave(): void {
    this.links.leave();
    this.underline?.dispose();
    this.underline = undefined;
  }
}

/** A link provider for the plain web addresses in a pane, a wrapped one included, each one wired to `links`. */
export class WebAddressProvider implements ILinkProvider {
  constructor(
    private readonly terminal: Pick<Terminal, 'buffer' | 'cols'>,
    private readonly underlines: UnderlineLayer,
    private readonly links: SessionLinks,
  ) {}

  provideLinks(y: number, callback: (links: ILink[] | undefined) => void): void {
    const buffer = this.terminal.buffer.active;
    const line = logicalLineAt(buffer, y - 1);
    const found = webAddressesIn(line.text)
      .flatMap((address) => {
        const parts = rangesOf(buffer, line.cells.slice(address.start, address.start + address.url.length));
        return parts.map((range) => new WebAddressLink(address.url, range, parts, this.terminal.cols, this.underlines, this.links));
      })
      .filter((link) => link.range.start.y <= y && link.range.end.y >= y);
    callback(found.length === 0 ? undefined : found);
  }
}
