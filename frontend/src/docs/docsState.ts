import type { DocsDocsView } from '../protocol';

export enum DocsStateKind {
  Loading = 'loading',
  Empty = 'empty',
  Unavailable = 'unavailable',
  Fault = 'fault',
  Populated = 'populated',
}

export type DocsTreeView = Extract<DocsDocsView, { kind: 'docsTree' }>;

/** `docs/watch` has not answered yet. */
export class LoadingDocs {
  readonly kind = DocsStateKind.Loading;

  shown(): DocsTreeView | undefined {
    return undefined;
  }
}

/** The docs folder was read and lists no document. */
export class EmptyDocs {
  readonly kind = DocsStateKind.Empty;

  shown(): DocsTreeView | undefined {
    return undefined;
  }
}

/** The sidecar answered that `docs/` could not be listed, and why. */
export class UnavailableDocs {
  readonly kind = DocsStateKind.Unavailable;

  constructor(readonly reason: string) {}

  shown(): DocsTreeView | undefined {
    return undefined;
  }
}

/** The read itself failed: a JSON-RPC error, or a sidecar that is not there. */
export class FaultedDocs {
  readonly kind = DocsStateKind.Fault;

  constructor(readonly message: string) {}

  shown(): DocsTreeView | undefined {
    return undefined;
  }
}

/** The tree with at least one document. */
export class PopulatedDocs {
  readonly kind = DocsStateKind.Populated;

  constructor(readonly tree: DocsTreeView) {}

  shown(): DocsTreeView | undefined {
    return this.tree;
  }
}

export type DocsState = LoadingDocs | EmptyDocs | UnavailableDocs | FaultedDocs | PopulatedDocs;
