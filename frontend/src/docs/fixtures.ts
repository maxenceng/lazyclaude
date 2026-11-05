import type { DocumentView, FolderView } from '../protocol';
import type { DocsTreeView } from './docsState';

export const readFixture = (path: string, value: string): DocumentView => ({ kind: 'readDocument', path, text: { kind: 'content', value } });

export const emptyFixture = (path: string): DocumentView => ({ kind: 'readDocument', path, text: { kind: 'emptyDocument' } });

export const unreadableFixture = (path: string, reason: string): DocumentView => ({ kind: 'unreadableDocument', path, reason });

export const folderFixture = (path: string, documents: DocumentView[], folders: FolderView[] = []): FolderView => ({ path, documents, folders });

export const treeFixture = (documents: DocumentView[], folders: FolderView[] = []): DocsTreeView => ({ kind: 'docsTree', documents, folders });
