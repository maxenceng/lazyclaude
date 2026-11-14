import type { IBufferRange, ILink, ILinkProvider, Terminal } from '@xterm/xterm';
import { i18n } from '../i18n/i18n';
import type { NoticeStore } from '../shared/noticeStore';
import { Store } from '../shared/store';

/** One web address in a line of output, and the column its first character is in. */
export interface WebAddress {
  url: string;
  start: number;
}

const WEB_ADDRESS = /https?:\/\/[^\s<>"'`]+/g;
const SENTENCE_PUNCTUATION = /[.,;:!?\]}]$/;

const count = (text: string, character: string): number => text.split(character).length - 1;

/** Drops what ends a sentence rather than the address: punctuation, and a closing parenthesis the address never opened. */
const trimmed = (url: string): string => {
  const closesUnopened = url.endsWith(')') && count(url, ')') > count(url, '(');
  return closesUnopened || SENTENCE_PUNCTUATION.test(url) ? trimmed(url.slice(0, -1)) : url;
};

/** The http and https addresses in one line, left to right; any other scheme is not a link here. */
export const urlsIn = (line: string): WebAddress[] =>
  [...line.matchAll(WEB_ADDRESS)].map((found) => ({ url: trimmed(found[0]), start: found.index }));

const WEB_SCHEMES = ['http:', 'https:'];

const isWeb = (target: string): boolean => {
  try {
    return WEB_SCHEMES.includes(new URL(target).protocol);
  } catch {
    return false;
  }
};

/** What opens a web address in the system browser. */
export interface UrlOpener {
  openUrl(url: string): Promise<void>;
}

export interface SessionLinksDeps {
  opener: UrlOpener;
  notices: NoticeStore;
}

/** The links of one session pane: followed with ctrl held, and the one the pointer is on. */
export class SessionLinks {
  readonly hovered: Store<string | undefined> = new Store<string | undefined>(undefined);

  constructor(private readonly deps: SessionLinksDeps) {}

  hover(url: string): void {
    this.hovered.set(url);
  }

  leave(): void {
    this.hovered.set(undefined);
  }

  activate(event: MouseEvent, url: string): void {
    if (!event.ctrlKey) {
      return;
    }
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

/** One plain web address in a pane, handing what happens to it to `links`. */
class WebAddressLink implements ILink {
  readonly text: string;
  readonly range: IBufferRange;

  constructor(
    address: WebAddress,
    line: number,
    private readonly links: SessionLinks,
  ) {
    this.text = address.url;
    this.range = { start: { x: address.start + 1, y: line }, end: { x: address.start + address.url.length, y: line } };
  }

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

/** A link provider for the plain web addresses in a pane's lines, each one wired to `links`. */
export class WebAddressProvider implements ILinkProvider {
  constructor(
    private readonly terminal: Pick<Terminal, 'buffer'>,
    private readonly links: SessionLinks,
  ) {}

  provideLinks(y: number, callback: (links: ILink[] | undefined) => void): void {
    const line = this.terminal.buffer.active.getLine(y - 1)?.translateToString(true) ?? '';
    const found = urlsIn(line).map((address) => new WebAddressLink(address, y, this.links));
    callback(found.length === 0 ? undefined : found);
  }
}

const CLIPPED_LENGTH = 20;
const ELLIPSIS = '…';

/** An address as the key bar shows it: cut to a length that leaves the other keys in view, with an ellipsis where it was cut. */
export const clipped = (url: string): string => (url.length <= CLIPPED_LENGTH ? url : `${url.slice(0, CLIPPED_LENGTH - 1)}${ELLIPSIS}`);
