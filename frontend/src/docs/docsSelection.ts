import type { RowKind } from './rows';

/** A row of the tree, by what it is and its path: how the selection survives the tree being read again. */
export interface RowKey {
  kind: RowKind;
  path: string;
}

/**
 * The tree's selection, the one state of the Docs panel: the row it is on (undefined until the tree is first read,
 * which pins its first row), the heading the last open asked for, and a count that changes with each open so asking again scrolls again.
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

  /** The first row, pinned when the tree is first read, so a later read that puts another row first moves nothing. */
  pinned(row: RowKey): DocsSelection {
    return new DocsSelection(row, undefined, this.opened);
  }

  on(row: RowKey | undefined, fragment?: string): DocsSelection {
    return new DocsSelection(row, fragment, this.opened + 1);
  }
}
