import { headingSlugs } from '../design-system';
import { i18n } from '../i18n/i18n';
import type { Client, DocumentView } from '../protocol';
import { Store } from '../shared/store';
import { DocsSelection, type RowKey } from './docsSelection';
import { DocsStore } from './docsStore';
import { type DocumentLink, LinkKind, type LinkTarget, linkTarget } from './links';
import type { LinkOpener } from './opener';
import { type DocsRow, RowKind, documentIn, visibleRows } from './rows';
import { FoundDocument, MissingDocument, NoDocument, type ShownDocument } from './shownDocument';

const PROJECT_DIRECTORY = '.';

export interface DocsDeps {
  client: Client;
  /** The project's root, none before the board is read. */
  root: () => string | undefined;
  opener: LinkOpener;
  /** Puts a warning on the status line. */
  warn: (text: string) => void;
}

const keyOf = (row: DocsRow): RowKey => ({ kind: row.kind, path: row.path });

const sameKey = (row: DocsRow, key: RowKey): boolean => row.kind === key.kind && row.path === key.path;

const contentOf = (document: DocumentView): string | undefined =>
  document.kind === 'readDocument' && document.text.kind === 'content' ? document.text.value : undefined;

const ancestorsOf = (path: string): string[] => path.split('/').slice(0, -1).map((_, index, folders) => folders.slice(0, index + 1).join('/'));

/** The Docs panel: its one read, the selection in the tree, its folds, and where each link in a document goes. */
export class DocsController {
  readonly store: DocsStore;
  readonly selection: Store<DocsSelection> = new Store(DocsSelection.none());
  readonly folded: Store<ReadonlySet<string>> = new Store<ReadonlySet<string>>(new Set());
  private known: DocsRow[] = [];

  constructor(private readonly deps: DocsDeps) {
    this.store = new DocsStore(deps.client);
    this.store.state.subscribe(() => this.treeRead());
  }

  load(): Promise<void> {
    const root = this.deps.root();
    return root === undefined ? Promise.resolve() : this.store.load(root);
  }

  rows(): DocsRow[] {
    const tree = this.store.state.get().shown();
    return tree === undefined ? [] : visibleRows(tree, this.folded.get());
  }

  /** The row the selection is on: the one chosen, or the first before any is. */
  selectedKey(): RowKey | undefined {
    return this.keyIn(this.rows());
  }

  /** What the main view shows: the selected document, a path the tree does not list, or nothing. */
  shown(): ShownDocument {
    const key = this.selectedKey();
    const tree = this.store.state.get().shown();
    if (key === undefined || key.kind === RowKind.Folder) {
      return new NoDocument();
    }
    const document = tree === undefined ? undefined : documentIn(tree, key.path);
    return document === undefined ? new MissingDocument(key.path) : new FoundDocument(document);
  }

  move(delta: number): void {
    const rows = this.rows();
    const key = this.keyIn(rows);
    if (rows.length === 0 || key === undefined) {
      return;
    }
    const index = Math.max(0, rows.findIndex((row) => sameKey(row, key)));
    const target = rows[Math.min(rows.length - 1, Math.max(0, index + delta))];
    if (target !== undefined) {
      this.selection.set(this.selection.get().on(keyOf(target)));
    }
  }

  /** A press on a row: the first selects it, a second on the selected row is `enter`. */
  press(key: RowKey): void {
    const current = this.selectedKey();
    if (current !== undefined && current.kind === key.kind && current.path === key.path) {
      this.activate();
    } else {
      this.selection.set(this.selection.get().on(key));
    }
  }

  /** `enter`: a folder folds or unfolds, a document opens again at its top. */
  activate(): void {
    const key = this.selectedKey();
    if (key === undefined) {
      return;
    }
    if (key.kind === RowKind.Folder) {
      const next = new Set(this.folded.get());
      if (!next.delete(key.path)) {
        next.add(key.path);
      }
      this.folded.set(next);
    } else {
      this.selection.set(this.selection.get().on(key));
    }
  }

  /** A link in the shown document. */
  follow(href: string): void {
    const key = this.selectedKey();
    const from = key !== undefined && key.kind === RowKind.Document ? key.path : undefined;
    this.route(linkTarget(href, from, this.deps.root() ?? PROJECT_DIRECTORY));
  }

  /** A link in a ticket's body, which starts at the project's root. */
  followFromTicket(href: string): void {
    this.route(linkTarget(href, undefined, this.deps.root() ?? PROJECT_DIRECTORY));
  }

  private route(target: LinkTarget): void {
    switch (target.kind) {
      case LinkKind.Document:
        return this.openDocument(target);
      case LinkKind.Outside:
        return this.opened(target.path, this.deps.opener.openPath(target.path));
      case LinkKind.Browser:
        return this.opened(target.url, this.deps.opener.openUrl(target.url));
      case LinkKind.Refused:
        return this.deps.warn(i18n.t('docs:links.refused', { href: target.href }));
    }
  }

  private openDocument(link: DocumentLink): void {
    const tree = this.store.state.get().shown();
    const document = tree === undefined ? undefined : documentIn(tree, link.path);
    const text = document === undefined ? undefined : contentOf(document);
    const found = link.fragment !== undefined && text !== undefined && headingSlugs(text).includes(link.fragment);
    if (link.fragment !== undefined && !found && document !== undefined) {
      this.deps.warn(i18n.t('docs:links.fragmentNotFound', { fragment: link.fragment, path: link.path }));
    }
    this.folded.set(new Set([...this.folded.get()].filter((path) => !ancestorsOf(link.path).includes(path))));
    this.selection.set(this.selection.get().on({ kind: RowKind.Document, path: link.path }, found ? link.fragment : undefined));
  }

  private opened(target: string, opening: Promise<void>): void {
    opening.catch((error: unknown) => {
      const reason = error instanceof Error ? error.message : String(error);
      this.deps.warn(i18n.t('docs:links.openFailed', { target, reason }));
    });
  }

  private keyIn(rows: DocsRow[]): RowKey | undefined {
    const chosen = this.selection.get().row;
    const first = rows[0];
    return chosen ?? (first === undefined ? undefined : keyOf(first));
  }

  private treeRead(): void {
    const before = this.known;
    const was = this.keyIn(before);
    const now = this.rows();
    this.known = now;
    if (was === undefined || now.some((row) => sameKey(row, was))) {
      return;
    }
    const index = before.findIndex((row) => sameKey(row, was));
    const replacement = index === -1 ? undefined : now[Math.min(index, now.length - 1)];
    if (index !== -1) {
      this.selection.set(this.selection.get().on(replacement === undefined ? undefined : keyOf(replacement)));
    }
  }
}
