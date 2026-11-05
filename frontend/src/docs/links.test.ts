import { describe, expect, it } from 'vitest';
import { LinkKind, linkTarget } from './links';

const ROOT = '/p';

describe('where a link in a document goes', () => {
  it('goes outside the tree when the link climbs out of docs', () => {
    expect(linkTarget('../../README.md', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/README.md' });
  });

  it('goes to a document of the tree, by its path under docs, when the link stays inside', () => {
    expect(linkTarget('../adr/0001-first.md', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Document, path: 'adr/0001-first.md', fragment: undefined });
    expect(linkTarget('./other.md', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Document, path: 'guide/other.md', fragment: undefined });
    expect(linkTarget('top.md', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Document, path: 'top.md', fragment: undefined });
  });

  it('carries the fragment, decoded, and drops a query', () => {
    expect(linkTarget('other.md?plain=1#Some%20Heading', 'guide/intro.md', ROOT)).toEqual({
      kind: LinkKind.Document,
      path: 'guide/other.md',
      fragment: 'Some Heading',
    });
  });

  it('stays on the document when the link is only a fragment', () => {
    expect(linkTarget('#setup', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Document, path: 'guide/intro.md', fragment: 'setup' });
  });

  it('opens an http or https link in the browser, whole', () => {
    expect(linkTarget('https://example.com/a?b=1#c', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Browser, url: 'https://example.com/a?b=1#c' });
    expect(linkTarget('HTTP://example.com', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Browser, url: 'HTTP://example.com' });
  });

  it('refuses a scheme it does not open', () => {
    expect(linkTarget('javascript:alert(1)', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Refused, href: 'javascript:alert(1)' });
    expect(linkTarget('mailto:me@example.com', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Refused, href: 'mailto:me@example.com' });
  });

  it('resolves a link of something that is not a document from the project root', () => {
    expect(linkTarget('docs/guide/intro.md#a', undefined, ROOT)).toEqual({ kind: LinkKind.Document, path: 'guide/intro.md', fragment: 'a' });
    expect(linkTarget('src/main.rs', undefined, ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/src/main.rs' });
  });

  it('starts a path beginning with a slash at the project root, as GitHub does', () => {
    expect(linkTarget('/docs/adr/0001-first.md', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Document, path: 'adr/0001-first.md', fragment: undefined });
    expect(linkTarget('/src/main.rs', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/src/main.rs' });
  });

  it('keeps a file under docs that is not markdown, or a folder, outside the tree', () => {
    expect(linkTarget('diagram.svg', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/docs/guide/diagram.svg' });
    expect(linkTarget('../adr/', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/docs/adr' });
    expect(linkTarget('../adr', 'guide/intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p/docs/adr' });
  });

  it('follows no fragment of a ticket\'s own body anywhere', () => {
    expect(linkTarget('#section', undefined, ROOT)).toEqual({ kind: LinkKind.TicketHeading, href: '#section' });
    expect(linkTarget('', undefined, ROOT)).toEqual({ kind: LinkKind.TicketHeading, href: '' });
  });

  it('keeps a link to the docs folder itself outside the tree', () => {
    expect(linkTarget('..', 'intro.md', ROOT)).toEqual({ kind: LinkKind.Outside, path: '/p' });
  });
});
