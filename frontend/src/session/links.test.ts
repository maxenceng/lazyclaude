import { Terminal, type ILink } from '@xterm/xterm';
import { describe, expect, it, vi } from 'vitest';
import { NoticeStore } from '../shared/noticeStore';
import { SessionLinks, webAddressesIn, WebAddressProvider } from './links';

describe('the web addresses in a line of output', () => {
  it('finds an http address and where it starts', () => {
    expect(webAddressesIn('see http://example.com/a for more')).toEqual([{ url: 'http://example.com/a', start: 4 }]);
  });

  it('finds an https address at the end of the line', () => {
    expect(webAddressesIn('https://example.com/a?b=1')).toEqual([{ url: 'https://example.com/a?b=1', start: 0 }]);
  });

  it('leaves trailing punctuation out of the address', () => {
    expect(webAddressesIn('go to https://example.com/a, then https://example.com/b.')).toEqual([
      { url: 'https://example.com/a', start: 6 },
      { url: 'https://example.com/b', start: 34 },
    ]);
  });

  it('leaves the closing parenthesis of a sentence out, and keeps one the address balances', () => {
    expect(webAddressesIn('(https://example.com/a) and https://example.com/w_(x)')).toEqual([
      { url: 'https://example.com/a', start: 1 },
      { url: 'https://example.com/w_(x)', start: 28 },
    ]);
  });

  it('keeps the bracket of an IPv6 host and drops one the address never opened', () => {
    expect(webAddressesIn('[http://[::1]:8080/a] and http://[::1]')).toEqual([
      { url: 'http://[::1]:8080/a', start: 1 },
      { url: 'http://[::1]', start: 26 },
    ]);
  });

  it('finds no address in a scheme with no host', () => {
    expect(webAddressesIn('see http://. and https://')).toEqual([]);
  });

  it('finds no address of another scheme', () => {
    expect(webAddressesIn('file:///etc/passwd ftp://example.com custom://x')).toEqual([]);
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

describe('a followed link and the notice', () => {
  it('clears an earlier notice when a ctrl+click opens a link', () => {
    const { links, notices } = linked();
    notices.raise('Did not open file:///etc/passwd: only http and https links are opened.');
    links.activate(click({ ctrlKey: true }), 'https://example.com/a');
    expect(notices.notice.get()).toBeUndefined();
  });

  it('keeps an earlier notice on a plain click', () => {
    const { links, notices } = linked();
    notices.raise('Could not open https://example.com/a: no browser');
    links.activate(click(), 'https://example.com/b');
    expect(notices.notice.get()).toBe('Could not open https://example.com/a: no browser');
  });
});

describe('the link the pointer is on', () => {
  it.each(['file:///etc/passwd', 'custom://thing'])('is none on %s, a link ctrl+click would refuse', (target) => {
    const { links } = linked();
    links.hover(target);
    expect(links.hovered.get()).toBeUndefined();
  });

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


const noUnderlines = () => {
  const drawn = { dispose: vi.fn() };
  return { drawn, draw: vi.fn(() => drawn) };
};

const linksOn = async (output: string, line: number, cols = 80): Promise<ILink[] | undefined> => {
  const terminal = new Terminal({ cols, rows: 10 });
  await new Promise<void>((done) => terminal.write(output, done));
  const { links } = linked();
  return new Promise((found) => new WebAddressProvider(terminal, noUnderlines(), links).provideLinks(line, found));
};

describe('the provider of web addresses in a pane', () => {
  it('gives a link over each address, its range in the terminal\'s one-based cells', async () => {
    const found = await linksOn('see https://example.com/a.\r\nnothing here', 1);
    expect(found?.map((link) => [link.text, link.range])).toEqual([
      ['https://example.com/a', { start: { x: 5, y: 1 }, end: { x: 25, y: 1 } }],
    ]);
  });

  it('places an address after wide characters in the cells they fill, two each', async () => {
    const found = await linksOn('漢字 https://example.com/a', 1);
    expect(found?.map((link) => [link.text, link.range])).toEqual([
      ['https://example.com/a', { start: { x: 6, y: 1 }, end: { x: 26, y: 1 } }],
    ]);
  });

  it('keeps an address with a wide character inside it one link', async () => {
    const found = await linksOn('https://example.com/漢a', 1);
    expect(found?.map((link) => link.range)).toEqual([{ start: { x: 1, y: 1 }, end: { x: 23, y: 1 } }]);
  });

  it('ends an address on a wide character on that character\'s second cell', async () => {
    const found = await linksOn('https://example.com/漢', 1);
    expect(found?.map((link) => link.range)).toEqual([{ start: { x: 1, y: 1 }, end: { x: 22, y: 1 } }]);
  });

  it.each([1, 2])('gives the whole of an address wrapped onto the next row, from row %i', async (row) => {
    const found = await linksOn('see https://example.com/abcdefghij', row, 20);
    expect(found?.map((link) => [link.text, link.range])).toEqual([
      ['https://example.com/abcdefghij', { start: { x: 5, y: 1 }, end: { x: 14, y: 2 } }],
    ]);
  });

  it.each([
    [1, { start: { x: 7, y: 1 }, end: { x: 20, y: 1 } }],
    [2, { start: { x: 7, y: 2 }, end: { x: 20, y: 2 } }],
    [3, { start: { x: 7, y: 3 }, end: { x: 12, y: 3 } }],
  ])('gives the whole of an address the program cut at the edge and carried on after an indent, over row %i\'s part alone', async (row, range) => {
    const found = await linksOn('  see https://exampl\r\n      e.com/abcdefgh\r\n      ijkl/x', row, 20);
    expect(found?.map((link) => [link.text, link.range]), 'the indent between parts is not underlined').toEqual([['https://example.com/abcdefghijkl/x', range]]);
  });

  it('carries on an address that stops just short of the edge, as a boxed line leaves its last columns empty', async () => {
    const found = await linksOn('  see https://exam\r\n  ple.com/a', 2, 20);
    expect(found?.map((link) => link.text)).toEqual(['https://example.com/a']);
  });

  it('keeps an address ending well before the edge apart from the next row', async () => {
    const found = await linksOn('see https://example.com\r\n  and more', 1, 40);
    expect(found?.map((link) => link.text)).toEqual(['https://example.com']);
  });

  it('gives no link on the row after an address ending well before the edge', async () => {
    expect(await linksOn('see https://example.com\r\n  and more', 2, 40)).toBeUndefined();
  });

  it('gives no link on a row the wrapped address does not reach', async () => {
    expect(await linksOn('nothing\r\n\r\nsee https://example.com/abcdefghij', 2, 20)).toBeUndefined();
  });

  it('gives nothing for a line without an address', async () => {
    expect(await linksOn('see https://example.com/a.\r\nnothing here', 2)).toBeUndefined();
  });

  it('underlines every part of a hard-wrapped address, without its indent, while the pointer is on one, and none once it leaves', async () => {
    const terminal = new Terminal({ cols: 20, rows: 10 });
    await new Promise<void>((done) => terminal.write('  see https://exampl\r\n      e.com/abcdefgh\r\n      ijkl/x', done));
    const underlines = noUnderlines();
    const { links } = linked();
    const [link] = await new Promise<ILink[]>((found) => new WebAddressProvider(terminal, underlines, links).provideLinks(2, (each) => found(each ?? [])));
    link?.hover?.(click(), 'https://example.com/abcdefghijkl/x');
    expect(link?.decorations?.underline, 'xterm would underline the hovered part alone').toBe(false);
    expect(underlines.draw).toHaveBeenCalledWith([
      { start: { x: 7, y: 1 }, end: { x: 20, y: 1 } },
      { start: { x: 7, y: 2 }, end: { x: 20, y: 2 } },
      { start: { x: 7, y: 3 }, end: { x: 12, y: 3 } },
    ]);
    link?.leave?.(click(), 'https://example.com/abcdefghijkl/x');
    expect(underlines.drawn.dispose).toHaveBeenCalledOnce();
  });

  it('underlines a soft-wrapped address over each row it fills', async () => {
    const underlines = noUnderlines();
    const terminal = new Terminal({ cols: 20, rows: 10 });
    await new Promise<void>((done) => terminal.write('see https://example.com/abcdefghij', done));
    const [link] = await new Promise<ILink[]>((found) => new WebAddressProvider(terminal, underlines, linked().links).provideLinks(1, (each) => found(each ?? [])));
    link?.hover?.(click(), 'https://example.com/abcdefghij');
    expect(underlines.draw).toHaveBeenCalledWith([
      { start: { x: 5, y: 1 }, end: { x: 20, y: 1 } },
      { start: { x: 1, y: 2 }, end: { x: 14, y: 2 } },
    ]);
  });

  it('opens the address on ctrl+click and shows it as hovered while the pointer is on it', async () => {
    const terminal = new Terminal();
    await new Promise<void>((done) => terminal.write('https://example.com/a', done));
    const { links, opener } = linked();
    const [link] = await new Promise<ILink[]>((found) => new WebAddressProvider(terminal, noUnderlines(), links).provideLinks(1, (each) => found(each ?? [])));
    link?.hover?.(click(), 'https://example.com/a');
    expect(links.hovered.get()).toBe('https://example.com/a');
    link?.leave?.(click(), 'https://example.com/a');
    expect(links.hovered.get()).toBeUndefined();
    link?.activate(click({ ctrlKey: true }), 'https://example.com/a');
    expect(opener.openUrl).toHaveBeenCalledWith('https://example.com/a');
  });
});
