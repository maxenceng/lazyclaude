import type { RowKind } from './rows';

/** A row of the tree, by what it is and its path: how the selection survives the tree being read again. */
export interface RowKey {
  kind: RowKind;
  path: string;
}

/**
 * The tree's selection, the one state of the Docs panel: the row it is on (undefined until one is chosen, which
 * is the first), the heading the last open asked for, and a count that changes with each open so asking again scrolls again.
 */
export class DocsSelection {
  constructor(
    readonly row: RowKey | undefined,
    readonly fragment: string | undefined,
    readonly opened: number,
  ) {}

  /** Before anything is chosen. */
  static none(): DocsSelection {
    return new DocsSelection(undefined, undefined, 0);
  }

  on(row: RowKey | undefined, fragment?: string): DocsSelection {
    return new DocsSelection(row, fragment, this.opened + 1);
  }
}
