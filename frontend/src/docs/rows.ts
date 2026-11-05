import type { DocumentView, FolderView } from '../protocol';
import type { DocsTreeView } from './docsState';

export enum RowKind {
  Document = 'document',
  Folder = 'folder',
}

/** One line of the Docs panel: a document's file name or a folder's header, set in by how many folders it sits in. */
export interface DocsRow {
  kind: RowKind;
  path: string;
  name: string;
  depth: number;
  count: number;
  folded: boolean;
}

const fileName = (path: string): string => path.slice(path.lastIndexOf('/') + 1);

const countIn = (folder: FolderView): number => folder.documents.length + folder.folders.reduce((sum, child) => sum + countIn(child), 0);

const rowsIn = (documents: DocumentView[], folders: FolderView[], depth: number, folded: ReadonlySet<string>): DocsRow[] => {
  const own = documents.map((document): DocsRow => ({ kind: RowKind.Document, path: document.path, name: fileName(document.path), depth, count: 0, folded: false }));
  const beneath = folders.flatMap((folder): DocsRow[] => {
    const isFolded = folded.has(folder.path);
    const header: DocsRow = { kind: RowKind.Folder, path: folder.path, name: fileName(folder.path), depth, count: countIn(folder), folded: isFolded };
    return isFolded ? [header] : [header, ...rowsIn(folder.documents, folder.folders, depth + 1, folded)];
  });
  return [...own, ...beneath];
};

/** The rows the panel shows, in the order the tree arrives: a folder's documents, then its folders, a folded one without what is beneath it. */
export const visibleRows = (tree: DocsTreeView, folded: ReadonlySet<string>): DocsRow[] => rowsIn(tree.documents, tree.folders, 0, folded);

const documentsIn = (documents: DocumentView[], folders: FolderView[]): DocumentView[] => [
  ...documents,
  ...folders.flatMap((folder) => documentsIn(folder.documents, folder.folders)),
];

/** The document the tree lists at `path`, at any depth. */
export const documentIn = (tree: DocsTreeView, path: string): DocumentView | undefined =>
  documentsIn(tree.documents, tree.folders).find((document) => document.path === path);
