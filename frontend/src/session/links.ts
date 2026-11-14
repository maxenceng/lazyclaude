import type { IBuffer, IBufferCellPosition, IBufferRange, ILink, ILinkProvider, Terminal } from '@xterm/xterm';
import { i18n } from '../i18n/i18n';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';

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

/** One logical line of a pane, its soft-wrapped rows joined: its text, and the one-based cell each of its UTF-16 units starts in. */
interface LogicalLine {
  text: string;
  cells: IBufferCellPosition[];
}

const firstRowOf = (buffer: IBuffer, row: number): number =>
  row > 0 && buffer.getLine(row)?.isWrapped === true ? firstRowOf(buffer, row - 1) : row;

const rowsFrom = (buffer: IBuffer, row: number): number[] =>
  buffer.getLine(row + 1)?.isWrapped === true ? [row, ...rowsFrom(buffer, row + 1)] : [row];

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

/** The logical line holding the zero-based buffer row `row`, read cell by cell so a wide character keeps its two columns. */
const logicalLineAt = (buffer: IBuffer, row: number): LogicalLine =>
  rowsFrom(buffer, firstRowOf(buffer, row))
    .map((each) => unitsOfRow(buffer, each))
    .reduce<LogicalLine>((joined, part) => ({ text: joined.text + part.text, cells: [...joined.cells, ...part.cells] }), { text: '', cells: [] });

const lastCellOf = (buffer: IBuffer, at: IBufferCellPosition): IBufferCellPosition => {
  const wide = buffer.getLine(at.y - 1)?.getCell(at.x - 1)?.getWidth() ?? 1;
  return { x: at.x + wide - 1, y: at.y };
};

/** One plain web address in a pane, handing what happens to it to `links`. */
class WebAddressLink implements ILink {
  constructor(
    readonly text: string,
    readonly range: IBufferRange,
    private readonly links: SessionLinks,
  ) {}

  activate(event: MouseEvent, text: string): void {
    this.links.activate(event, text);
  }

  hover(_event: MouseEvent, text: string): void {
    this.links.hover(text);
  }

  leave(): void {
    this.links.leave();
  }
}

/** A link provider for the plain web addresses in a pane, a soft-wrapped one included, each one wired to `links`. */
export class WebAddressProvider implements ILinkProvider {
  constructor(
    private readonly terminal: Pick<Terminal, 'buffer'>,
    private readonly links: SessionLinks,
  ) {}

  provideLinks(y: number, callback: (links: ILink[] | undefined) => void): void {
    const buffer = this.terminal.buffer.active;
    const line = logicalLineAt(buffer, y - 1);
    const found = webAddressesIn(line.text)
      .map((address) => {
        const first = line.cells[address.start];
        const last = line.cells[address.start + address.url.length - 1];
        return first === undefined || last === undefined ? undefined : new WebAddressLink(address.url, { start: first, end: lastCellOf(buffer, last) }, this.links);
      })
      .filter((link): link is WebAddressLink => link !== undefined && link.range.start.y <= y && link.range.end.y >= y);
    callback(found.length === 0 ? undefined : found);
  }
}
