import type { DocsDocumentView, DocsFolderView } from '../protocol';
import type { DocsTreeView } from './docsState';

export const readFixture = (path: string, value: string): DocsDocumentView => ({ kind: 'readDocument', path, text: { kind: 'content', value } });

export const emptyFixture = (path: string): DocsDocumentView => ({ kind: 'readDocument', path, text: { kind: 'emptyDocument' } });

export const unreadableFixture = (path: string, reason: string): DocsDocumentView => ({ kind: 'unreadableDocument', path, reason });

export const folderFixture = (path: string, documents: DocsDocumentView[], folders: DocsFolderView[] = []): DocsFolderView => ({ path, documents, folders });

export const treeFixture = (documents: DocsDocumentView[], folders: DocsFolderView[] = []): DocsTreeView => ({ kind: 'docsTree', documents, folders });
