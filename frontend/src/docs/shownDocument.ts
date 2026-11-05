import type { DocumentView } from '../protocol';

export enum ShownKind {
  None = 'none',
  Found = 'found',
  Missing = 'missing',
}

/** The cursor is on no document: nothing is selected, or it is on a folder. */
export class NoDocument {
  readonly kind = ShownKind.None;
}

/** The document the tree lists at the selected path. */
export class FoundDocument {
  readonly kind = ShownKind.Found;

  constructor(readonly document: DocumentView) {}
}

/** A path a link named under `docs/` that the tree does not list. */
export class MissingDocument {
  readonly kind = ShownKind.Missing;

  constructor(readonly path: string) {}
}

export type ShownDocument = NoDocument | FoundDocument | MissingDocument;
