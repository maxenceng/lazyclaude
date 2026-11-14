import { Terminal, type ILink } from '@xterm/xterm';
import { describe, expect, it, vi } from 'vitest';
import { NoticeStore } from '../shared/noticeStore';
import { SessionLinks, urlsIn, WebAddressProvider } from './links';

describe('the web addresses in a line of output', () => {
  it('finds an http address and where it starts', () => {
    expect(urlsIn('see http://example.com/a for more')).toEqual([{ url: 'http://example.com/a', start: 4 }]);
  });

  it('finds an https address at the end of the line', () => {
    expect(urlsIn('https://example.com/a?b=1')).toEqual([{ url: 'https://example.com/a?b=1', start: 0 }]);
  });

  it('leaves trailing punctuation out of the address', () => {
    expect(urlsIn('go to https://example.com/a, then https://example.com/b.')).toEqual([
      { url: 'https://example.com/a', start: 6 },
      { url: 'https://example.com/b', start: 34 },
    ]);
  });

  it('leaves the closing parenthesis of a sentence out, and keeps one the address balances', () => {
    expect(urlsIn('(https://example.com/a) and https://example.com/w_(x)')).toEqual([
      { url: 'https://example.com/a', start: 1 },
      { url: 'https://example.com/w_(x)', start: 28 },
    ]);
  });

  it('finds no address of another scheme', () => {
    expect(urlsIn('file:///etc/passwd ftp://example.com custom://x')).toEqual([]);
  });
});

const linked = (opening: () => Promise<void> = () => Promise.resolve()) => {
  const opener = { openUrl: vi.fn(opening) };
  const notices = new NoticeStore();
  return { opener, notices, links: new SessionLinks({ opener, notices }) };
};

const click = (init: MouseEventInit = {}) => new MouseEvent('click', init);

describe('a link followed in a session pane', () => {
  it('opens the address in the system browser on ctrl+click', () => {
    const { links, opener } = linked();
    links.activate(click({ ctrlKey: true }), 'https://example.com/a');
    expect(opener.openUrl).toHaveBeenCalledWith('https://example.com/a');
  });

  it('opens nothing on a plain click', () => {
    const { links, opener } = linked();
    links.activate(click(), 'https://example.com/a');
    expect(opener.openUrl).not.toHaveBeenCalled();
  });

  it.each(['file:///etc/passwd', 'custom://thing', 'javascript:alert(1)', 'not a url'])('opens nothing for %s and raises a notice naming it as refused', (target) => {
    const { links, opener, notices } = linked();
    links.activate(click({ ctrlKey: true }), target);
    expect(opener.openUrl).not.toHaveBeenCalled();
    expect(notices.notice.get()).toBe(`Did not open ${target}: only http and https links are opened.`);
  });

  it('raises no refusal on a plain click on a link of another scheme', () => {
    const { links, notices } = linked();
    links.activate(click(), 'file:///etc/passwd');
    expect(notices.notice.get()).toBeUndefined();
  });

  it('raises a notice with the address and the reason when the browser fails to open', async () => {
    const { links, notices } = linked(() => Promise.reject(new Error('no browser')));
    links.activate(click({ ctrlKey: true }), 'https://example.com/a');
    await vi.waitFor(() => expect(notices.notice.get()).toBe('Could not open https://example.com/a: no browser'));
  });
});

describe('the link the pointer is on', () => {
  it('is none until the pointer reaches a link', () => {
    expect(linked().links.hovered.get()).toBeUndefined();
  });

  it('is the link from the pointer reaching it until it leaves', () => {
    const { links } = linked();
    links.hover('https://example.com/a');
    expect(links.hovered.get()).toBe('https://example.com/a');
    links.leave();
    expect(links.hovered.get()).toBeUndefined();
  });
});


const linksOn = async (output: string, line: number): Promise<ILink[] | undefined> => {
  const terminal = new Terminal();
  await new Promise<void>((done) => terminal.write(output, done));
  const { links } = linked();
  return new Promise((found) => new WebAddressProvider(terminal, links).provideLinks(line, found));
};

describe('the provider of web addresses in a pane', () => {
  it('gives a link over each address, its range in the terminal\'s one-based cells', async () => {
    const found = await linksOn('see https://example.com/a.\r\nnothing here', 1);
    expect(found?.map((link) => [link.text, link.range])).toEqual([
      ['https://example.com/a', { start: { x: 5, y: 1 }, end: { x: 25, y: 1 } }],
    ]);
  });

  it('gives nothing for a line without an address', async () => {
    expect(await linksOn('see https://example.com/a.\r\nnothing here', 2)).toBeUndefined();
  });

  it('opens the address on ctrl+click and shows it as hovered while the pointer is on it', async () => {
    const terminal = new Terminal();
    await new Promise<void>((done) => terminal.write('https://example.com/a', done));
    const { links, opener } = linked();
    const [link] = await new Promise<ILink[]>((found) => new WebAddressProvider(terminal, links).provideLinks(1, (each) => found(each ?? [])));
    link?.hover?.(click(), 'https://example.com/a');
    expect(links.hovered.get()).toBe('https://example.com/a');
    link?.leave?.(click(), 'https://example.com/a');
    expect(links.hovered.get()).toBeUndefined();
    link?.activate(click({ ctrlKey: true }), 'https://example.com/a');
    expect(opener.openUrl).toHaveBeenCalledWith('https://example.com/a');
  });
});
