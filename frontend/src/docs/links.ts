const DOCS_FOLDER = 'docs';
const WEB_LINK = /^https?:\/\//i;
const SCHEME = /^[a-z][a-z0-9+.-]*:/i;

export enum LinkKind {
  Document = 'document',
  Outside = 'outside',
  Browser = 'browser',
  Refused = 'refused',
}

/** A document of the tree, by its path under `docs/`, and the heading it asks for. */
export interface DocumentLink {
  kind: LinkKind.Document;
  path: string;
  fragment: string | undefined;
}

/** A file or folder the tree does not list, by its absolute path. */
export interface OutsideLink {
  kind: LinkKind.Outside;
  path: string;
}

export interface BrowserLink {
  kind: LinkKind.Browser;
  url: string;
}

/** A link whose scheme nothing here opens. */
export interface RefusedLink {
  kind: LinkKind.Refused;
  href: string;
}

export type LinkTarget = DocumentLink | OutsideLink | BrowserLink | RefusedLink;

const decoded = (text: string): string => {
  try {
    return decodeURIComponent(text);
  } catch {
    return text;
  }
};

const normalised = (segments: string[]): string[] => {
  return segments.reduce<string[]>((kept, segment) => {
    if (segment === '' || segment === '.') {
      return kept;
    }
    if (segment === '..') {
      return kept.length > 0 && kept[kept.length - 1] !== '..' ? kept.slice(0, -1) : [...kept, segment];
    }
    return [...kept, segment];
  }, []);
};

/**
 * Where a link goes. `from` is the document that holds it, by its path under `docs/`, or undefined for text that is not
 * a document (a ticket's body), whose links start at the project `root`.
 */
export const linkTarget = (href: string, from: string | undefined, root: string): LinkTarget => {
  if (WEB_LINK.test(href)) {
    return { kind: LinkKind.Browser, url: href };
  }
  if (SCHEME.test(href)) {
    return { kind: LinkKind.Refused, href };
  }
  const hash = href.indexOf('#');
  const beforeFragment = hash === -1 ? href : href.slice(0, hash);
  const fragment = hash === -1 || hash === href.length - 1 ? undefined : decoded(href.slice(hash + 1));
  const query = beforeFragment.indexOf('?');
  const file = decoded(query === -1 ? beforeFragment : beforeFragment.slice(0, query));
  if (file === '' && from !== undefined) {
    return { kind: LinkKind.Document, path: from, fragment };
  }
  if (file.startsWith('/')) {
    return { kind: LinkKind.Outside, path: file };
  }
  const base = from === undefined ? [] : [DOCS_FOLDER, ...from.split('/').slice(0, -1)];
  const resolved = normalised([...base, ...file.split('/')]);
  if (resolved[0] === DOCS_FOLDER && resolved.length > 1) {
    return { kind: LinkKind.Document, path: resolved.slice(1).join('/'), fragment };
  }
  return { kind: LinkKind.Outside, path: [root, ...resolved].join('/') };
};
